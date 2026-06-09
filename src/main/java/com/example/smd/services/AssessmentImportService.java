package com.example.smd.services;

import com.example.smd.dto.excel.AssessmentImportDTO;
import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.response.validate.AssessmentImportResult;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.example.smd.services.excelService.ExcelImporter;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.bcel.generic.ATHROW;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Service xử lý luồng Import Assessment từ file Excel.
 *
 * <p><b>Business Flow:</b>
 * <ol>
 *   <li>Đọc file Excel → List&lt;AssessmentImportDTO&gt; (bỏ qua "ghost rows").</li>
 *   <li>Validate Category/Type theo quy tắc Formative/Summative.</li>
 *   <li>Validate CLO-Mapping: kiểm tra mã CLO hợp lệ với Subject.</li>
 *   <li>Validate tổng Weight == 100.</li>
 *   <li>Nếu có lỗi → trả về ngay, KHÔNG lưu DB.</li>
 *   <li>Nếu hợp lệ → REPLACE: xóa cũ rồi lưu mới (Assessment + CLO mapping).</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentImportService {

    // ─── Quy tắc Category ↔ Type (case-insensitive) ─────────────────────
    private static final Set<String> FORMATIVE_TYPES = Set.of("LAB", "PRESENTATION", "QUIZ");
    private static final Set<String> SUMMATIVE_TYPES = Set.of("FINAL", "MIDTERM", "PROJECT");

    private static final String CATEGORY_FORMATIVE = "FORMATIVE";
    private static final String CATEGORY_SUMMATIVE = "SUMMATIVE";

    /**
     * Bảng quy tắc: Type (viết hoa) → tập Question Type hợp lệ (viết hoa).
     *
     * <pre>
     * Formative:
     *   QUIZ         → Multiple Choice, Essay
     *   LAB          → Practical Exam, Assignment
     *   PRESENTATION → Presentation
     *
     * Summative:
     *   MIDTERM → Multiple Choice, Essay, Case Study
     *   PROJECT → Project-based
     *   FINAL   → Practical Exam, Essay, Case Study, Multiple Choice
     * </pre>
     */
    private static final Map<String, Set<String>> ALLOWED_QUESTION_TYPES_BY_TYPE = Map.of(
            // ── Formative ──
            "QUIZ",         Set.of("MULTIPLE CHOICE", "ESSAY"),
            "LAB",          Set.of("PRACTICAL EXAM", "ASSIGNMENT"),
            "PRESENTATION", Set.of("PRESENTATION"),
            // ── Summative ──
            "MIDTERM",      Set.of("MULTIPLE CHOICE", "ESSAY", "CASE STUDY"),
            "PROJECT",      Set.of("PROJECT-BASED"),
            "FINAL",        Set.of("PRACTICAL EXAM", "ESSAY", "CASE STUDY", "MULTIPLE CHOICE")
    );

    // ─── Dependencies ─────────────────────────────────────────────────────
    private final SyllabusRepository syllabusRepository;
    private final CLOsRepository closRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCategoryRepository assessmentCategoryRepository;
    private final AssessmentTypeRepository assessmentTypeRepository;
    private final CloAssessmentMappingRepository cloAssessmentMappingRepository;
    private final AssessmentService assessmentService;

    // ═══════════════════════════════════════════════════════════════════ //
    //                        MAIN ENTRY POINT                           //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Import danh sách Assessment từ file Excel vào Syllabus.
     *
     * @param file       File Excel (.xlsx) — dòng 1 là header, dữ liệu từ dòng 2
     * @param syllabusId UUID của Syllabus cần import vào
     * @param subjectId  UUID của Subject (dùng để lấy CLO hợp lệ)
     * @return {@link AssessmentImportResult} — chứa lỗi hoặc kết quả success
     */
    @Transactional
    public AssessmentImportResult importFromExcel(MultipartFile file,
                                                  UUID syllabusId,
                                                  UUID subjectId) {
        // ── Bước 0: Xác nhận Syllabus tồn tại ───────────────────────────
        Syllabus syllabus = syllabusRepository.findByIdWithSubject(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        // ── Bước 1: Đọc file Excel ────────────────────────────────────────
        List<AssessmentImportDTO> rows;
        try {
            rows = readExcel(file);
        } catch (Exception e) {
            log.error("Cannot read Excel file: {}", e.getMessage(), e);
            AssessmentImportResult result = new AssessmentImportResult();
            result.addError("FILE_READ_ERROR",
                    "Cannot read the Excel file. Please ensure it is a valid .xlsx file.", -1);
            return result;
        }

        AssessmentImportResult result = new AssessmentImportResult();
        result.setTotalRows(rows.size());

        if (rows.isEmpty()) {
            result.addError("EMPTY_FILE", "The Excel file contains no data rows.", -1);
            return result;
        }

        // ── Bước 2: Validate (gom lỗi) ────────────────────────────────────

        // 2a. Validate Category & Type & Question Type
        validateCategoryTypePairs(rows, result);

        // 2b. Validate kiểu số: Part, Weight, Duration phải là số hợp lệ
        validateNumericFields(rows, result);

        // 2c. Validate CLO-Mapping — CLO phải thuộc Subject
        validateCloMappings(rows, subjectId, result);

        // 2d. Validate tổng thể toàn bộ assessment
        validateAssessment(rows, result, syllabusId);

        // ── Bước 3: Nếu có lỗi → Return sớm, KHÔNG lưu DB ───────────────
        if (!result.isValid()) {
            return result;
        }

        // ── Bước 4: Lưu DB theo chiến lược REPLACE ───────────────────────
        int savedCount = replaceAndSave(rows, syllabus, subjectId);
        result.setSavedCount(savedCount);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //                     STEP 1: READ EXCEL                            //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Đọc file Excel và map sang {@link AssessmentImportDTO}.
     * Dùng {@link ExcelImporter} theo annotation {@code @ExcelColumn}.
     * Sau khi đọc, gán {@code rowNumber} (1-indexed, tính từ dòng data đầu tiên = dòng 2 trong Excel).
     *
     * <p><b>Ghost row detection:</b> Nếu cả {@code category} và {@code type} đều rỗng → bỏ qua dòng.
     */
    private List<AssessmentImportDTO> readExcel(MultipartFile file) throws Exception {
        List<AssessmentImportDTO> rawRows = ExcelImporter.importFromExcel(file, AssessmentImportDTO.class);

        List<AssessmentImportDTO> filtered = new ArrayList<>();
        int excelRowNumber = 2; // dòng 1 là header; dữ liệu bắt đầu từ dòng 2

        for (AssessmentImportDTO row : rawRows) {
            // Chặn "ghost rows": bỏ qua dòng mà cả Category lẫn Type đều rỗng
            boolean categoryBlank = row.getCategory() == null || row.getCategory().isBlank();
            boolean typeBlank     = row.getType()     == null || row.getType().isBlank();

            if (categoryBlank && typeBlank) {
                excelRowNumber++;
                continue;
            }

            row.setRowNumber(excelRowNumber);
            filtered.add(row);
            excelRowNumber++;
        }

        return filtered;
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //           STEP 2a: VALIDATE CATEGORY / TYPE / QUESTION TYPE       //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Validate toàn bộ 3 tầng: Category → Type → Question Type.
     *
     * <p>Quy tắc đầy đủ:
     * <pre>
     * Formative:
     *   Quiz         → Question Type ∈ {Multiple Choice, Essay}
     *   Lab          → Question Type ∈ {Practical Exam, Assignment}
     *   Presentation → Question Type ∈ {Presentation}
     *
     * Summative:
     *   Midterm → Question Type ∈ {Multiple Choice, Essay, Case Study}
     *   Project → Question Type ∈ {Project-based}
     *   Final   → Question Type ∈ {Practical Exam, Essay, Case Study, Multiple Choice}
     * </pre>
     *
     * <p>Chiến lược gom lỗi: lỗi Type chặn validate Question Type ở dòng đó
     * (không có ý nghĩa khi Type đã sai).
     */
    private void validateCategoryTypePairs(List<AssessmentImportDTO> rows, AssessmentImportResult result) {
        for (AssessmentImportDTO row : rows) {
            String cat          = normalise(row.getCategory());
            String type         = normalise(row.getType());
            String questionType = normalise(row.getQuestionType());

            // ── 1. Validate trường bắt buộc ──────────────────────────────
            if (cat.isBlank()) {
                result.addError("MISSING_REQUIRED_FIELD",
                        String.format("Row %d: Column 'Category' is required.", row.getRowNumber()),
                        row.getRowNumber());
                continue; // không thể validate tiếp khi Category rỗng
            }
            if (type.isBlank()) {
                result.addError("MISSING_REQUIRED_FIELD",
                        String.format("Row %d: Column 'Type' is required.", row.getRowNumber()),
                        row.getRowNumber());
                continue; // không thể validate Question Type khi Type rỗng
            }

            // ── 2. Validate Category phải là Formative hoặc Summative ─────
            boolean isFormative = CATEGORY_FORMATIVE.equals(cat);
            boolean isSummative = CATEGORY_SUMMATIVE.equals(cat);

            if (!isFormative && !isSummative) {
                result.addError("INVALID_CATEGORY",
                        String.format("Row %d: Unknown Category '%s'. Expected 'Formative' or 'Summative'.",
                                row.getRowNumber(), row.getCategory()),
                        row.getRowNumber());
                continue; // Category sai → không validate Type/QuestionType nữa
            }

            // ── 3. Validate cặp Category → Type ──────────────────────────
            Set<String> allowedTypes = isFormative ? FORMATIVE_TYPES : SUMMATIVE_TYPES;
            if (!allowedTypes.contains(type)) {
                result.addError("CATEGORY_TYPE_MISMATCH",
                        String.format("Row %d: Category '%s' requires Type to be one of %s, but got '%s'.",
                                row.getRowNumber(),
                                row.getCategory(),
                                formatAllowedSet(allowedTypes),
                                row.getType()),
                        row.getRowNumber());
                // Type đã sai → validate Question Type sẽ vô nghĩa, skip
                continue;
            }

            // ── 4. Validate Question Type theo Type ───────────────────────
            if (!questionType.isBlank()) {
                Set<String> allowedQTypes = ALLOWED_QUESTION_TYPES_BY_TYPE.getOrDefault(type, Set.of());
                if (!allowedQTypes.contains(questionType)) {
                    result.addError("QUESTION_TYPE_MISMATCH",
                            String.format(
                                    "Row %d: Type '%s' requires Question Type to be one of %s, but got '%s'.",
                                    row.getRowNumber(),
                                    row.getType(),
                                    formatAllowedSet(allowedQTypes),
                                    row.getQuestionType()),
                            row.getRowNumber());
                }
            }
            // Question Type có thể để trống — không bắt buộc ở tầng này
        }
    }

    // ─── Helpers dùng riêng cho validate ─────────────────────────────────

    /**
     * Chuẩn hoá chuỗi: trim + toUpperCase. Trả về chuỗi rỗng nếu null.
     * Dùng để so sánh không phân biệt hoa thường và khoảng trắng thừa.
     */
    private String normalise(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase();
    }

    /**
     * Format tập hợp thành chuỗi dạng [A, B, C] có chữ hoa đầu mỗi từ
     * để thông báo lỗi thân thiện với người dùng.
     */
    private String formatAllowedSet(Set<String> keys) {
        return keys.stream()
                .map(k -> Arrays.stream(k.split(" "))
                        .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
                        .collect(Collectors.joining(" ")))
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //              STEP 2b: VALIDATE NUMERIC FIELDS                     //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Kiểm tra các cột số bắt buộc/tùy chọn phải có định dạng số hợp lệ.
     *
     * <ul>
     *   <li><b>Weight</b>   — bắt buộc, phải là số thực dương (VD: 30, 30.5)</li>
     *   <li><b>Part</b>     — tùy chọn, nếu có thì phải là số nguyên dương</li>
     *   <li><b>Duration</b> — tùy chọn, nếu có thì phải là số nguyên dương</li>
     * </ul>
     *
     * <p>Gom toàn bộ lỗi trên tất cả các dòng trước khi trả về —
     * không dừng sớm ({@code return}) sau lỗi đầu tiên.
     */
    private void validateNumericFields(List<AssessmentImportDTO> rows, AssessmentImportResult result) {
        for (AssessmentImportDTO row : rows) {
            int rowNo = row.getRowNumber();

            // ── Weight: bắt buộc, số thực dương ─────────────────────────
            String weightRaw = row.getWeight();
            if (weightRaw == null || weightRaw.isBlank()) {
                result.addError("MISSING_REQUIRED_FIELD",
                        String.format("Row %d: Column 'Weight' is required.", rowNo),
                        rowNo);
            } else {
                try {
                    double w = Double.parseDouble(weightRaw.trim());
                    if (w <= 0) {
                        result.addError("INVALID_WEIGHT",
                                String.format("Row %d: Weight must be a positive number, but got '%s'.", rowNo, weightRaw.trim()),
                                rowNo);
                    }
                } catch (NumberFormatException e) {
                    result.addError("INVALID_WEIGHT_FORMAT",
                            String.format("Row %d: Weight '%s' is not a valid number (only digits and '.' are allowed).",
                                    rowNo, weightRaw.trim()),
                            rowNo);
                }
            }

            // ── Part: tùy chọn, nếu có thì phải là số nguyên dương ──────
            String partRaw = row.getPart();
            if (partRaw != null && !partRaw.isBlank()) {
                try {
                    int p = Integer.parseInt(partRaw.trim());
                    if (p <= 0) {
                        result.addError("INVALID_PART",
                                String.format("Row %d: Part must be a positive integer, but got '%s'.", rowNo, partRaw.trim()),
                                rowNo);
                    }
                } catch (NumberFormatException e) {
                    result.addError("INVALID_PART_FORMAT",
                            String.format("Row %d: Part '%s' is not a valid integer (only digits are allowed).",
                                    rowNo, partRaw.trim()),
                            rowNo);
                }
            }

            // ── Duration: tùy chọn, nếu có thì phải là số nguyên dương ──
            String durationRaw = row.getDuration();
            if (durationRaw != null && !durationRaw.isBlank()) {
                try {
                    int d = Integer.parseInt(durationRaw.trim());
                    if (d <= 0) {
                        result.addError("INVALID_DURATION",
                                String.format("Row %d: Duration must be a positive integer (minutes), but got '%s'.",
                                        rowNo, durationRaw.trim()),
                                rowNo);
                    }
                } catch (NumberFormatException e) {
                    result.addError("INVALID_DURATION_FORMAT",
                            String.format("Row %d: Duration '%s' is not a valid integer (only digits are allowed).",
                                    rowNo, durationRaw.trim()),
                            rowNo);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //                     STEP 2c: VALIDATE CLO MAPPING                 //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Kiểm tra từng mã CLO trong cột CLO-Mapping có thuộc Subject không.
     * Tải danh sách CLO hợp lệ một lần duy nhất (tránh N+1).
     */
    private void validateCloMappings(List<AssessmentImportDTO> rows,
                                     UUID subjectId,
                                     AssessmentImportResult result) {
        Set<String> validCloCodes = closRepository.findBySubject_SubjectId(subjectId)
                .stream()
                .map(clo -> clo.getCloCode().toUpperCase().trim())
                .collect(Collectors.toSet());

        for (AssessmentImportDTO row : rows) {
            if (row.getCloMapping() == null || row.getCloMapping().isBlank()) continue;

            List<String> requestedCodes = parseCloMapping(row.getCloMapping());
            for (String code : requestedCodes) {
                if (!validCloCodes.contains(code)) {
                    result.addError("CLO_INVALID",
                            String.format("Row %d: CLO code '%s' does not belong to this Subject.",
                                    row.getRowNumber(), code),
                            row.getRowNumber());
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //                     STEP 2d: VALIDATE TOTAL WEIGHT                //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Tính tổng cột Weight của toàn bộ các dòng import.
     * Bước này chỉ chạy SAU KHI {@link #validateNumericFields} đã pass,
     * nên {@code Double.parseDouble} ở đây sẽ không throw exception.
     * Báo lỗi tổng quan (rowNumber = -1) nếu tổng ≠ 100.
     */
    private void validateAssessment(List<AssessmentImportDTO> rows, AssessmentImportResult result, UUID syllabusId) {
        // Nếu đã có lỗi format số ở bước trước → skip (tổng không có ý nghĩa)
        List<AssessmentRequest> assessmentRequestList = new ArrayList<>();
        for (AssessmentImportDTO row : rows) {
            var assessmentCategory = assessmentCategoryRepository.findByCategoryName(row.getCategory()).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_CATEGORY_NAME_INVALID));;
            var assessmentType = assessmentTypeRepository.findByTypeName(row.getType()).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_TYPE_NAME_INVALID));

            AssessmentRequest assessmentRequest = new AssessmentRequest();
            assessmentRequest.setCategoryId(assessmentCategory.getCategoryId());
            assessmentRequest.setTypeId(assessmentType.getTypeId());
            assessmentRequest.setSyllabusId(syllabusId);
            assessmentRequest.setDuration(
                    (row.getDuration() != null && !row.getDuration().isEmpty())
                            ? Integer.parseInt(row.getDuration())
                            : null
            );
            assessmentRequest.setPart(row.getRowNumber());
            assessmentRequest.setNote(row.getNote());
            assessmentRequest.setWeight(Double.parseDouble(row.getWeight()));
            assessmentRequest.setCompletionCriteria(row.getCompletionCriteria());
            assessmentRequest.setQuestionType(row.getQuestionType());
            assessmentRequest.setGradingGuide(row.getGradingGuide());
            assessmentRequest.setKnowledgeSkill(row.getKnowledgeSkill());
            assessmentRequestList.add(assessmentRequest);
        }
        var validateResult = assessmentService.validate(assessmentRequestList, syllabusId);
        if(!validateResult.isValid() && !validateResult.getErrors().isEmpty()) {
            for(var error : validateResult.getErrors()) {
                result.addError(error.getCode(), error.getMessage(), -1);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //                     STEP 4: SAVE (REPLACE MODE)                   //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Xóa toàn bộ dữ liệu cũ và lưu dữ liệu mới từ file Excel.
     * Thứ tự: xóa CLO_Assessment mapping → xóa Assessment → flush → insert mới.
     *
     * @return Số Assessment đã lưu thành công
     */
    private int replaceAndSave(List<AssessmentImportDTO> rows, Syllabus syllabus, UUID subjectId) {
        UUID syllabusId = syllabus.getSyllabusId();

        // 1. Xóa CLO_Assessment mapping trước (tránh FK violation)
        cloAssessmentMappingRepository.deleteByAssessment_Syllabus_SyllabusId(syllabusId);

        // 2. Xóa toàn bộ Assessment cũ
        assessmentRepository.deleteAllBySyllabus_SyllabusId(syllabusId);
        assessmentRepository.flush(); // Đảm bảo DELETE xong trước INSERT

        // 3. Pre-load lookup maps (tránh N+1 query)
        Map<String, Assessment_Category> categoryMap  = buildCategoryMap();
        Map<String, Assessment_Type>     typeMap       = buildTypeMap();
        Map<String, CLOs>                cloMap        = buildCloMap(subjectId);

        int savedCount = 0;

        for (AssessmentImportDTO row : rows) {
            // Lấy entity Category và Type
            String catKey  = row.getCategory().trim().toUpperCase();
            String typeKey = row.getType().trim().toUpperCase();

            Assessment_Category category = categoryMap.get(catKey);
            Assessment_Type     type     = typeMap.get(typeKey);

            // Nếu DB chưa có Category / Type → tạo mới (auto-provision)
            if (category == null) {
                category = assessmentCategoryRepository.save(
                        Assessment_Category.builder()
                                .categoryName(capitalize(row.getCategory().trim()))
                                .build());
                categoryMap.put(catKey, category);
            }
            if (type == null) {
                type = assessmentTypeRepository.save(
                        Assessment_Type.builder()
                                .typeName(capitalize(row.getType().trim()))
                                .build());
                typeMap.put(typeKey, type);
            }

            // Xây dựng Assessment entity
            Assessment assessment = Assessment.builder()
                    .syllabus(syllabus)
                    .assessmentCategory(category)
                    .assessmentType(type)
                    .part(parseIntSafe(row.getPart()))
                    .weight(parseDoubleSafe(row.getWeight()))
                    .completionCriteria(row.getCompletionCriteria())
                    .duration(parseIntSafe(row.getDuration()))
                    .questionType(row.getQuestionType())
                    .knowledgeSkill(row.getKnowledgeSkill())
                    .gradingGuide(row.getGradingGuide())
                    .note(row.getNote())
                    .build();

            Assessment saved = assessmentRepository.save(assessment);
            savedCount++;

            // Lưu CLO_Assessment mapping
            if (row.getCloMapping() != null && !row.getCloMapping().isBlank()) {
                List<CLO_Assessment> mappings = parseCloMapping(row.getCloMapping()).stream()
                        .map(code -> cloMap.get(code.toUpperCase().trim()))
                        .filter(Objects::nonNull)
                        .map(clo -> CLO_Assessment.builder()
                                .clo(clo)
                                .assessment(saved)
                                .build())
                        .collect(Collectors.toList());

                cloAssessmentMappingRepository.saveAll(mappings);
            }
        }

        return savedCount;
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //                          PRIVATE HELPERS                          //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Tách chuỗi CLO-Mapping thành danh sách mã CLO đã chuẩn hoá (trim + uppercase).
     * VD: {@code "CLO1, CLO2 , clo3"} → {@code ["CLO1", "CLO2", "CLO3"]}
     */
    private List<String> parseCloMapping(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Tải tất cả {@link Assessment_Category} từ DB và index theo tên viết hoa.
     * Tránh N+1 query trong vòng lặp lưu.
     */
    private Map<String, Assessment_Category> buildCategoryMap() {
        return assessmentCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> c.getCategoryName().toUpperCase().trim(),
                        c -> c,
                        (a, b) -> a // giữ bản đầu tiên nếu trùng tên
                ));
    }

    /**
     * Tải tất cả {@link Assessment_Type} từ DB và index theo tên viết hoa.
     */
    private Map<String, Assessment_Type> buildTypeMap() {
        return assessmentTypeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getTypeName().toUpperCase().trim(),
                        t -> t,
                        (a, b) -> a
                ));
    }

    /**
     * Tải tất cả CLO thuộc Subject và index theo cloCode viết hoa.
     */
    private Map<String, CLOs> buildCloMap(UUID subjectId) {
        return closRepository.findBySubject_SubjectId(subjectId).stream()
                .collect(Collectors.toMap(
                        c -> c.getCloCode().toUpperCase().trim(),
                        c -> c
                ));
    }

    /** Parse chuỗi số nguyên an toàn. Trả về {@code null} nếu rỗng hoặc không hợp lệ. */
    private Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return (int) Double.parseDouble(value.trim()); // xử lý cả "1.0"
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parse chuỗi số thực an toàn. Trả về {@code null} nếu rỗng hoặc không hợp lệ. */
    private Double parseDoubleSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Viết hoa chữ cái đầu, phần còn lại giữ nguyên. */
    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }
    // ═══════════════════════════════════════════════════════════════════ //
    //                   EXPORT TO EXCEL                                  //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Xuất danh sách Assessment của một Syllabus ra file Excel (.xlsx).
     *
     * <p>Cấu trúc cột hoàn toàn tương thích với template Import (0-indexed):
     * <pre>
     *   Col 0 : Category
     *   Col 1 : Type
     *   Col 2 : Part
     *   Col 3 : Weight
     *   Col 4 : Completion Criteria
     *   Col 5 : Duration
     *   Col 6 : Question Type
     *   Col 7 : Knowledge Skill
     *   Col 8 : Grading Guide
     *   Col 9 : Note
     *   Col 10: CLO-Mapping
     * </pre>
     *
     * @param syllabusId UUID của Syllabus cần export
     * @return byte array nội dung file .xlsx
     */
    @Transactional(readOnly = true)
    public byte[] exportToExcel(UUID syllabusId) {
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        // 1. Truy vấn danh sách Assessment sắp xếp theo part tăng dần
        List<Assessment> assessments = assessmentRepository
                .findBySyllabus_SyllabusIdOrderByPartAsc(syllabusId);

        // 2. Truy vấn toàn bộ CLO mapping của Syllabus 1 lần, group theo assessmentId
        Map<UUID, List<String>> cloByAssessmentId = cloAssessmentMappingRepository
                .findByAssessment_Syllabus_SyllabusId(syllabusId)
                .stream()
                .filter(ca -> ca.getClo() != null && ca.getClo().getCloCode() != null)
                .collect(Collectors.groupingBy(
                        ca -> ca.getAssessment().getAssessmentId(),
                        Collectors.mapping(
                                ca -> ca.getClo().getCloCode().trim(),
                                Collectors.toList()
                        )
                ));

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Assessments");

            // ── Header style — in đậm ─────────────────────────────────
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // ── Tạo dòng Header (index 0) ───────────────────────────────
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Category", "Type", "Part", "Weight",
                    "Completion Criteria", "Duration", "Question Type",
                    "Knowledge Skill", "Grading Guide", "Note", "CLO-Mapping"
            };
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            // ── Ghi dữ liệu từng Assessment ─────────────────────────────
            int rowIdx = 1;
            for (Assessment a : assessments) {
                Row row = sheet.createRow(rowIdx++);

                // Col 0: Category
                String categoryName = (a.getAssessmentCategory() != null
                        && a.getAssessmentCategory().getCategoryName() != null)
                        ? a.getAssessmentCategory().getCategoryName() : "";
                row.createCell(0).setCellValue(categoryName);

                // Col 1: Type
                String typeName = (a.getAssessmentType() != null
                        && a.getAssessmentType().getTypeName() != null)
                        ? a.getAssessmentType().getTypeName() : "";
                row.createCell(1).setCellValue(typeName);

                // Col 2: Part (số nguyên, để rống nếu null)
                if (a.getPart() != null) {
                    row.createCell(2).setCellValue(a.getPart());
                } else {
                    row.createCell(2).setCellValue("");
                }

                // Col 3: Weight (số thực)
                if (a.getWeight() != null) {
                    row.createCell(3).setCellValue(a.getWeight());
                } else {
                    row.createCell(3).setCellValue("");
                }

                // Col 4: Completion Criteria
                row.createCell(4).setCellValue(
                        a.getCompletionCriteria() != null ? a.getCompletionCriteria() : "");

                // Col 5: Duration (số nguyên)
                if (a.getDuration() != null) {
                    row.createCell(5).setCellValue(a.getDuration());
                } else {
                    row.createCell(5).setCellValue("");
                }

                // Col 6: Question Type
                row.createCell(6).setCellValue(
                        a.getQuestionType() != null ? a.getQuestionType() : "");

                // Col 7: Knowledge Skill
                row.createCell(7).setCellValue(
                        a.getKnowledgeSkill() != null ? a.getKnowledgeSkill() : "");

                // Col 8: Grading Guide
                row.createCell(8).setCellValue(
                        a.getGradingGuide() != null ? a.getGradingGuide() : "");

                // Col 9: Note
                row.createCell(9).setCellValue(
                        a.getNote() != null ? a.getNote() : "");

                // Col 10: CLO-Mapping — join các cloCode bằng ", " (sắp xếp alpha)
                List<String> cloCodes = cloByAssessmentId
                        .getOrDefault(a.getAssessmentId(), List.of());
                String cloMapping = cloCodes.stream()
                        .sorted()
                        .collect(Collectors.joining(", "));
                row.createCell(10).setCellValue(cloMapping);
            }

            // ── Auto-size toàn bộ cột cho đẹp ──────────────────────────
            for (int col = 0; col < headers.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to export Assessments to Excel for syllabusId={}: {}", syllabusId, e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
