package com.example.smd.services;

import com.example.smd.dto.excel.AssessmentImportDTO;
import com.example.smd.dto.response.validate.AssessmentImportResult;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.example.smd.services.excelService.ExcelImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

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

    // ─── Dependencies ─────────────────────────────────────────────────────
    private final SyllabusRepository syllabusRepository;
    private final CLOsRepository closRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentCategoryRepository assessmentCategoryRepository;
    private final AssessmentTypeRepository assessmentTypeRepository;
    private final CloAssessmentMappingRepository cloAssessmentMappingRepository;

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

        // 2a. Validate Category & Type theo quy tắc Formative/Summative
        validateCategoryTypePairs(rows, result);

        // 2b. Validate CLO-Mapping — CLO phải thuộc Subject
        validateCloMappings(rows, subjectId, result);

        // 2c. Validate tổng Weight == 100
        validateTotalWeight(rows, result);

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
    //                     STEP 2a: VALIDATE CATEGORY / TYPE             //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Kiểm tra từng dòng: Category và Type phải hợp lệ và đúng cặp.
     * <ul>
     *   <li>Formative  → Type phải thuộc {Lab, Presentation, Quiz}</li>
     *   <li>Summative → Type phải thuộc {Final, Midterm, Project}</li>
     * </ul>
     */
    private void validateCategoryTypePairs(List<AssessmentImportDTO> rows, AssessmentImportResult result) {
        for (AssessmentImportDTO row : rows) {
            String cat  = (row.getCategory() == null ? "" : row.getCategory().trim()).toUpperCase();
            String type = (row.getType()     == null ? "" : row.getType().trim()).toUpperCase();

            // Validate trường bắt buộc không được rỗng
            if (cat.isBlank()) {
                result.addError("MISSING_REQUIRED_FIELD",
                        String.format("Row %d: Column 'Category' is required.", row.getRowNumber()),
                        row.getRowNumber());
                continue;
            }
            if (type.isBlank()) {
                result.addError("MISSING_REQUIRED_FIELD",
                        String.format("Row %d: Column 'Type' is required.", row.getRowNumber()),
                        row.getRowNumber());
                continue;
            }

            // Validate cặp Category – Type
            if (CATEGORY_FORMATIVE.equals(cat)) {
                if (!FORMATIVE_TYPES.contains(type)) {
                    result.addError("CATEGORY_TYPE_MISMATCH",
                            String.format("Row %d: Category 'Formative' requires Type to be one of %s, but got '%s'.",
                                    row.getRowNumber(), FORMATIVE_TYPES, row.getType()),
                            row.getRowNumber());
                }
            } else if (CATEGORY_SUMMATIVE.equals(cat)) {
                if (!SUMMATIVE_TYPES.contains(type)) {
                    result.addError("CATEGORY_TYPE_MISMATCH",
                            String.format("Row %d: Category 'Summative' requires Type to be one of %s, but got '%s'.",
                                    row.getRowNumber(), SUMMATIVE_TYPES, row.getType()),
                            row.getRowNumber());
                }
            } else {
                result.addError("INVALID_CATEGORY",
                        String.format("Row %d: Unknown Category '%s'. Expected 'Formative' or 'Summative'.",
                                row.getRowNumber(), row.getCategory()),
                        row.getRowNumber());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════ //
    //                     STEP 2b: VALIDATE CLO MAPPING                 //
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
    //                     STEP 2c: VALIDATE TOTAL WEIGHT                //
    // ═══════════════════════════════════════════════════════════════════ //

    /**
     * Tính tổng cột Weight của toàn bộ các dòng import.
     * Báo lỗi tổng quan (rowNumber = -1) nếu tổng ≠ 100.
     */
    private void validateTotalWeight(List<AssessmentImportDTO> rows, AssessmentImportResult result) {
        double total = 0.0;

        for (AssessmentImportDTO row : rows) {
            if (row.getWeight() == null || row.getWeight().isBlank()) {
                result.addError("MISSING_REQUIRED_FIELD",
                        String.format("Row %d: Column 'Weight' is required.", row.getRowNumber()),
                        row.getRowNumber());
                return; // Không thể tính tổng nếu có dòng thiếu weight
            }
            try {
                total += Double.parseDouble(row.getWeight().trim());
            } catch (NumberFormatException e) {
                result.addError("INVALID_WEIGHT_FORMAT",
                        String.format("Row %d: Weight '%s' is not a valid number.", row.getRowNumber(), row.getWeight()),
                        row.getRowNumber());
                return;
            }
        }

        // Làm tròn để tránh lỗi floating-point (VD: 99.99999999)
        double rounded = Math.round(total * 100.0) / 100.0;
        if (rounded != 100.0) {
            result.addError("WEIGHT_NOT_100",
                    String.format("Total weight of all assessments must be exactly 100%% (currently %.2f%%).", rounded),
                    -1);
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
}
