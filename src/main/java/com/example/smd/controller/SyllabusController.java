package com.example.smd.controller;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.ImpactResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SyllabusActionType;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.services.AccountService;
import com.example.smd.services.EmbeddingService;
import com.example.smd.services.SyllabusActionLogService;
import com.example.smd.services.SyllabusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/syllabus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Syllabus", description = "Endpoints for managing course syllabuses and their review lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class SyllabusController {

    SyllabusService syllabusService;
    SyllabusActionLogService syllabusActionLogService;
    EmbeddingService embeddingService;
    AccountService accountService;

    @PostMapping("/account/{email}")
    @Operation(summary = "Create a new syllabus", description = "Initializes a syllabus for a specific subject with status 'DRAFT'")
    @PreAuthorize("hasAuthority('SYLLABUS_CREATE')")
        public ResponseObject<SyllabusResponse> create(
                @RequestBody @Valid SyllabusRequest request,
                @PathVariable String email,
                @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        SyllabusResponse response = syllabusService.create(request, userId);

        SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
        logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
        logRequest.setActionByEmail(email); // Truyền email vào đây
        logRequest.setActionType(SyllabusActionType.CREATE.toString());
        logRequest.setNote("Hệ thống: Khởi tạo đề cương mới cho môn học.");

        // 4. Lưu Log
        syllabusActionLogService.createLog(logRequest);

        return ResponseObject.<SyllabusResponse>builder()
                .data(response)
                .message("Syllabus created successfully")
                .build();
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(
            summary = "Update Syllabus Lifecycle Status (Cập nhật trạng thái Đề cương)",
            description = "### 🔄 Quy trình vòng đời của Đề cương môn học (Syllabus Workflow):\n" +
                    "Trạng thái này điều khiển quyền chỉnh sửa và khả năng hiển thị của toàn bộ Session, Assessment, Material và CLO:\n\n" +
                    "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Quyền hạn & Ràng buộc |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** HoPDC mới tạo khung đề cương, chưa có nội dung chi tiết. | Chỉ người tạo nhìn thấy. |\n" +
                    "| **IN_PROGRESS** | **Đang biên soạn:** Giảng viên đang xây dựng CLO, Session, chọn lọc học liệu và thiết kế Assessment. | Cho phép sửa nội dung chi tiết. |\n" +
                    "| **PENDING_REVIEW** | **Chờ duyệt:** Đề cương đã hoàn thiện, đang đợi Hội đồng/HoD phân công Reviewer thẩm định. | Khóa chỉnh sửa tạm thời. |\n" +
                    "| **REVISION_REQUESTED**| **Yêu cầu chỉnh sửa:** Reviewer đã gửi Feedback. Giảng viên cần cập nhật nội dung theo yêu cầu. | Mở lại quyền chỉnh sửa. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Nội dung đã thông qua về mặt chuyên môn, sẵn sàng để đóng gói ban hành. | Khóa toàn bộ nội dung. |\n" +
                    "| **PUBLISHED** | **Ban hành:** Đề cương chính thức có hiệu lực. Sinh viên bắt đầu được xem tài liệu và CLO. | Khóa vĩnh viễn (Read-only). |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Đề cương hết hiệu lực (do đổi phiên bản), giữ lại để tra cứu lịch sử đào tạo. | Ẩn khỏi danh sách hiện hành. |\n\n"
    )
    public ResponseObject<List<SyllabusResponse>> getAllBySubject(
            @PathVariable UUID subjectId,
            @Parameter(description = "Filter by status (DRAFT, INTERNAL_REVIEW, PUBLISHED, ARCHIVED)")
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<SyllabusResponse>>builder()
                .status(1000)
                .data(syllabusService.getAllBySubject(subjectId, status, userId))
                .message("Syllabuses retrieved successfully for subject: " + subjectId)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get syllabus detail", description = "Retrieves full details of a syllabus including sessions and assessments")
    public ResponseObject<SyllabusResponse> getDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SyllabusResponse>builder()
                .data(syllabusService.getDetail(id, userId))
                .message("Syllabus detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update syllabus information", description = "Updates metadata like bloom level and name. Only allowed for DRAFT status.")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    public ResponseObject<SyllabusResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid SyllabusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SyllabusResponse>builder()
                .data(syllabusService.update(id, request, userId))
                .message("Syllabus updated successfully")
                .build();
    }

    @PatchMapping("/{id}/account/{accountId}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE_STATUS')")
    @Operation(
            summary = "Update Syllabus Lifecycle Status (Cập nhật trạng thái Đề cương)",
            description = "### 🔄 Quy trình vòng đời của Đề cương môn học (Syllabus Workflow):\n" +
                    "Trạng thái này điều khiển quyền chỉnh sửa và khả năng hiển thị của toàn bộ Session, Assessment, Material và CLO:\n\n" +
                    "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Quyền hạn & Ràng buộc |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** HoPDC mới tạo khung đề cương, chưa có nội dung chi tiết. | Chỉ người tạo nhìn thấy. |\n" +
                    "| **IN_PROGRESS** | **Đang biên soạn:** Giảng viên đang xây dựng CLO, Session, chọn lọc học liệu và thiết kế Assessment. | Cho phép sửa nội dung chi tiết. |\n" +
                    "| **PENDING_REVIEW** | **Chờ duyệt:** Đề cương đã hoàn thiện, đang đợi Hội đồng/HoD phân công Reviewer thẩm định. | Khóa chỉnh sửa tạm thời. |\n" +
                    "| **REVISION_REQUESTED**| **Yêu cầu chỉnh sửa:** Reviewer đã gửi Feedback. Giảng viên cần cập nhật nội dung theo yêu cầu. | Mở lại quyền chỉnh sửa. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Nội dung đã thông qua về mặt chuyên môn, sẵn sàng để đóng gói ban hành. | Khóa toàn bộ nội dung. |\n" +
                    "| **PUBLISHED** | **Ban hành:** Đề cương chính thức có hiệu lực. Sinh viên bắt đầu được xem tài liệu và CLO. | Khóa vĩnh viễn (Read-only). |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Đề cương hết hiệu lực (do đổi phiên bản), giữ lại để tra cứu lịch sử đào tạo. | Ẩn khỏi danh sách hiện hành. |\n\n"
    )
    public ResponseObject<SyllabusResponse> updateStatus(
            @PathVariable UUID id,
            @PathVariable String accountId,
            @RequestParam String status) {
        var account =  accountService.getAccountById(accountId);
        SyllabusResponse response = syllabusService.getDetail(id, accountId);

        SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
        logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
        logRequest.setActionByEmail(account.getEmail()); // Truyền email vào đây

        SyllabusActionType actionType = syllabusActionLogService.mapStatusToAction(status);

        logRequest.setActionType(actionType.toString());
        logRequest.setNote("Hệ thống: Ẩn đề cương cho môn học.");

        // 4. Lưu Log
        syllabusActionLogService.createLog(logRequest);
        return ResponseObject.<SyllabusResponse>builder()
                .data(syllabusService.updateStatus(id, status))
                .message("Syllabus status updated to: " + status)
                .build();
    }

    @DeleteMapping("/{id}/account/{accountId}")
    @Operation(summary = "Delete syllabus", description = "Sets status to 'ARCHIVED' instead of physical deletion")
    @PreAuthorize("hasAuthority('SYLLABUS_DELETE')")
    public ResponseObject<Void> delete(
            @PathVariable UUID id,
            @PathVariable String accountId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        var account =  accountService.getAccountById(accountId);
        SyllabusResponse response = syllabusService.getDetail(id, accountId);

        syllabusService.delete(id, userId);
        if(!response.getStatus().equals("DRAFT")) {
            SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
            logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
            logRequest.setActionByEmail(account.getEmail()); // Truyền email vào đây
            logRequest.setActionType(SyllabusActionType.ARCHIVE.toString());
            logRequest.setNote("Hệ thống: Ẩn đề cương cho môn học.");

            // 4. Lưu Log
            syllabusActionLogService.createLog(logRequest);
        }
        return ResponseObject.<Void>builder()
                .message("Syllabus archived successfully")
                .build();
    }

    @PostMapping("/compare")
    public ResponseObject<ComparisonResult> compareSyllabusVersions(
            @RequestParam("oldSyllabusId") UUID oldSyllabusId,
            @RequestParam("newSyllabusId") UUID newSyllabusId) {

        // Gọi Service xử lý logic: Query DB -> AI Analysis -> Result
        ComparisonResult analysis = embeddingService.compareSyllabus(oldSyllabusId, newSyllabusId);

        // Trả về mã 200 OK kèm cục dữ liệu phân tích

        return ResponseObject.<ComparisonResult>builder()
                .data(analysis)
                .message("Compare syllabus successfully")
                .build();
    }

    @PostMapping("/check-impact")
    public ResponseObject<List<ImpactResponse>> checkProgramImpact(
            @RequestParam("rootId") UUID rootId,
            @RequestBody List<String> removedConcepts) {

        // Gọi Service để quét qua toàn bộ chuỗi môn học phụ thuộc
        List<ImpactResponse> impactReports = new ArrayList<>();
        for (String removedConcept : removedConcepts) {
            var response = embeddingService.checkImpact(removedConcept, rootId);
            impactReports.add(response);
        }
        return ResponseObject.<List<ImpactResponse>>builder()
                .data(impactReports)
                .message("Compare syllabus successfully")
                .build();
    }

    @GetMapping("/pending-review/department")
    @Operation(
            summary = "Get Pending Review Syllabuses by Department",
            description = "Lấy danh sách các Đề cương đang chờ duyệt thuộc Phòng ban của người dùng hiện tại."
    )
    public ResponseObject<List<SyllabusResponse>> getPendingSyllabusesByDept(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<SyllabusResponse>>builder()
                .data(syllabusService.getSyllabusesByDepartment(userId, SyllabusStatus.PENDING_REVIEW.toString()))
                .message("Syllabuses retrieved successfully")
                .build();
    }

    @GetMapping("/in-progress/department")
    @Operation(
            summary = "Get in prgress Syllabuses by Department",
            description = "Lấy danh sách các Đề cương đang biên soạn chi tiết nội dung Phòng ban của người dùng hiện tại."
    )
    public ResponseObject<List<SyllabusResponse>> getInProgressSyllabusesByDept(
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<SyllabusResponse>>builder()
                .data(syllabusService.getSyllabusesByDepartment(userId, SyllabusStatus.IN_PROGRESS.toString()))
                .message("Syllabuses retrieved successfully")
                .build();
    }
}
