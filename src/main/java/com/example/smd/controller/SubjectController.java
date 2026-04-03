package com.example.smd.controller;

import com.example.smd.dto.request.subject.SubjectPublishRequest;
import com.example.smd.dto.request.subject.SubjectRequest;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import com.example.smd.services.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Subject", description = "Endpoints for managing academic subjects and their metadata")
@SecurityRequirement(name = "bearerAuth")
public class SubjectController {

    SubjectService subjectService;

    @PostMapping
    @Operation(summary = "Create a new subject", description = "Registers a new subject in the system with unique subject code")
    @PreAuthorize("hasAuthority('SUBJECT_CREATE')")
    public ResponseObject<SubjectResponse> create(@RequestBody @Valid SubjectRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.create(request, userId))
                .message("Subject created successfully")
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Advanced search subjects with pagination",
            description = "Search by 'code' or 'name' directly at the database level. Filters by status is also supported."
    )
    public ResponseObject<PagedResponse<SubjectResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "code") String searchBy,
            @Parameter(
                    description = "Filter subjects by their current lifecycle status. Valid values are:\n" +
                            "| Status | Description |\n" +
                            "| :--- | :--- |\n" +
                            "| **DRAFT** | **Biên soạn nháp:** Initial creation. Basic info (code, name) is being entered. Not visible to Curriculum proposals. |\n" +
                            "| **DEFINED** | **Đã xác định nội dung:** Description and credits are finalized. Subject is now eligible to be included in a Curriculum draft for VP approval. |\n" +
                            "| **WAITING_SYLLABUS** | **Chờ Syllabus:** The Curriculum containing this subject is approved. System is waiting for the Department to submit a detailed Syllabus. |\n" +
                            "| **PENDING_REVIEW** | **Đợi phân công review:** Submitted and awaiting review assignment. |\n"+
                            "| **COMPLETED** | **Hoàn tất:** Detailed Syllabus is approved and linked. The subject is fully validated and ready for teaching/enrollment. |\n" +
                            "| **ARCHIVED** | **Lưu trữ:** Subject is retired from active curricula. Kept as Read-only for historical academic records. |"
            )
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "subjectCode") String sortBy,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "asc") String direction,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<SubjectResponse>>builder()
                .data(PagedResponse.of(subjectService.getAll(search, searchBy,status, departmentId, pageable, userId)))
                .message("Subjects retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get subject detail by ID",
            description = "Retrieves full subject details including Department and CLOs using JOIN FETCH for optimal performance."
    )
    public ResponseObject<SubjectResponse> getDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.getDetail(id, userId))
                .message("Subject detail retrieved successfully")
                .build();
    }

    @GetMapping("/code/{subjectCode}")
    public ResponseObject<SubjectResponse> getByCode(@PathVariable String subjectCode, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SubjectResponse>builder()
                .status(1000)
                .data(subjectService.getDetailByCode(subjectCode, userId))
                .message("Get subject detail by code successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update subject information")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    public ResponseObject<SubjectResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.update(id, request, userId))
                .message("Subject updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBJECT_DELETE')")
    @Operation(summary = "Soft delete subject", description = "Sets subject status to inactive instead of hard deleting from database")
    public ResponseObject<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        subjectService.delete(id, userId);
        return ResponseObject.<Void>builder()
                .message("Subject deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('SUBJECT_PUBLISH')")
    @Operation(
            summary = "Publish a subject syllabus",
            description = "Change subject status from Draft to Published. Requires 'SUBJECT_PUBLISH' authority and a valid decision number."
    )
    public ResponseObject<SubjectResponse> publish(
            @PathVariable UUID id,
            @RequestBody @Valid SubjectPublishRequest request) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.publishSubject(id, request.getDecisionNo()))
                .message("Subject published successfully with decision: " + request.getDecisionNo())
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    @Operation(
            summary = "Update Subject Lifecycle Status (Cập nhật trạng thái vòng đời Môn học)",
            description = "### 📚 Quy trình điều phối Môn học (Subject Workflow):\n" +
                    "Quản lý tính sẵn sàng của môn học từ khi khởi tạo khung đến khi có Syllabus hoàn chỉnh:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) | Ràng buộc hệ thống |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** HoCF mới tạo mã và tên môn học. Thông tin cơ bản đang được nhập liệu. | Chỉ hiển thị nội bộ cho HoCF. |\n" +
                    "| **DEFINED** | **Đã xác định:** Bản mô tả môn học và số tín chỉ đã hoàn thiện, sẵn sàng để đưa vào dự thảo Curriculum trình VP. | Có thể gán vào Curriculum DRAFT. |\n" +
                    "| **WAITING_SYLLABUS** | **Chờ Syllabus:** Curriculum chứa môn này đã được VP duyệt. Hệ thống đang đợi HoPDC nộp Syllabus chi tiết. | **Mở quyền soạn thảo Syllabus.** |\n" +
                    "| **PENDING_REVIEW** | **Chờ thẩm định:** Syllabus chi tiết đã nộp và đang đợi Hội đồng phân công Reviewer đánh giá. | Khóa chỉnh sửa nội dung Syllabus. |\n" +
                    "| **COMPLETED** | **Hoàn tất:** Syllabus đã được duyệt và liên kết chính thức. Môn học sẵn sàng để giảng dạy/tuyển sinh. | Dữ liệu chuyển sang Read-only. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Môn học không còn nằm trong chương trình giảng dạy chính thức, giữ lại để đối soát lịch sử. | Ẩn khỏi danh sách đăng ký mới. |\n\n"
    )
    public ResponseObject<SubjectResponse> publishInternal(
            @PathVariable UUID id,
            @RequestParam String newStatus) {
        return ResponseObject.<SubjectResponse>builder()
                .data(subjectService.updateSubjectStatus(id, newStatus))
                .message("Subject has been successfully moved to internal review status.")
                .build();
    }

    @PatchMapping("/curriculum/{curriculum_id}/department/{department_id}/status")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    @Operation(
            summary = "Update Subject Lifecycle Status (Cập nhật trạng thái vòng đời Môn học)",
            description = "### 📚 Quy trình điều phối Môn học (Subject Workflow):\n" +
                    "Quản lý tính sẵn sàng của môn học từ khi khởi tạo khung đến khi có Syllabus hoàn chỉnh:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) | Ràng buộc hệ thống |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** HoCF mới tạo mã và tên môn học. Thông tin cơ bản đang được nhập liệu. | Chỉ hiển thị nội bộ cho HoCF. |\n" +
                    "| **DEFINED** | **Đã xác định:** Bản mô tả môn học và số tín chỉ đã hoàn thiện, sẵn sàng để đưa vào dự thảo Curriculum trình VP. | Có thể gán vào Curriculum DRAFT. |\n" +
                    "| **WAITING_SYLLABUS** | **Chờ Syllabus:** Curriculum chứa môn này đã được VP duyệt. Hệ thống đang đợi HoPDC nộp Syllabus chi tiết. | **Mở quyền soạn thảo Syllabus.** |\n" +
                    "| **PENDING_REVIEW** | **Chờ thẩm định:** Syllabus chi tiết đã nộp và đang đợi Hội đồng phân công Reviewer đánh giá. | Khóa chỉnh sửa nội dung Syllabus. |\n" +
                    "| **COMPLETED** | **Hoàn tất:** Syllabus đã được duyệt và liên kết chính thức. Môn học sẵn sàng để giảng dạy/tuyển sinh. | Dữ liệu chuyển sang Read-only. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Môn học không còn nằm trong chương trình giảng dạy chính thức, giữ lại để đối soát lịch sử. | Ẩn khỏi danh sách đăng ký mới. |\n\n"
    )
    public ResponseObject<SubjectResponse> changeStatus(
            @PathVariable UUID curriculum_id,
            @PathVariable UUID department_id,
            @RequestParam String newStatus,
            @RequestParam(required = false) String oldStatus
    ) {
        int updatedCount = subjectService.updateAllSubjectStatusInCurriculum(curriculum_id, department_id, newStatus, oldStatus);
        return ResponseObject.<SubjectResponse>builder()
                .status(1000)
                .data(SubjectResponse.builder().build()) // Return empty response as bulk operation
                .message("Cập nhật trạng thái " + updatedCount + " môn học thành công")
                .build();
    }

    @PatchMapping("/curriculum/{curriculum_id}/department/{department_id}/decision")
    @PreAuthorize("hasAuthority('SUBJECT_UPDATE')")
    @Operation(
            summary = "Update Decision Number for all subjects in Curriculum and Department",
            description = "Cập nhật đồng loạt số quyết định và ngày phê duyệt cho các môn học thuộc Khoa và Khung chương trình cụ thể."
    )
    public ResponseObject<Integer> updateBulkDecision(
            @Parameter(description = "ID của Khung chương trình", required = true)
            @RequestParam UUID curriculumId,

            @Parameter(description = "ID của Khoa/Bộ môn", required = true)
            @RequestParam UUID departmentId,

            @Parameter(description = "Số quyết định ban hành", required = true)
            @RequestParam String decisionNo
    ) {
        int updatedCount = subjectService.updateDecisionOnly(curriculumId, departmentId, decisionNo);
        return ResponseObject.<Integer>builder()
                .status(1000)
                .data(updatedCount)
                .message("Đã cập nhật số quyết định cho " + updatedCount + " môn học thành công.")
                .build();
    }

    @PostMapping(value = "/import", consumes =
                MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SUBJECT_CREATE')")
    @Operation(
            summary = "Import subjects from Excel",
            description = "Import subject records from Excel columns: subjectCode, subjectName, credits, degreeLevel, timeAllocation, description, departmentCode, mintopass, studentLimit, studentTasks, scoringScale"
    )
    public ResponseObject<ImportSubjectResponse> importSubjects(@RequestParam("file") MultipartFile file) {
        return ResponseObject.<ImportSubjectResponse>builder()
                .status(1000)
                .data(subjectService.importSubjects(file))
                .message("Import subjects successfully")
                .build();
    }

    @GetMapping("/department/{departmentId}")
    @Operation(
            summary = "Get subjects by Department ID",
            description = "Retrieve a list of subjects belonging to a specific department. \n\n" +
                    "### 🔒 Access Control Policy:\n" +
                    "* **STUDENT / LECTURER**: Can **ONLY** view subjects with **COMPLETED** status.\n"+
                    "* ** Other Actors **: Can view subjects in **ALL** statuses.\n"
    )
    public ResponseObject<List<SubjectResponse>> getByDepartment(
            @PathVariable UUID departmentId,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<SubjectResponse>>builder()
                .status(1000)
                .data(subjectService.getSubjectsByDepartment(departmentId, userId))
                .message("Get subjects by department successfully")
                .build();
    }
}
