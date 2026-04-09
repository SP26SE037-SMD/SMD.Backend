package com.example.smd.controller;

import com.example.smd.dto.request.SourceRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SourceResponse;
import com.example.smd.services.SourceService;
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
@RequestMapping("/api/sources")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Source", description = "Quản lý tài liệu tham khảo (Sách, Giáo trình, Link)")
@SecurityRequirement(name = "bearerAuth")
public class SourceController {
        SourceService service;

        @PostMapping
        @Operation(summary = "Tạo mới tài liệu tham khảo", description = "Tạo một tài liệu mới (Textbook, Reference,...) vào hệ thống.")
        @PreAuthorize("hasAuthority('SOURCE_CREATE')")
        public ResponseObject<SourceResponse> create(@RequestBody @Valid SourceRequest request) {
                return ResponseObject.<SourceResponse>builder()
                                .status(1000)
                                .data(service.create(request))
                                .message("Source created successfully")
                                .build();
        }

        @GetMapping
        @Operation(summary = "Lấy danh sách tài liệu", description = "Hỗ trợ phân trang, tìm kiếm theo tên/tác giả và lọc theo loại tài liệu.")
        public ResponseObject<PagedResponse<SourceResponse>> getAll(
                        @RequestParam(required = false) String search,

                        @Parameter(description = "Loại tài liệu (TEXTBOOK, REFERENCE_BOOK, ONLINE_COURSE, DOCUMENTATION, JOURNAL_PAPER, ARTICLE)") @RequestParam(required = false) String type,

                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                return ResponseObject.<PagedResponse<SourceResponse>>builder()
                                .status(1000)
                                .data(PagedResponse.of(service.getAll(search, type, page, size)))
                                .message("Get sources list successfully")
                                .build();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Xem chi tiết tài liệu", description = "Lấy thông tin chi tiết của một tài liệu dựa trên ID.")
        public ResponseObject<SourceResponse> getDetail(@PathVariable UUID id) {
                return ResponseObject.<SourceResponse>builder()
                                .status(1000)
                                .data(service.getDetail(id))
                                .message("Get source detail successfully") // Bổ sung message theo ý bạn
                                .build();
        }

        @PutMapping("/{id}")
        @Operation(summary = "Cập nhật tài liệu", description = "Cập nhật thông tin tài liệu đã tồn tại dựa trên ID.")
        @PreAuthorize("hasAuthority('SOURCE_UPDATE')")
        public ResponseObject<SourceResponse> update(@PathVariable UUID id, @RequestBody SourceRequest request) {
                return ResponseObject.<SourceResponse>builder()
                                .status(1000)
                                .data(service.update(id, request))
                                .message("Source updated successfully")
                                .build();
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Xóa tài liệu", description = "Xóa vĩnh viễn tài liệu khỏi hệ thống.")
        @PreAuthorize("hasAuthority('SOURCE_DELETE')")
        public ResponseObject<Void> delete(@PathVariable UUID id) {
                service.delete(id);
                return ResponseObject.<Void>builder()
                                .status(1000)
                                .message("Source deleted successfully")
                                .build();
        }

        @GetMapping("/subject/{subjectId}")
        @Operation(summary = "Get all sources for a subject", description = "Retrieves a consolidated list of all reference sources across all syllabuses linked to the specified Subject ID.")
        public ResponseObject<List<SourceResponse>> getBySubject(@PathVariable UUID subjectId) {
                return ResponseObject.<List<SourceResponse>>builder()
                                .status(1000)
                                .data(service.getSourcesBySubject(subjectId))
                                .message("Sources retrieved successfully for Subject ID: " + subjectId)
                                .build();
        }
}
