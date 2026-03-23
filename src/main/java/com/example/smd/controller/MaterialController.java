package com.example.smd.controller;

import com.example.smd.dto.request.MaterialRequest;
import com.example.smd.dto.response.MaterialResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Material", description = "Material Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class MaterialController {
    MaterialService materialService;

    @PostMapping
    @PreAuthorize("hasAuthority('MATERIAL_CREATE')")
    @Operation(summary = "Create new material for a syllabus")
    public ResponseObject<MaterialResponse> create(@RequestBody @Valid MaterialRequest request) {
        return ResponseObject.<MaterialResponse>builder()
                .status(1000).data(materialService.create(request)).build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all materials by Syllabus ID")
    public ResponseObject<List<MaterialResponse>> getAllBySyllabus(
            @PathVariable UUID syllabusId,
            @Parameter(description = "Filter by status (DRAFT, DEFINED, PUBLISHED, WAITING_SYLLABUS, PENDING_REVIEW, ARCHIVED)")
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<MaterialResponse>>builder()
                .status(1000)
                .data(materialService.getAllBySyllabus(syllabusId, status, userId))
                .message("Materials retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_UPDATE')")
    @Operation(summary = "Update material details")
    public ResponseObject<MaterialResponse> update(@PathVariable UUID id, @RequestBody MaterialRequest request) {
        return ResponseObject.<MaterialResponse>builder()
                .status(1000).data(materialService.update(id, request)).build();
    }

    @PatchMapping("/syllabus/{syllabusId}/status")
    @PreAuthorize("hasAuthority('MATERIAL_UPDATE_STATUS')")
    @Operation(
            summary = "Update Material Lifecycle Status",
            description = "### Quy trình phê duyệt tài liệu học tập (Material Approval):\n" +
                    "Cập nhật trạng thái để kiểm soát quyền truy cập và hiển thị của tài liệu:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **PENDING_REVIEW** | **Chờ duyệt:** Giảng viên đã upload xong và đang đợi HoD/FDC kiểm tra nội dung. |\n" +
                    "| **REVISION_REQUESTED** | **Yêu cầu chỉnh sửa:** Tài liệu cần được điều chỉnh hoặc bổ sung theo feedback của người duyệt. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Nội dung tài liệu đạt yêu cầu, sẵn sàng để đưa vào Syllabus chính thức. |\n" +
                    "| **REJECTED** | **Từ chối:** Tài liệu không phù hợp với chương trình đào tạo hoặc vi phạm quy định. |\n" +
                    "| **PUBLISHED** | **Đã ban hành:** Tài liệu chính thức hiển thị cho sinh viên xem và tải về trên Portal. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Tài liệu của các học kỳ cũ, không còn áp dụng nhưng được giữ lại để đối soát lịch sử. |"
    )
    public ResponseObject<Void> updateStatusBySyllabus(
            @Parameter(description = "ID of the Syllabus")
            @PathVariable String syllabusId,

            @Parameter(description = "New status to apply (e.g., PUBLISHED)")
            @RequestParam String newStatus) {

        materialService.updateMaterialStatusBySyllabus(syllabusId, newStatus);

        return ResponseObject.<Void>builder()
                .status(1000)
                .message("All materials in syllabus " + syllabusId + " updated to " + newStatus)
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_DELETE')")
    @Operation(summary = "Delete a material")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        materialService.delete(id);
        return ResponseObject.<Void>builder().status(1000).message("Deleted successfully").build();
    }

    @GetMapping("/{materialId}")
    @Operation(
            summary = "Get detailed information of a Material",
            description = "Retrieves full details of a specific material by its UUID. \n\n" +
                    "### 🔒 Security Note:\n" +
                    "* Public roles (Student/Lecturer) can only access **PUBLISHED** materials.\n" +
                    "* Unauthorized access to DRAFT/ARCHIVED materials will return 403 Forbidden."
    )
    public ResponseObject<MaterialResponse> getDetail(
            @Parameter(description = "UUID of the material to retrieve")
            @PathVariable UUID materialId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<MaterialResponse>builder()
                .status(1000)
                .data(materialService.getDetail(materialId, userId))
                .message("Material details retrieved successfully")
                .build();
    }
}
