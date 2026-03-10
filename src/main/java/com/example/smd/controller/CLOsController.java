package com.example.smd.controller;

import com.example.smd.dto.request.clo.CLOsRequest;
import com.example.smd.dto.request.clo.CloCheckRequest;
import com.example.smd.dto.request.clo.CloGenerationRequest;
import com.example.smd.dto.response.*;
import com.example.smd.enums.SyllabusStatus;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "CLOs", description = "Course Learning Outcomes Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CLOsController {

    CLOsService closService;
    GeminiService geminiService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLOS_CREATE')")
    @Operation(summary = "Create a new CLO for a specific Subject")
    public ResponseObject<CLOsResponse> create(@RequestBody @Valid CLOsRequest request) {
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .data(closService.createClo(request))
                .message("CLO created successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information of a CLO by ID")
    public ResponseObject<CLOsResponse> getDetail(@PathVariable String id) {
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .data(closService.getCloDetail(id))
                .message("Get CLO detail successfully")
                .build();
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(summary = "Get list of CLOs by Subject ID with pagination")
    public ResponseObject<PagedResponse<CLOsResponse>> getBySubject(
            @PathVariable String subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<CLOsResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(closService.getClosBySubject(subjectId, page, size)))
                .message("Get CLOs by subject successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLOS_UPDATE')")
    @Operation(summary = "Update CLO information including Code, Name, Description, and Bloom Level")
    public ResponseObject<CLOsResponse> update(
            @PathVariable String id,
            @RequestBody @Valid CLOsRequest request) {
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .data(closService.updateClo(id, request))
                .message("CLO updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLOS_DELETE')")
    @Operation(summary = "Delete a CLO")
    public ResponseObject<Void> delete(@PathVariable String id) {
        closService.deleteClo(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("CLO deleted successfully")
                .build();
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('CLOS_GENERATE')")
    @Operation(summary = "Generate a new CLO using AI")
    public ResponseEntity<CLOsGenerationResponse> generateClo(@RequestBody @Valid CloGenerationRequest request) {
        CLOsGenerationResponse result = geminiService.generateClo(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/check")
    @PreAuthorize("hasAuthority('CLOS_CHECK')")
    @Operation(summary = "Validate CLO against Bloom's Taxonomy")
    public ResponseEntity<CloCheckResponse> checkClo(@RequestBody @Valid CloCheckRequest request) {
        CloCheckResponse result = geminiService.checkClo(request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLOS_UPDATE_STATUS')")
    @Operation(
            summary = "Change CLO Status",
            description = "### Quy trình cập nhật trạng thái của CLO:\n" +
                    "Chọn một trong các giá trị sau từ danh sách thả xuống:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **PENDING_REVIEW** | Chờ duyệt: Đã gửi yêu cầu và đợi HoD phê duyệt. |\n" +
                    "| **IN_REVIEW** | Đang đánh giá: Chuyên gia đang xem xét nội dung. |\n" +
                    "| **REVISION_REQUESTED** | Yêu cầu chỉnh sửa: Cần sửa lại theo feedback của người duyệt. |\n" +
                    "| **APPROVED** | Đã duyệt: Nội dung đạt yêu cầu, sẵn sàng để xuất bản. |\n" +
                    "| **REJECTED** | Bị từ chối: Nội dung không đạt yêu cầu hệ thống. |\n" +
                    "| **PUBLISHED** | Đã xuất bản: CLO chính thức có hiệu lực cho môn học. |\n"
    )
    public ResponseObject<CLOsResponse> changeStatus(
            @PathVariable String id,
            @RequestParam String newStatus // Swagger sẽ hiện Dropdown ở đây
    ) {
        return ResponseObject.<CLOsResponse>builder()
                .status(1000)
                .data(closService.updateStatus(id, newStatus))
                .message("Cập nhật trạng thái CLO thành công")
                .build();
    }
}
