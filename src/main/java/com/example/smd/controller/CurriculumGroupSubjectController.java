package com.example.smd.controller;

import com.example.smd.dto.request.CurriculumGroupSubjectRequest;
import com.example.smd.dto.response.CurriculumGroupSubjectResponse;
import com.example.smd.dto.response.CurriculumSemesterMappingsResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SubjectSimpleResponse;
import com.example.smd.dto.response.curriculumgroupsubject.ImportCurriculumGroupSubjectResponse;
import com.example.smd.services.CurriculumGroupSubjectService;
import com.example.smd.dto.request.BulkSemesterMappingRequest;
import com.example.smd.dto.response.BulkSemesterMappingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@Tag(name = "Curriculum Group Subject", description = "Curriculum" +
        "-Group-Subject Mapping APIs - Quản lý môn học trong khung " +
        "chương trình")
@RestController
@RequestMapping("/api/curriculum-group-subjects")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CurriculumGroupSubjectController {

    private final CurriculumGroupSubjectService curriculumGroupSubjectService;

    /**
     * API thêm môn học vào curriculum thông qua group
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CURRICULUM_UPDATE', " +
            "'SUBJECT_UPDATE', 'GROUP_UPDATE')") // Yêu cầu quyền cập nhật curriculum, subject hoặc group
    @Operation(
        summary = "Add subject to curriculum (via group)",
        description = "Create a mapping between Curriculum, Group (optional), and Subject. " +
                      "This assigns a subject to a curriculum, optionally within a group group, " +
                      "and specifies the recommended semester. Requires CURRICULUM_UPDATE permission."
    )
    public ResponseObject<CurriculumGroupSubjectResponse> createCurriculumGroupSubject(
            @Valid @RequestBody CurriculumGroupSubjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<CurriculumGroupSubjectResponse>builder()
                .status(1000)
                .data(curriculumGroupSubjectService.createCurriculumGroupSubject(request, userId))
                .message("Subject added to curriculum successfully")
                .build();
    }

    /**
     * API bulk configure semester-group-subject mappings
     */
    @PostMapping("/bulk-configure")
    @PreAuthorize("hasAnyAuthority('CURRICULUM_UPDATE', 'SUBJECT_UPDATE', 'GROUP_UPDATE')")
    @Operation(
            summary = "Bulk configure semester-subject-group mappings ",
            description = "Delete mappings by deleteSubjectsList (if provided) before processing insert. Then atomically insert multiple " +
                    "subject-semester-group mappings for a curriculum. when a duplicate subject exists in the same curriculum: " +
                    "if groupId is not null then add; if groupId is null then skip that item and continue processing others."
    )
    public ResponseObject<BulkSemesterMappingResponse> bulkConfigureSemesterMappings(
            @Valid @RequestBody BulkSemesterMappingRequest request
    ) {
        BulkSemesterMappingResponse response =
                curriculumGroupSubjectService.bulkConfigureSemesterMappingsPut(request);

        return ResponseObject.<BulkSemesterMappingResponse>builder()
                .status(response.isSuccess() ? 1000 : 400)
                .data(response)
                .message(response.isSuccess() ?
                        "Bulk mapping completed successfully" :
                        "Bulk mapping completed with validation issues")
                .build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('CURRICULUM_UPDATE', 'SUBJECT_UPDATE', 'GROUP_UPDATE')")
    @Operation(
            summary = "Import curriculum-group-subject mappings from Excel",
            description = "Import mappings from Excel columns: Group code, Subject Code, Semester. " +
                    "Curriculum is provided by request parameter curriculumId."
    )
    public ResponseObject<ImportCurriculumGroupSubjectResponse> importCurriculumGroupSubjectMappings(
            @RequestParam("file") MultipartFile file,
            @RequestParam UUID curriculumId
    ) {
        return ResponseObject.<ImportCurriculumGroupSubjectResponse>builder()
                .status(1000)
                .data(curriculumGroupSubjectService.importCurriculumGroupSubjectMappings(file, curriculumId))
                .message("Import curriculum-group-subject mappings successfully")
                .build();
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export curriculum-group-subject mappings to Excel",
            description = "Export file with first row Curriculum Code/Curriculum Name and second row headers: " +
                    "Group subject, Subject Code, Subject Name, Semester"
    )
    public ResponseEntity<InputStreamResource> exportCurriculumGroupSubjectMappings(
            @RequestParam UUID curriculumId
    ) {
        ByteArrayInputStream excel = curriculumGroupSubjectService.exportCurriculumGroupSubjectMappings(curriculumId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=curriculum-group-subjects.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(excel));
    }

    /**
     * API lấy danh sách subjects theo curriculum hoặc group với phân trang
     */
    @GetMapping("/subjects")
    @Operation(
        summary = "Get subjects by curriculum or group with pagination",
        description = "Get subjects in a curriculum or group by ID. " +
                      "SearchType must be either 'curriculum' or 'group'. " +
                      "Sort format: field (semester, credits), direction (asc/desc). " +
                      "Response includes semester information from curriculum mapping."
    )
    public ResponseObject<PagedResponse<SubjectSimpleResponse>> getSubjects(
            @Parameter(description = "Search type: 'curriculum' or 'group' (required)", required = true)
            @RequestParam(name = "searchType") String searchType,

            @Parameter(description = "ID of curriculum or group (UUID, required)", required = true)
            @RequestParam(name = "searchId") String searchId,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field and direction: semester or credits with asc|desc (e.g., semester,asc or credits,desc)")
            @RequestParam(defaultValue = "semester,asc") String[] sort
    ) {
        Page<SubjectSimpleResponse> subjects = curriculumGroupSubjectService.searchSubjects(
            searchType, searchId, page, size, sort
        );

        return ResponseObject.<PagedResponse<SubjectSimpleResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(subjects))
                .message("Get subjects successfully")
                .build();
    }

    @GetMapping("/subjects/by-curriculum")
    @Operation(
            summary = "Get subjects by curriculum and department",
            description = "Get subjects by curriculumId and departmentId. " +
                    "Optional curriculum status filter: if provided and curriculum status mismatches, return an empty list. " +
                    "Response includes subjectId, subjectCode, subjectName, subject status, and semester."
    )
    public ResponseObject<List<SubjectSimpleResponse>> getSubjectsByCurriculumAndDepartment(
            @Parameter(description = "Curriculum ID (UUID, required)", required = true)
            @RequestParam(name = "curriculumId") String curriculumId,

            @Parameter(description = "Department ID (UUID, required)", required = true)
            @RequestParam(name = "departmentId") String departmentId,

            @Parameter(description = "Curriculum status filter (optional)")
            @RequestParam(name = "status", required = false) String status
    ) {
        List<SubjectSimpleResponse> subjects =
                curriculumGroupSubjectService.getSubjectsByCurriculumStatusAndDepartment(
                        curriculumId,
                        status,
                        departmentId
                );

        return ResponseObject.<List<SubjectSimpleResponse>>builder()
                .status(1000)
                .data(subjects)
                .message("Get subjects successfully")
                .build();
    }

        @GetMapping("/semester-mappings")
        @Operation(
        summary = "Get semester mappings by curriculumId",
        description = "Return curriculum subject mappings grouped by semester. " +
                  "Each subject includes subjectId, subjectCode, subjectName, and groupId."
        )
        public ResponseObject<CurriculumSemesterMappingsResponse> getSemesterMappingsByCurriculum(
            @Parameter(description = "Curriculum ID (UUID, required)", required = true)
            @RequestParam(name = "curriculumId") String curriculumId
        ) {
        CurriculumSemesterMappingsResponse response =
            curriculumGroupSubjectService.getSemesterMappingsByCurriculum(curriculumId);

        return ResponseObject.<CurriculumSemesterMappingsResponse>builder()
            .status(1000)
            .data(response)
            .message("Get semester mappings successfully")
            .build();
        }


}
