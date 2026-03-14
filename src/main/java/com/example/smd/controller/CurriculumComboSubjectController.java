package com.example.smd.controller;

import com.example.smd.dto.request.CurriculumComboSubjectRequest;
import com.example.smd.dto.response.CurriculumComboSubjectResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SubjectSimpleResponse;
import com.example.smd.services.CurriculumComboSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Curriculum Combo Subject", description = "Curriculum-Combo-Subject Mapping APIs - Quản lý môn học trong khung chương trình")
@RestController
@RequestMapping("/api/curriculum-combo-subjects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CurriculumComboSubjectController {

    private final CurriculumComboSubjectService curriculumComboSubjectService;

    /**
     * API thêm môn học vào curriculum thông qua combo
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CURRICULUM_UPDATE', " +
            "'SUBJECT_UPDATE', 'COMBO_UPDATE')") // Yêu cầu quyền cập nhật curriculum, subject hoặc combo
    @Operation(
        summary = "Add subject to curriculum (via combo)",
        description = "Create a mapping between Curriculum, Combo (optional), and Subject. " +
                      "This assigns a subject to a curriculum, optionally within a combo group, " +
                      "and specifies the recommended semester. Requires CURRICULUM_UPDATE permission."
    )
    public ResponseObject<CurriculumComboSubjectResponse> createCurriculumComboSubject(
            @Valid @RequestBody CurriculumComboSubjectRequest request
    ) {
        return ResponseObject.<CurriculumComboSubjectResponse>builder()
                .status(1000)
                .data(curriculumComboSubjectService.createCurriculumComboSubject(request))
                .message("Subject added to curriculum successfully")
                .build();
    }

    /**
     * API lấy danh sách subjects theo curriculum hoặc combo với search và phân trang
     */
    @GetMapping("/subjects")
    @Operation(
        summary = "Get subjects by curriculum or combo with search and pagination",
        description = "Search subjects in a curriculum or combo by subject code or name. " +
                      "SearchType must be either 'curriculum' or 'combo'. " +
                      "Search parameter can be empty to get all subjects. " +
                      "Sort format: field (semester, credits), direction (asc/desc). " +
                      "Response includes semester information from curriculum mapping."
    )
    public ResponseObject<PagedResponse<SubjectSimpleResponse>> getSubjects(
            @Parameter(description = "Search keyword for subject code or name (can be empty)")
            @RequestParam(required = false, name = "search") String search,

            @Parameter(description = "Search type: 'curriculum' or 'combo' (required)", required = true)
            @RequestParam(name = "searchType") String searchType,

            @Parameter(description = "ID of curriculum or combo (UUID, required)", required = true)
            @RequestParam(name = "searchId") String searchId,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field and direction: semester or credits with asc|desc (e.g., semester,asc or credits,desc)")
            @RequestParam(defaultValue = "semester,asc") String[] sort
    ) {
        Page<SubjectSimpleResponse> subjects = curriculumComboSubjectService.searchSubjects(
            search, searchType, searchId, page, size, sort
        );

        return ResponseObject.<PagedResponse<SubjectSimpleResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(subjects))
                .message("Get subjects successfully")
                .build();
    }
}
