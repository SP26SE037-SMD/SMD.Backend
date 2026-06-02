package com.example.smd.controller;

import com.example.smd.dto.request.rubric.*;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.rubric.*;
import com.example.smd.services.RubricImportService;
import com.example.smd.services.RubricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/rubrics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Rubric", description = "Rubric Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RubricController {

    RubricService       rubricService;
    RubricImportService rubricImportService;

    // ==================== IMPORT APIs ====================

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import Rubric từ file Excel",
            description = "File Excel phẳng (flat data), không merged cells.\n\n" +
                    "Cấu trúc cột (theo thứ tự):\n" +
                    "| Rubric Code | Rubric Name | Criteria Code | Criteria Name | Weight | Level | Description |"
    )
    public ResponseObject<RubricImportResponse> importRubric(
            @RequestParam String syllabusId,
            @RequestPart("file") MultipartFile file) {
        return ResponseObject.<RubricImportResponse>builder()
                .status(1000)
                .data(rubricImportService.importRubric(syllabusId, file))
                .message("Import Rubric hoàn tất")
                .build();
    }

    @PostMapping
    @Operation(summary = "Tạo mới Rubric (kèm criteria và levels)")
    public ResponseObject<RubricResponse> createRubric(@RequestBody RubricRequest request) {
        return ResponseObject.<RubricResponse>builder()
                .status(1000)
                .data(rubricService.createRubric(request))
                .message("Tạo Rubric thành công")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết Rubric theo ID")
    public ResponseObject<RubricResponse> getRubricById(@PathVariable String id) {
        return ResponseObject.<RubricResponse>builder()
                .status(1000)
                .data(rubricService.getRubricById(id))
                .message("Lấy Rubric thành công")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả Rubric")
    public ResponseObject<List<RubricResponse>> getAllRubrics() {
        return ResponseObject.<List<RubricResponse>>builder()
                .status(1000)
                .data(rubricService.getAllRubrics())
                .message("Lấy danh sách Rubric thành công")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Lấy danh sách Rubric theo Syllabus ID (JSON response)")
    public ResponseObject<List<RubricResponse>> getRubricsBySyllabusId(@PathVariable String syllabusId) {
        return ResponseObject.<List<RubricResponse>>builder()
                .status(1000)
                .data(rubricService.getRubricsBySyllabusId(syllabusId))
                .message("Lấy danh sách Rubric theo Syllabus thành công")
                .build();
    }

    @GetMapping(value = "/syllabus/{syllabusId}/text", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Lấy danh sách Rubric theo Syllabus ID (trả về chuỗi text JSON thuần)")
    public ResponseEntity<String> getRubricsBySyllabusIdAsText(@PathVariable String syllabusId) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(rubricService.getRubricsBySyllabusIdAsText(syllabusId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật Rubric theo ID (ghi đè toàn bộ criteria và levels)")
    public ResponseObject<RubricResponse> updateRubric(@PathVariable String id,
                                                       @RequestBody RubricRequest request) {
        return ResponseObject.<RubricResponse>builder()
                .status(1000)
                .data(rubricService.updateRubric(id, request))
                .message("Cập nhật Rubric thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá Rubric theo ID (kèm toàn bộ criteria và levels)")
    public ResponseObject<Void> deleteRubric(@PathVariable String id) {
        rubricService.deleteRubric(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Xoá Rubric thành công")
                .build();
    }

    // ==================== RUBRIC CRITERION APIs ====================

    @PostMapping("/{rubricId}/criteria")
    @Operation(summary = "Tạo mới Criterion cho Rubric")
    public ResponseObject<CriterionResponse> createCriterion(@PathVariable String rubricId,
                                                              @RequestBody CriterionRequest request) {
        return ResponseObject.<CriterionResponse>builder()
                .status(1000)
                .data(rubricService.createCriterion(rubricId, request))
                .message("Tạo Criterion thành công")
                .build();
    }

    @GetMapping("/criteria/{criterionId}")
    @Operation(summary = "Lấy thông tin Criterion theo ID")
    public ResponseObject<CriterionResponse> getCriterionById(@PathVariable String criterionId) {
        return ResponseObject.<CriterionResponse>builder()
                .status(1000)
                .data(rubricService.getCriterionById(criterionId))
                .message("Lấy Criterion thành công")
                .build();
    }

    @PutMapping("/criteria/{criterionId}")
    @Operation(summary = "Cập nhật Criterion theo ID")
    public ResponseObject<CriterionResponse> updateCriterion(@PathVariable String criterionId,
                                                              @RequestBody CriterionRequest request) {
        return ResponseObject.<CriterionResponse>builder()
                .status(1000)
                .data(rubricService.updateCriterion(criterionId, request))
                .message("Cập nhật Criterion thành công")
                .build();
    }

    @DeleteMapping("/criteria/{criterionId}")
    @Operation(summary = "Xoá Criterion theo ID")
    public ResponseObject<Void> deleteCriterion(@PathVariable String criterionId) {
        rubricService.deleteCriterion(criterionId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Xoá Criterion thành công")
                .build();
    }

    // ==================== RUBRIC LEVEL APIs ====================

    @PostMapping("/levels")
    @Operation(summary = "Tạo mới RubricLevel (mức đánh giá dùng chung)")
    public ResponseObject<RubricLevelResponse> createLevel(@RequestBody RubricLevelRequest request) {
        return ResponseObject.<RubricLevelResponse>builder()
                .status(1000)
                .data(rubricService.createLevel(request))
                .message("Tạo RubricLevel thành công")
                .build();
    }

    @GetMapping("/levels/{levelId}")
    @Operation(summary = "Lấy thông tin RubricLevel theo ID")
    public ResponseObject<RubricLevelResponse> getLevelById(@PathVariable String levelId) {
        return ResponseObject.<RubricLevelResponse>builder()
                .status(1000)
                .data(rubricService.getLevelById(levelId))
                .message("Lấy RubricLevel thành công")
                .build();
    }

    @GetMapping("/levels")
    @Operation(summary = "Lấy danh sách tất cả RubricLevel")
    public ResponseObject<List<RubricLevelResponse>> getAllLevels() {
        return ResponseObject.<List<RubricLevelResponse>>builder()
                .status(1000)
                .data(rubricService.getAllLevels())
                .message("Lấy danh sách RubricLevel thành công")
                .build();
    }

    @PutMapping("/levels/{levelId}")
    @Operation(summary = "Cập nhật RubricLevel theo ID")
    public ResponseObject<RubricLevelResponse> updateLevel(@PathVariable String levelId,
                                                           @RequestBody RubricLevelRequest request) {
        return ResponseObject.<RubricLevelResponse>builder()
                .status(1000)
                .data(rubricService.updateLevel(levelId, request))
                .message("Cập nhật RubricLevel thành công")
                .build();
    }

    @DeleteMapping("/levels/{levelId}")
    @Operation(summary = "Xoá RubricLevel theo ID")
    public ResponseObject<Void> deleteLevel(@PathVariable String levelId) {
        rubricService.deleteLevel(levelId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Xoá RubricLevel thành công")
                .build();
    }

    // ==================== CRITERIA LEVEL APIs ====================

    @PostMapping("/criteria/{criterionId}/levels")
    @Operation(summary = "Tạo mới CriteriaLevel (gắn level vào criterion)")
    public ResponseObject<CriteriaLevelResponse> createCriteriaLevel(@PathVariable String criterionId,
                                                                       @RequestBody CriteriaLevelRequest request) {
        return ResponseObject.<CriteriaLevelResponse>builder()
                .status(1000)
                .data(rubricService.createCriteriaLevel(criterionId, request))
                .message("Tạo CriteriaLevel thành công")
                .build();
    }

    @GetMapping("/criteria-levels/{id}")
    @Operation(summary = "Lấy thông tin CriteriaLevel theo ID")
    public ResponseObject<CriteriaLevelResponse> getCriteriaLevelById(@PathVariable String id) {
        return ResponseObject.<CriteriaLevelResponse>builder()
                .status(1000)
                .data(rubricService.getCriteriaLevelById(id))
                .message("Lấy CriteriaLevel thành công")
                .build();
    }

    @PutMapping("/criteria-levels/{id}")
    @Operation(summary = "Cập nhật CriteriaLevel theo ID")
    public ResponseObject<CriteriaLevelResponse> updateCriteriaLevel(@PathVariable String id,
                                                                       @RequestBody CriteriaLevelRequest request) {
        return ResponseObject.<CriteriaLevelResponse>builder()
                .status(1000)
                .data(rubricService.updateCriteriaLevel(id, request))
                .message("Cập nhật CriteriaLevel thành công")
                .build();
    }

    @DeleteMapping("/criteria-levels/{id}")
    @Operation(summary = "Xoá CriteriaLevel theo ID")
    public ResponseObject<Void> deleteCriteriaLevel(@PathVariable String id) {
        rubricService.deleteCriteriaLevel(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Xoá CriteriaLevel thành công")
                .build();
    }
}
