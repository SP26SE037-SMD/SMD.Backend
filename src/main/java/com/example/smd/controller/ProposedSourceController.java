package com.example.smd.controller;

import com.example.smd.dto.request.ProposedSourceRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ProposedSourceResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.ProposedSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proposed-sources")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Proposed Source", description = "Quản lý đề xuất tài liệu tham khảo cho môn học (ProposedSource)")
@SecurityRequirement(name = "bearerAuth")
public class ProposedSourceController {

    ProposedSourceService service;

    // -------------------------------------------------------
    // POST /api/proposed-sources
    // -------------------------------------------------------
    @PostMapping
    @Operation(
            summary = "Tạo đề xuất tài liệu cho môn học",
            description = "Liên kết một tài liệu (Source) với một môn học (Subject) dưới dạng đề xuất tham khảo."
    )
//    @PreAuthorize("hasAuthority('PROPOSED_SOURCE_CREATE')")
    public ResponseObject<ProposedSourceResponse> create(@RequestBody @Valid ProposedSourceRequest request) {
        return ResponseObject.<ProposedSourceResponse>builder()
                .status(1000)
                .data(service.create(request))
                .message("Proposed source created successfully")
                .build();
    }

    // -------------------------------------------------------
    // GET /api/proposed-sources  (paginated + search)
    // -------------------------------------------------------
    @GetMapping
    @Operation(
            summary = "Lấy danh sách tất cả đề xuất tài liệu",
            description = "Hỗ trợ phân trang và tìm kiếm theo tên/mã tài liệu hoặc tên/mã môn học."
    )
    public ResponseObject<PagedResponse<ProposedSourceResponse>> getAll(
            @Parameter(description = "Tìm kiếm theo sourceCode, sourceName, subjectCode, subjectName")
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseObject.<PagedResponse<ProposedSourceResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(service.getAll(search, page, size)))
                .message("Get proposed sources list successfully")
                .build();
    }

    // -------------------------------------------------------
    // GET /api/proposed-sources/{id}
    // -------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(
            summary = "Xem chi tiết đề xuất tài liệu",
            description = "Lấy thông tin chi tiết của một đề xuất tài liệu dựa trên ID."
    )
    public ResponseObject<ProposedSourceResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<ProposedSourceResponse>builder()
                .status(1000)
                .data(service.getDetail(id))
                .message("Get proposed source detail successfully")
                .build();
    }

    // -------------------------------------------------------
    // GET /api/proposed-sources/subject/{subjectId}
    // -------------------------------------------------------
    @GetMapping("/subject/{subjectId}")
    @Operation(
            summary = "Lấy danh sách đề xuất tài liệu theo môn học",
            description = "Trả về tất cả tài liệu được đề xuất cho một môn học cụ thể."
    )
    public ResponseObject<List<ProposedSourceResponse>> getBySubject(@PathVariable UUID subjectId) {
        return ResponseObject.<List<ProposedSourceResponse>>builder()
                .status(1000)
                .data(service.getBySubject(subjectId))
                .message("Get proposed sources by subject successfully")
                .build();
    }

    // -------------------------------------------------------
    // GET /api/proposed-sources/source/{sourceId}
    // -------------------------------------------------------
    @GetMapping("/source/{sourceId}")
    @Operation(
            summary = "Lấy danh sách đề xuất theo tài liệu",
            description = "Trả về tất cả môn học đang đề xuất sử dụng một tài liệu cụ thể."
    )
    public ResponseObject<List<ProposedSourceResponse>> getBySource(@PathVariable UUID sourceId) {
        return ResponseObject.<List<ProposedSourceResponse>>builder()
                .status(1000)
                .data(service.getBySource(sourceId))
                .message("Get proposed sources by source successfully")
                .build();
    }

    // -------------------------------------------------------
    // PUT /api/proposed-sources/{id}
    // -------------------------------------------------------
    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật đề xuất tài liệu",
            description = "Thay đổi tài liệu hoặc môn học được liên kết trong một đề xuất tham khảo."
    )
//    @PreAuthorize("hasAuthority('PROPOSED_SOURCE_UPDATE')")
    public ResponseObject<ProposedSourceResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid ProposedSourceRequest request) {
        return ResponseObject.<ProposedSourceResponse>builder()
                .status(1000)
                .data(service.update(id, request))
                .message("Proposed source updated successfully")
                .build();
    }

    // -------------------------------------------------------
    // DELETE /api/proposed-sources/{id}
    // -------------------------------------------------------
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa đề xuất tài liệu",
            description = "Xóa liên kết đề xuất tài liệu. Không ảnh hưởng đến tài liệu hay môn học gốc."
    )
//    @PreAuthorize("hasAuthority('PROPOSED_SOURCE_DELETE')")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Proposed source deleted successfully")
                .build();
    }
}
