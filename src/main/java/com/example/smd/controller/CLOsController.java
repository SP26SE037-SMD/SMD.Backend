package com.example.smd.controller;

import com.example.smd.dto.request.clo.CLOsCreateRequest;
import com.example.smd.dto.request.clo.CLOsRequest;
import com.example.smd.dto.request.clo.CloCheckRequest;
import com.example.smd.dto.request.clo.CloGenerationRequest;
import com.example.smd.dto.response.*;
import com.example.smd.dto.response.clo.CLOsGenerationResponse;
import com.example.smd.dto.response.clo.CLOsResponse;
import com.example.smd.dto.response.clo.CloCheckResponse;
import com.example.smd.services.CLOsService;
import com.example.smd.services.GeminiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "CLOs", description = "Course Learning Outcomes Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CLOsController {

    CLOsService closService;
    GeminiService geminiService;

    @PostMapping("/subject/{subjectId}")
    @PreAuthorize("hasAuthority('CLOS_CREATE')")
    @Operation(summary = "Create multiple CLOs", description = "Create CLOs linked to a specific Subject via PathVariable.")
    public ResponseObject<List<CLOsResponse>> createBulk(
            @PathVariable String subjectId,
            @RequestBody @Valid List<CLOsCreateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<CLOsResponse>>builder()
                .status(1000)
                .data(closService.createBulkClos(subjectId, request, userId))
                .message("CLOs created successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information of a CLO by ID")
    public ResponseObject<CLOsResponse> getDetail(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .data(closService.getCloDetail(id, userId))
                .message("Get CLO detail successfully")
                .build();
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get list of CLOs by Subject ID with pagination")
    public ResponseObject<PagedResponse<CLOsResponse>> getBySubject(
            @PathVariable String subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<PagedResponse<CLOsResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(closService.getClosBySubject(subjectId, page, size, userId)))
                .message("Get CLOs by subject successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLOS_UPDATE')")
    @Operation(summary = "Update CLO information including Code, Name, Description, and Bloom Level")
    public ResponseObject<CLOsResponse> update(
            @PathVariable String id,
            @RequestBody @Valid CLOsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .data(closService.updateClo(id, request, userId))
                .message("CLO updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLOS_DELETE')")
    @Operation(summary = "Delete a CLO")
    public ResponseObject<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        closService.deleteClo(id, userId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("CLO deleted successfully")
                .build();
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('CLOS_GENERATE')")
    @Operation(summary = "Generate a new CLO using AI")
    public ResponseEntity<CLOsGenerationResponse> generateClo(
            @RequestBody @Valid CloGenerationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        CLOsGenerationResponse result = geminiService.generateClo(request, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/check")
    @PreAuthorize("hasAuthority('CLOS_CHECK')")
    @Operation(summary = "Validate CLO against Bloom's Taxonomy")
    public ResponseEntity<CloCheckResponse> checkClo(
            @RequestBody @Valid CloCheckRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        CloCheckResponse result = geminiService.checkClo(request, userId);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/subject/{subjectId}/status")
    @PreAuthorize("hasAuthority('CLOS_UPDATE_STATUS')")
    @Operation(
            summary = "Update CLOs Status (Cập nhật trạng thái Chuẩn đầu ra môn học)",
            description = "### 🔄 Quy trình điều phối và ánh xạ CLO (Mapping Workflow):\n" +
                    "Trạng thái của CLO quyết định khả năng hiển thị và quyền được 'khớp' (map) vào PLO của ngành:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) | Khả năng Mapping |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Bản thảo:** CLO đang trong giai đoạn biên soạn nội dung hoặc chỉnh sửa động từ (Bloom). | ❌ Không được phép |\n" +
                    "| **INTERNAL_REVIEW** | **Công khai nội bộ:** Đã hoàn thiện nội dung, mở quyền cho Trưởng bộ môn/Hội đồng rà soát tính tương thích với PLO. | ⚠️ Chỉ xem (Read-only) |\n" +
                    "| **PUBLISHED** | **Đã ban hành:** CLO chính thức có hiệu lực, được dùng làm căn cứ để thiết kế Syllabus và tính toán ma trận kỹ năng. | ✅ Chính thức |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** CLO không còn phù hợp với bộ chuẩn mới, giữ lại để đối chiếu các khóa cũ hoặc phiên bản Syllabus cũ. | 🔒 Khóa vĩnh viễn |\n\n"
    )
    public ResponseObject<CLOsResponse> changeStatus(
            @PathVariable String subjectId,
            @RequestParam String newStatus // Swagger sẽ hiện Dropdown ở đây
    ) {
        closService.updateStatusBySubject(subjectId, newStatus);
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .message("Cập nhật trạng thái CLO thành công")
                .build();
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasAuthority('CLOS_CREATE')") // Or whatever the appropriate authority is
    @Operation(summary = "Import CLO_PLO_Mapping from Excel file")
    public ResponseObject<com.example.smd.dto.response.cloplo.ImportCloPloMappingResponse> importExcel(
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ResponseObject.<com.example.smd.dto.response.cloplo.ImportCloPloMappingResponse>builder()
                .status(1000)
                .data(closService.importCloPloMapping(file))
                .message("Import CLO_PLO_Mapping completed")
                .build();
    }
}
