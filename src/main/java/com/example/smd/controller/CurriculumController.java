package com.example.smd.controller;

import com.example.smd.dto.request.curriculum.CurriculumCreateRequest;
import com.example.smd.dto.response.CurriculumResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.CurriculumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/curriculums")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Curriculum", description = "Curriculum Management APIs - Quản lý khung chương trình đào tạo")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

    CurriculumService curriculumService;

    /**
     * API lấy danh sách curriculum với phân trang và bộ lọc
     */
    @GetMapping
    @Operation(
            summary = "Get curriculums with pagination and filters",
            description = "Retrieve a paginated list of curriculums. You can filter by curriculum code, name, major, and status."
    )
    public ResponseObject<PagedResponse<CurriculumResponse>> getAllCurriculums(
            @Parameter(description = "Search keyword for curriculum code or name")
            @RequestParam(required = false) String search,

            @Parameter(description = "Search by: 'code', 'name', or 'all'")
            @RequestParam(required = false) String searchBy,

            @Parameter(
                    description = "Filter curriculums by their current lifecycle status. Valid values are:\n" +
                            "| Status | Description |\n" +
                            "| :--- | :--- |\n" +
                            "| **DRAFT** | **Biên soạn nháp:** Initial creation by HoC/FDC, not visible to other roles. |\n" +
                            "| **STRUCTURE_REVIEWED** | **Duyệt cấu trúc:** Approved by VP to continue internal detailed development. |\n" +
                            "| **SYLLABUS_DEVELOPING**| **Phát triển Syllabus:** HoC and Departments are creating detailed course syllabuses. |\n" +
                            "| **FINAL_REVIEW** | **Thẩm định cuối:** Overall content review before submitting for official signing. |\n" +
                            "| **SIGNED** | **Đã ký ban hành:** Officially enacted by the Vice President. |\n" +
                            "| **PUBLISHED** | **Công bố:** Curriculum and all linked Syllabuses are now public and viewable. |\n" +
                            "| **ARCHIVED** | **Lưu trữ:** Old version, no longer in use for new student intakes. |"
            )
            @RequestParam(required = false) String status,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field and direction: [field, asc|desc]")
            @RequestParam(defaultValue = "curriculumCode,asc") String[] sort,

            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        Page<CurriculumResponse> curriculums = curriculumService.getAllCurriculums(
                search, searchBy, status, page, size, sort, userId
        );

        return ResponseObject.<PagedResponse<CurriculumResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(curriculums))
                .message("Get all curriculums successfully")
                .build();
    }

    /**
     * API tạo curriculum mới
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CURRICULUM_CREATE')")
    @Operation(
            summary = "Create a new curriculum",
            description = "Create a new curriculum with unique curriculum code. Requires CURRICULUM_CREATE permission."
    )
    public ResponseObject<CurriculumResponse> createCurriculum(
            @RequestBody @Valid CurriculumCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.createCurriculum(request, userId))
                .message("Curriculum created successfully")
                .build();
    }

    /**
     * API lấy chi tiết curriculum theo ID
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get curriculum detail by ID",
            description = "Retrieve full curriculum details including major information."
    )
    public ResponseObject<CurriculumResponse> getCurriculumDetail(
            @Parameter(description = "Curriculum ID (UUID)")
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.getCurriculumDetail(id, userId))
                .message("Get curriculum detail successfully")
                .build();
    }

    /**
     * API lấy curriculum theo code
     */
    @GetMapping("/code/{code}")
    @Operation(
            summary = "Get curriculum by curriculum code",
            description = "Retrieve curriculum information using its unique code."
    )
    public ResponseObject<CurriculumResponse> getCurriculumByCode(
            @Parameter(description = "Curriculum code")
            @PathVariable String code,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.getCurriculumByCode(code, userId))
                .message("Get curriculum by code successfully")
                .build();
    }

    /**
     * API cập nhật curriculum
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    @Operation(
            summary = "Update an existing curriculum",
            description = "Update curriculum details. Requires CURRICULUM_UPDATE permission."
    )
    public ResponseObject<CurriculumResponse> updateCurriculum(
            @Parameter(description = "Curriculum ID (UUID)")
            @PathVariable String id,
            @RequestBody @Valid CurriculumCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.updateCurriculum(id, request, userId))
                .message("Curriculum updated successfully")
                .build();
    }



    /**
     * API cập nhật status của curriculum
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    @Operation(
            summary = "Update Curriculum Life-cycle Status",
            description = "### Curriculum Life-cycle Workflow (Quy trình vòng đời Khung chương trình):\n" +
                    "Select one of the following values to coordinate the approval and syllabus development flow:\n\n" +
                    "| Status | Business Logic Description (Mô tả nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** HoC/FDC đang thiết kế cấu trúc khung và danh mục môn học. Chỉ hiển thị nội bộ, cho phép chỉnh sửa toàn diện. |\n" +
                    "| **STRUCTURE_REVIEWED** | **Đã duyệt cấu trúc:** Văn phòng (VP) đã duyệt danh mục môn học. Chờ quyết định hành chính để triển khai tiếp. |\n" +
                    "| **SYLLABUS_DEVELOPING** | **Soạn thảo Syllabus:** Hệ thống mở quyền cho các Bộ môn bắt đầu biên soạn Syllabus chi tiết dựa trên khung đã duyệt. |\n" +
                    "| **FINAL_REVIEW** | **Rà soát tổng thể:** HoC/FDC kiểm tra lại toàn bộ nội dung Syllabus và Khung trước khi trình ký chính thức. |\n" +
                    "| **SIGNED** | **Đã ký ban hành:** VP đã ký quyết định phê duyệt. Dữ liệu được khóa để chuẩn bị xuất bản. |\n" +
                    "| **PUBLISHED** | **Đã xuất bản:** Khung chương trình chính thức áp dụng cho khóa tuyển sinh. Cho phép mọi người dùng xem (Public view). |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Khung cũ đã hết hiệu lực. Chuyển sang chế độ chỉ đọc (Read-only) để tra cứu lịch sử đào tạo. |"
    )
    public ResponseObject<CurriculumResponse> updateCurriculumStatus(
            @Parameter(description = "Curriculum ID (UUID)")
            @PathVariable String id,
            @Parameter(description = "New status: DRAFT, INTERNAL_REVIEW_WITHOUT_ENACTMENT, INTERNAL_REVIEW_WITH_ENACTMENT, PUBLISHED, ARCHIVED")
            @RequestParam String status
    ) {
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.updateCurriculumStatus(id, status))
                .message("Curriculum status updated successfully")
                .build();
    }

    /**
     * API cập nhật endYear của curriculum
     */
    @PatchMapping("/{id}/end-year")
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    public ResponseObject<CurriculumResponse> updateCurriculumEndYear(
            @Parameter(description = "Curriculum ID (UUID)")
            @PathVariable String id,

            @Parameter(description = "New end-year must be greater than or equal to start-year")
            @RequestParam int endYear,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.updateCurriculumEndYear(id, endYear, userId))
                .message("Curriculum end-year updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CURRICULUM_DELETE')")
    @Operation(summary = "Soft delete curriculum", description = "Sets curriculum status to inactive instead of hard deleting from database")
    public ResponseObject<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        curriculumService.delete(id, userId);
        return ResponseObject.<Void>builder()
                .message("Curriculum deleted successfully")
                .build();
    }
}
