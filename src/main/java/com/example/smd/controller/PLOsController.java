package com.example.smd.controller;

import com.example.smd.dto.request.plo.PLOsCreateRequest;
import com.example.smd.dto.request.plo.PLOsRequest;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.PLOsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "PLOs", description = "Program Learning Outcomes Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PLOsController {
    PLOsService ploService;

    @PostMapping("/curriculum/{curriculumId}")
    @PreAuthorize("hasAuthority('PLOS_CREATE')")
    @Operation(summary = "Create multiple PLOs", description = "Create PLOs linked to a specific Curriculum via PathVariable.")
    public ResponseObject<List<PLOsResponse>> createBulk(
            @PathVariable String curriculumId,
            @RequestBody @Valid List<PLOsCreateRequest> request) {
        return ResponseObject.<List<PLOsResponse>>builder()
                .status(1000)
                .data(ploService.createBulkPlos(curriculumId, request))
                .message("PLOs created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLOS_UPDATE')")
    @Operation(summary = "Update PLO", description = "Update plo_code and description.")
    public ResponseObject<PLOsResponse> update(@PathVariable String id, @RequestBody @Valid PLOsRequest request) {
        return ResponseObject.<PLOsResponse>builder()
                .status(1000)
                .data(ploService.updatePlo(id, request))
                .message("PLO updated successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PLO Detail")
    public ResponseObject<PLOsResponse> getDetail(@PathVariable String id) {
        return ResponseObject.<PLOsResponse>builder()
                .status(1000)
                .data(ploService.getPloDetail(id))
                .message("Get PLO detail successfully")
                .build();
    }

    @GetMapping("/curriculum/{curriculumId}")
    @Operation(summary = "Get PLOs by Curriculum ID")
    public ResponseObject<PagedResponse<PLOsResponse>> getByCurriculum(
            @PathVariable String curriculumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<PLOsResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(ploService.getPlosByCurriculum(curriculumId, page, size)))
                .message("Get PLOs by major successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLOS_DELETE')")
    @Operation(summary = "Delete PLO")
    public ResponseObject<Void> delete(@PathVariable String id) {
        ploService.deletePlo(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("PLO deleted successfully")
                .build();
    }

    @PatchMapping("/curriculum/{curriculum_id}/status")
    @PreAuthorize("hasAuthority('PLOS_UPDATE_STATUS')")
    @Operation(
            summary = "Update PLOs status",
            description = "### Quy trình cập nhật trạng thái của Chuẩn đầu ra ngành (PLO):\n" +
                    "Chọn một trong các giá trị sau để điều phối việc ánh xạ (mapping) PLO vào các môn học:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **DRAFT** | **Bản thảo:** PLO đang trong giai đoạn soạn thảo hoặc chỉnh sửa ngôn ngữ, chưa hiển thị để mapping với Syllabus. |\n" +
                    "| **INTERNAL_REVIEW** | **Công khai nội bộ:** Đã hoàn thiện nội dung và mở quyền cho Hội đồng khoa học/Giảng viên vào rà soát tính phù hợp. |\n" +
                    "| **PUBLISHED** | **Đã ban hành:** PLO chính thức có hiệu lực, được dùng làm căn cứ để thiết kế Syllabus và đánh giá khung chương trình. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** PLO không còn phù hợp với bộ tiêu chuẩn mới, giữ lại để đối chiếu các khóa cũ (Read-only). |"
    )
    public ResponseObject<PLOsResponse> changeStatus(
            @PathVariable String curriculum_id,
            @RequestParam String newStatus
    ) {
        ploService.updateStatusByCurriculum(curriculum_id, newStatus);
        return ResponseObject.<PLOsResponse>builder()
                .status(1000)
                .message("Cập nhật trạng thái PLO thành công")
                .build();
    }
}
