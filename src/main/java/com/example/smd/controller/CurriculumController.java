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

            @Parameter(description = "Filter by status: ACTIVE, INACTIVE, DRAFT, ARCHIVED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field and direction: [field, asc|desc]")
            @RequestParam(defaultValue = "curriculumCode,asc") String[] sort
    ) {
        Page<CurriculumResponse> curriculums = curriculumService.getAllCurriculums(
                search, searchBy, status, page, size, sort
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
            @RequestBody @Valid CurriculumCreateRequest request
    ) {
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.createCurriculum(request))
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
            @PathVariable String id
    ) {
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.getCurriculumDetail(id))
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
            @PathVariable String code
    ) {
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.getCurriculumByCode(code))
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
            @RequestBody @Valid CurriculumCreateRequest request
    ) {
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.updateCurriculum(id, request))
                .message("Curriculum updated successfully")
                .build();
    }



    /**
     * API cập nhật status của curriculum
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    @Operation(
            summary = "Update Curriculum Status",
            description = "### Quy trình cập nhật trạng thái vòng đời của Khung chương trình (Curriculum):\n" +
                    "Chọn một trong các giá trị sau để điều phối luồng phê duyệt và biên soạn Syllabus:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **DRAFT** | **Biên soạn:** Giai đoạn khởi tạo khung. Chỉ hiển thị nội bộ cho HoC/FDC để thiết kế danh mục môn học. |\n" +
                    "| **INTERNAL_REVIEW_WITHOUT_ENACTMENT** | **Chờ duyệt hành chính:** HoP đã chốt danh sách môn nhưng chưa có quyết định từ Văn phòng (VP). Chỉ được sửa mô tả môn học. |\n" +
                    "| **INTERNAL_REVIEW_WITH_ENACTMENT** | **Đã duyệt hành chính:** Đã có quyết định chính thức từ VP. **Kích hoạt quyền biên soạn Syllabus** cho các bộ môn. |\n" +
                    "| **PUBLISHED** | **Đã xuất bản:** Khung chương trình chính thức áp dụng. Toàn bộ danh mục môn học và cấu trúc bị khóa để đảm bảo tính pháp lý. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Khung cũ đã hết hiệu lực, không áp dụng cho các khóa mới nhưng vẫn giữ lại để tra cứu lịch sử (Read-only). |"
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
            @RequestParam int endYear
    ) {
        return ResponseObject.<CurriculumResponse>builder()
                .status(1000)
                .data(curriculumService.updateCurriculumEndYear(id, endYear))
                .message("Curriculum end-year updated successfully")
                .build();
    }
}
