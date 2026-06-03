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



    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin chi tiết Rubric theo ID")
    public ResponseObject<RubricResponse> getRubricById(@PathVariable String id) {
        return ResponseObject.<RubricResponse>builder()
                .status(1000)
                .data(rubricService.getRubricById(id))
                .message("Lấy Rubric thành công")
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

}
