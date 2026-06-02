package com.example.smd.services;

import com.example.smd.dto.excel.RubricImportDTO;
import com.example.smd.dto.response.rubric.RubricImportResponse;
import com.example.smd.dto.response.rubric.RubricImportRowResult;
import com.example.smd.entities.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.example.smd.services.excelService.ExcelImporter;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service xử lý import dữ liệu Rubric từ file Excel.
 *
 * Luồng xử lý:
 *  1. Đọc Excel → List<RubricImportDTO> (flat rows)
 *  2. Validate từng dòng → skip dòng lỗi, ghi log chi tiết
 *  3. Gom nhóm dữ liệu hợp lệ trong memory theo cấu trúc phân cấp
 *  4. Batch insert: Rubric → RubricCriterion + RubricLevel → CriteriaLevel
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RubricImportService {

    RubricRepository            rubricRepository;
    RubricCriterionRepository   rubricCriterionRepository;
    RubricLevelRepository       rubricLevelRepository;
    CriteriaLevelRepository     criteriaLevelRepository;
    SyllabusRepository          syllabusRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Import Rubric từ file Excel.
     *
     * @param syllabusId UUID của Syllabus mà các Rubric sẽ được gắn vào (bắt buộc).
     * @param file       File Excel có cấu trúc phẳng (không merged cells).
     * @return {@link RubricImportResponse} tổng hợp kết quả và danh sách lỗi.
     */
    @Transactional
    public RubricImportResponse importRubric(String syllabusId, MultipartFile file) {
        // ── Validate syllabusId trước khi xử lý bất kỳ dòng nào ───────────
        UUID syllabusUUID = parseSyllabusId(syllabusId);
        Syllabus syllabus = syllabusRepository.findById(syllabusUUID)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                        "Syllabus không tồn tại: " + syllabusId));

        // ── Bước 1: Đọc Excel → flat rows ──────────────────────────────────
        List<RubricImportDTO> rows = readExcel(file);

        // ── Bước 2: Validate từng dòng ─────────────────────────────────────
        List<RubricImportRowResult> errors    = new ArrayList<>();
        List<ValidatedRow>          validRows = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // +2 vì Excel bắt đầu từ 1 và dòng 1 là header
            RubricImportDTO dto = rows.get(i);
            List<String> rowErrors = validateRow(dto);

            if (!rowErrors.isEmpty()) {
                errors.add(RubricImportRowResult.builder()
                        .rowNumber(rowNum)
                        .rubricCode(dto.getRubricCode())
                        .criteriaCode(dto.getCriteriaCode())
                        .status("FAILED")
                        .message(String.join("; ", rowErrors))
                        .build());
            } else {
                validRows.add(new ValidatedRow(rowNum, dto));
            }
        }

        // ── Bước 3: Gom nhóm trong memory ──────────────────────────────────
        GroupedData grouped = groupByHierarchy(validRows);

        // ── Bước 4: Batch insert xuống DB ──────────────────────────────────
        InsertStats stats = batchInsert(grouped, syllabus);

        // ── Trả về kết quả ─────────────────────────────────────────────────
        return RubricImportResponse.builder()
                .total(rows.size())
                .success(validRows.size())
                .failed(errors.size())
                .rubricCreated(stats.rubricCreated)
                .criterionCreated(stats.criterionCreated)
                .criteriaLevelCreated(stats.criteriaLevelCreated)
                .errors(errors)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 1: Đọc Excel
    // ─────────────────────────────────────────────────────────────────────────

    private List<RubricImportDTO> readExcel(MultipartFile file) {
        try {
            return ExcelImporter.importFromExcel(file, RubricImportDTO.class);
        } catch (Exception e) {
            log.error("Failed to read Excel file for Rubric import", e);
            throw new RuntimeException("Không thể đọc file Excel: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 2: Validate từng dòng
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kiểm tra các trường bắt buộc và định dạng dữ liệu.
     * @return Danh sách thông báo lỗi (rỗng nếu hợp lệ).
     */
    private List<String> validateRow(RubricImportDTO dto) {
        List<String> errs = new ArrayList<>();

        // Các trường bắt buộc
        if (isBlank(dto.getRubricCode()))    errs.add("Rubric Code không được để trống");
        if (isBlank(dto.getRubricName()))    errs.add("Rubric Name không được để trống");
        if (isBlank(dto.getCriteriaCode()))  errs.add("Criteria Code không được để trống");
        if (isBlank(dto.getCriteriaName()))  errs.add("Criteria Name không được để trống");
        if (isBlank(dto.getLevel()))         errs.add("Level không được để trống");

        // Validate Weight
        if (isBlank(dto.getWeight())) {
            errs.add("Weight không được để trống");
        } else {
            try {
                BigDecimal weight = new BigDecimal(dto.getWeight().trim());
                if (weight.compareTo(BigDecimal.ZERO) < 0
                        || weight.compareTo(new BigDecimal("100")) > 0) {
                    errs.add("Weight phải nằm trong khoảng 0 – 100, hiện tại: " + dto.getWeight());
                }
            } catch (NumberFormatException e) {
                errs.add("Weight không phải số hợp lệ: '" + dto.getWeight() + "'");
            }
        }

        return errs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 3: Gom nhóm dữ liệu trong memory
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gom nhóm các dòng hợp lệ theo phân cấp:
     *   rubricCode → criteriaCode → levelCode → description
     */
    private GroupedData groupByHierarchy(List<ValidatedRow> validRows) {
        // Map<rubricCode, RubricInfo>
        Map<String, RubricInfo> rubricMap = new LinkedHashMap<>();

        for (ValidatedRow vr : validRows) {
            RubricImportDTO dto = vr.dto;

            String rubricCode   = dto.getRubricCode().trim();
            String rubricName   = dto.getRubricName().trim();
            String criteriaCode = dto.getCriteriaCode().trim();
            String criteriaName = dto.getCriteriaName().trim();
            BigDecimal weight   = new BigDecimal(dto.getWeight().trim());
            String levelCode    = dto.getLevel().trim();
            String description  = dto.getDescription() != null ? dto.getDescription().trim() : null;

            // ── Rubric ────────────────────────────────────────────────────
            RubricInfo rubricInfo = rubricMap.computeIfAbsent(
                    rubricCode,
                    k -> new RubricInfo(rubricCode, rubricName)
            );

            // ── RubricLevel (global per Rubric, key = levelCode) ──────────
            rubricInfo.levelMap.putIfAbsent(levelCode, new LevelInfo(levelCode));

            // ── RubricCriterion ───────────────────────────────────────────
            CriterionInfo criterionInfo = rubricInfo.criterionMap.computeIfAbsent(
                    criteriaCode,
                    k -> new CriterionInfo(criteriaCode, criteriaName, weight)
            );

            // ── CriteriaLevel (cặp criteriaCode × levelCode) ──────────────
            String clKey = criteriaCode + "||" + levelCode;
            criterionInfo.criteriaLevelMap.putIfAbsent(
                    clKey,
                    new CriteriaLevelInfo(levelCode, description)
            );
        }

        return new GroupedData(rubricMap);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 4: Batch insert xuống Database
    // ─────────────────────────────────────────────────────────────────────────

    private InsertStats batchInsert(GroupedData grouped, Syllabus syllabus) {
        int rubricCreated        = 0;
        int criterionCreated     = 0;
        int criteriaLevelCreated = 0;

        for (RubricInfo rubricInfo : grouped.rubricMap.values()) {

            // ── 4.1 Upsert Rubric ─────────────────────────────────────────
            Rubric rubric = rubricRepository.findByCode(rubricInfo.rubricCode)
                    .orElse(null);

            if (rubric == null) {
                // Tạo mới — gắn syllabus vào
                rubric = Rubric.builder()
                        .syllabus(syllabus)
                        .code(rubricInfo.rubricCode)
                        .name(rubricInfo.rubricName)
                        .build();
                rubric = rubricRepository.save(rubric);
                rubricCreated++;
                log.info("[RubricImport] Tạo mới Rubric: {} (syllabusId={})",
                        rubricInfo.rubricCode, syllabus.getSyllabusId());
            } else {
                log.info("[RubricImport] Rubric '{}' đã tồn tại, bỏ qua tạo mới — tiếp tục xử lý Criterion/Level bên dưới.",
                        rubricInfo.rubricCode);
            }

            // ── 4.2 Upsert RubricLevel (global của Rubric này) ────────────
            // Map<levelCode, RubricLevel entity> để dùng khi tạo CriteriaLevel
            Map<String, RubricLevel> savedLevelMap = new LinkedHashMap<>();

            for (LevelInfo levelInfo : rubricInfo.levelMap.values()) {
                RubricLevel level = rubricLevelRepository.findByLevelCode(levelInfo.levelCode)
                        .orElse(null);
                if (level == null) {
                    level = RubricLevel.builder()
                            .levelCode(levelInfo.levelCode)
                            .build();
                    level = rubricLevelRepository.save(level);
                    log.debug("[RubricImport]   Tạo mới RubricLevel: {}", levelInfo.levelCode);
                }
                savedLevelMap.put(levelInfo.levelCode, level);
            }

            // ── 4.3 Upsert RubricCriterion + CriteriaLevel ───────────────
            int orderIdx = 1;
            for (CriterionInfo criterionInfo : rubricInfo.criterionMap.values()) {

                // Kiểm tra xem Criterion đã tồn tại chưa (theo code)
                RubricCriterion criterion = findExistingCriterion(rubric, criterionInfo.criteriaCode);

                if (criterion == null) {
                    criterion = RubricCriterion.builder()
                            .rubric(rubric)
                            .code(criterionInfo.criteriaCode)
                            .criterionName(criterionInfo.criteriaName)
                            .weight(criterionInfo.weight)
                            .displayOrder(orderIdx)
                            .build();
                    criterion = rubricCriterionRepository.save(criterion);
                    criterionCreated++;
                    log.debug("[RubricImport]   Tạo mới Criterion: {}", criterionInfo.criteriaCode);
                } else {
                    log.debug("[RubricImport]   Criterion '{}' đã tồn tại, bỏ qua.",
                            criterionInfo.criteriaCode);
                }

                orderIdx++;

                // ── 4.4 CriteriaLevel ─────────────────────────────────────
                for (CriteriaLevelInfo clInfo : criterionInfo.criteriaLevelMap.values()) {
                    RubricLevel level = savedLevelMap.get(clInfo.levelCode);
                    if (level == null) {
                        log.warn("[RubricImport]   Level '{}' không tìm thấy, bỏ qua CriteriaLevel.", clInfo.levelCode);
                        continue;
                    }

                    boolean alreadyExists = criteriaLevelRepository
                            .existsByCriterion_CriterionIdAndLevel_LevelId(
                                    criterion.getCriterionId(),
                                    level.getLevelId()
                            );

                    if (!alreadyExists) {
                        CriteriaLevel criteriaLevel = CriteriaLevel.builder()
                                .criterion(criterion)
                                .level(level)
                                .description(clInfo.description)
                                .build();
                        criteriaLevelRepository.save(criteriaLevel);
                        criteriaLevelCreated++;
                        log.debug("[RubricImport]     Tạo CriteriaLevel: {} × {}", clInfo.levelCode, clInfo.description);
                    }
                }
            }
        }

        return new InsertStats(rubricCreated, criterionCreated, criteriaLevelCreated);
    }

    /**
     * Tìm RubricCriterion theo Rubric + criteriaCode.
     * Trả về null nếu chưa tồn tại.
     */
    private RubricCriterion findExistingCriterion(Rubric rubric, String criteriaCode) {
        return rubricCriterionRepository
                .findByRubric_RubricIdOrderByDisplayOrderAsc(rubric.getRubricId())
                .stream()
                .filter(c -> criteriaCode.equalsIgnoreCase(c.getCode()))
                .findFirst()
                .orElse(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Parse và validate định dạng UUID của syllabusId. */
    private UUID parseSyllabusId(String syllabusId) {
        if (isBlank(syllabusId)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "syllabusId không được để trống");
        }
        try {
            return UUID.fromString(syllabusId.trim());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "syllabusId không đúng định dạng UUID: " + syllabusId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal data holders (thay thế cho anonymous Map entries)
    // ─────────────────────────────────────────────────────────────────────────

    /** Một dòng Excel đã qua validate thành công. */
    private record ValidatedRow(int rowNum, RubricImportDTO dto) {}

    /** Thông tin gom nhóm của 1 Rubric trong memory. */
    private static class RubricInfo {
        String rubricCode;
        String rubricName;
        /** criteriaCode → CriterionInfo */
        Map<String, CriterionInfo> criterionMap = new LinkedHashMap<>();
        /** levelCode → LevelInfo */
        Map<String, LevelInfo>     levelMap     = new LinkedHashMap<>();

        RubricInfo(String rubricCode, String rubricName) {
            this.rubricCode = rubricCode;
            this.rubricName = rubricName;
        }
    }

    /** Thông tin gom nhóm của 1 Criterion trong memory. */
    private static class CriterionInfo {
        String criteriaCode;
        String criteriaName;
        BigDecimal weight;
        /** "criteriaCode||levelCode" → CriteriaLevelInfo */
        Map<String, CriteriaLevelInfo> criteriaLevelMap = new LinkedHashMap<>();

        CriterionInfo(String criteriaCode, String criteriaName, BigDecimal weight) {
            this.criteriaCode = criteriaCode;
            this.criteriaName = criteriaName;
            this.weight       = weight;
        }
    }

    /** Thông tin của 1 Level thuộc Rubric. */
    private record LevelInfo(String levelCode) {}

    /** Thông tin của 1 ô CriteriaLevel (giao điểm criterion × level). */
    private record CriteriaLevelInfo(String levelCode, String description) {}

    /** Toàn bộ dữ liệu đã gom nhóm. */
    private record GroupedData(Map<String, RubricInfo> rubricMap) {}

    /** Thống kê số bản ghi được INSERT. */
    private record InsertStats(int rubricCreated, int criterionCreated, int criteriaLevelCreated) {}
}
