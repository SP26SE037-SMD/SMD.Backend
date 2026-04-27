package com.example.smd.controller;

import com.example.smd.dto.request.po.POsCreateRequest;
import com.example.smd.dto.request.po.POsRequest;
import com.example.smd.dto.response.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.services.POsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "POs", description = "Program Outcomes Management APIs - Higher level educational goals")
@SecurityRequirement(name = "bearerAuth")
public class POsController {
    POsService poService;

    @PostMapping("/major/{majorId}")
    @PreAuthorize("hasAuthority('POS_CREATE')")
    @Operation(summary = "Create multiple POs", description = "Create POs linked to a specific Major via PathVariable.")
    public ResponseObject<List<POsResponse>> createBulk(
            @PathVariable String majorId,
            @RequestBody @Valid List<POsCreateRequest> request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<POsResponse>>builder()
                .status(1000)
                .data(poService.createBulkPos(majorId, request, userId))
                .message("POs created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_UPDATE')")
    @Operation(summary = "Update PO", description = "Update po_code and description for a specific PO.")
    public ResponseObject<POsResponse> update(
            @PathVariable String id,
            @RequestBody @Valid POsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<POsResponse>builder()
                .status(1000)
                .data(poService.updatePo(id, request, userId))
                .message("PO updated successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PO Detail", description = "Retrieve detailed information of a specific PO by its ID.")
    public ResponseObject<POsResponse> getDetail(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<POsResponse>builder()
                .status(1000)
                .data(poService.getPoDetail(id, userId))
                .message("Get PO detail successfully")
                .build();
    }

    @GetMapping("/major/{majorId}")
    @Operation(summary = "Get POs by Major ID", description = "Retrieve a paginated list of POs belonging to a specific Major.")
    public ResponseObject<PagedResponse<POsResponse>> getByMajor(
            @PathVariable String majorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "poCode") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @AuthenticationPrincipal Jwt jwt
            ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<PagedResponse<POsResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(poService.getPosByMajor(majorId, page, size, sortBy, direction, userId)))
                .message("Get POs by major successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_DELETE')")
    @Operation(summary = "Delete PO", description = "Soft delete a PO by moving its status to ARCHIVED.")
    public ResponseObject<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        poService.deletePo(id, userId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("PO deleted successfully")
                .build();
    }

    @PatchMapping("/major/{majorId}/status")
    @PreAuthorize("hasAuthority('POS_UPDATE_STATUS')")
    @Operation(
            summary = "Update POs status by Major",
            description = "### Quy trình cập nhật trạng thái của Chuẩn đầu ra (PO):\n" +
                    "Sử dụng để đồng bộ trạng thái của toàn bộ PO thuộc một Chuyên ngành (Major):\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **DRAFT** | **Bản thảo:** PO đang soạn thảo, chưa áp dụng để đối soát với PLO. |\n" +
                    "| **INTERNAL_REVIEW** | **Công khai nội bộ:** Đang đợi Hội đồng rà soát tính phù hợp của chuẩn đầu ra. |\n" +
                    "| **PUBLISHED** | **Đã ban hành:** PO chính thức có hiệu lực, dùng làm căn cứ thiết kế khung chương trình. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** PO hết hiệu lực hoặc lỗi thời, giữ lại làm dữ liệu lịch sử. |"
    )
    public ResponseObject<Void> changeStatus(
            @PathVariable String majorId,
            @RequestParam String newStatus
    ) {
        poService.updateStatusByMajor(majorId, newStatus);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("POs status updated successfully for Major: " + majorId)
                .build();
    }

    @PostMapping(value = "/major/{majorId}/validate-po")
    public ResponseObject<ComplianceCheckResponse> validatePo(
            @PathVariable("majorId") UUID majorId
    ) {
        var response = poService.validatePoCheck(majorId);
        return ResponseObject.<ComplianceCheckResponse>builder()
                .status(1000)
                .data(response)
                .message("POs status validate successfully for Major: " + majorId)
                .build();
    }
}
