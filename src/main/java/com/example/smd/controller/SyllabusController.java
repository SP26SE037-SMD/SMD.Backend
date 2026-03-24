package com.example.smd.controller;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.ImpactResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.enums.SyllabusActionType;
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
        public ResponseObject<SyllabusResponse> create(@RequestBody @Valid SyllabusRequest request, @PathVariable String email) {
        SyllabusResponse response = syllabusService.create(request);

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
            summary = "Change Syllabus Status (Cập nhật trạng thái quy trình Đề cương)",
            description = "### 🔄 Quy trình vòng đời của Đề cương môn học (Syllabus Workflow):\n" +
                    "Trạng thái này điều khiển quyền chỉnh sửa và khả năng hiển thị của toàn bộ học liệu, tài liệu tham khảo và CLO.\n\n" +
                    "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Quyền hạn & Ràng buộc |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **UNDER_DEVELOPMENT** | **Đang biên soạn:** Giai đoạn tập trung xây dựng nội dung chi tiết, chọn lọc tài liệu học tập (Textbook/Reference) và Slides. | Cho phép sửa nội dung. |\n" +
                    "| **PENDING_REVIEW** | **Chờ duyệt:** Đề cương đã hoàn thiện và đang nằm trong danh sách chờ Hội đồng/HoD phân công Reviewer. | Khóa chỉnh sửa tạm thời. |\n" +
                    "| **REVISION_REQ** | **Yêu cầu chỉnh sửa:** Reviewer đã gửi Feedback. Giảng viên cần cập nhật lại nội dung theo yêu cầu. | Mở lại quyền chỉnh sửa. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Nội dung đã thông qua về mặt chuyên môn, sẵn sàng để đóng gói ban hành. | Khóa nội dung. |\n" +
                    "| **REJECTED** | **Từ chối:** Đề cương không đạt yêu cầu hệ thống hoặc bị loại bỏ khỏi kế hoạch đào tạo. | Ngừng quy trình. |\n" +
                    "| **PUBLISHED** | **Ban hành:** Đề cương chính thức có hiệu lực. Sinh viên có thể xem tài liệu và CLO bắt đầu được dùng để mapping PLO. | Khóa vĩnh viễn (Read-only). |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Đề cương hết hiệu lực (do đổi phiên bản hoặc đổi chương trình), giữ lại để tra cứu lịch sử. | Ẩn khỏi danh sách hiện hành. |\n\n"
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
            @RequestBody @Valid SyllabusRequest request) {
        return ResponseObject.<SyllabusResponse>builder()
                .data(syllabusService.update(id, request))
                .message("Syllabus updated successfully")
                .build();
    }

    @PatchMapping("/{id}/account/{accountId}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE_STATUS')")
    @Operation(
            summary = "Change Syllabus Status (Cập nhật trạng thái quy trình Đề cương)",
            description = "### 🔄 Quy trình vòng đời của Đề cương môn học (Syllabus Workflow):\n" +
                    "Trạng thái này điều khiển quyền chỉnh sửa và khả năng hiển thị của toàn bộ học liệu, tài liệu tham khảo và CLO.\n\n" +
                    "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Quyền hạn & Ràng buộc |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **UNDER_DEVELOPMENT** | **Đang biên soạn:** Giai đoạn tập trung xây dựng nội dung chi tiết, chọn lọc tài liệu học tập (Textbook/Reference) và Slides. | Cho phép sửa nội dung. |\n" +
                    "| **PENDING_REVIEW** | **Chờ duyệt:** Đề cương đã hoàn thiện và đang nằm trong danh sách chờ Hội đồng/HoD phân công Reviewer. | Khóa chỉnh sửa tạm thời. |\n" +
                    "| **REVISION_REQ** | **Yêu cầu chỉnh sửa:** Reviewer đã gửi Feedback. Giảng viên cần cập nhật lại nội dung theo yêu cầu. | Mở lại quyền chỉnh sửa. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Nội dung đã thông qua về mặt chuyên môn, sẵn sàng để đóng gói ban hành. | Khóa nội dung. |\n" +
                    "| **REJECTED** | **Từ chối:** Đề cương không đạt yêu cầu hệ thống hoặc bị loại bỏ khỏi kế hoạch đào tạo. | Ngừng quy trình. |\n" +
                    "| **PUBLISHED** | **Ban hành:** Đề cương chính thức có hiệu lực. Sinh viên có thể xem tài liệu và CLO bắt đầu được dùng để mapping PLO. | Khóa vĩnh viễn (Read-only). |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Đề cương hết hiệu lực (do đổi phiên bản hoặc đổi chương trình), giữ lại để tra cứu lịch sử. | Ẩn khỏi danh sách hiện hành. |\n\n" +
                    "### ⚠️ Lưu ý quan trọng:\n" +
                    "1. **Đồng bộ CLO:** Khi Syllabus chuyển sang `PUBLISHED`, hệ thống sẽ tự động khóa tất cả CLO liên quan để đảm bảo tính toàn vẹn dữ liệu.\n" +
                    "2. **Hành động biên soạn:** Các tác vụ thêm/xóa Tài liệu (Material/Source) chỉ được thực hiện khi trạng thái là **UNDER_DEVELOPMENT** hoặc **REVISION_REQUESTED**."
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
    public ResponseObject<Void> delete(@PathVariable UUID id, @PathVariable String accountId) {

        var account =  accountService.getAccountById(accountId);
        SyllabusResponse response = syllabusService.getDetail(id, accountId);

        syllabusService.delete(id);
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
    public ResponseEntity<ComparisonResult> compareSyllabusVersions(
            @RequestParam("oldSyllabusId") UUID oldSyllabusId,
            @RequestParam("newSyllabusId") UUID newSyllabusId) {

        // Gọi Service xử lý logic: Query DB -> AI Analysis -> Result
        ComparisonResult analysis = embeddingService.compareSyllabus(oldSyllabusId, newSyllabusId);

        // Trả về mã 200 OK kèm cục dữ liệu phân tích
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/check-impact")
    public ResponseEntity<List<ImpactResponse>> checkProgramImpact(
            @RequestParam("rootId") UUID rootId,
            @RequestBody List<String> removedConcepts) {

        // Gọi Service để quét qua toàn bộ chuỗi môn học phụ thuộc
        List<ImpactResponse> impactReports = new ArrayList<>();
        for (String removedConcept : removedConcepts) {
            var response = embeddingService.checkImpact(removedConcept, rootId);
            impactReports.add(response);
        }

        return ResponseEntity.ok(impactReports);
    }
}
