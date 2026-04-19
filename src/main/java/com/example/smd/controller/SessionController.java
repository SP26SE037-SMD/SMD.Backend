package com.example.smd.controller;

import com.example.smd.dto.request.session.SessionRequest;
import com.example.smd.dto.request.session.SessionNumberListRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.services.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Session", description = "Session Management APIs")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class  SessionController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "Get all sessions with pagination and filters")
    public ResponseObject<PagedResponse<SessionResponse>> getAllSessions(
            @RequestParam(required = false) UUID syllabusId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "sessionNumber,asc") String[] sort,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<PagedResponse<SessionResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(sessionService.getAllSessions(
                        syllabusId, status, search, page, size, sort, userId
                )))
                .message("Get all sessions successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID")
    public ResponseObject<SessionResponse> getSessionById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.getSessionById(id, userId))
                .message("Get session successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all sessions by syllabus")
    public ResponseObject<List<SessionResponse>> getSessionsBySyllabus(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<SessionResponse>>builder()
                .status(1000)
                .data(sessionService.getSessionsBySyllabus(syllabusId))
                .message("Get sessions by syllabus successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new session")
    public ResponseObject<SessionResponse> createSession(@Valid @RequestBody SessionRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.createSession(request, userId))
                .message("Create session successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update session by ID")
    public ResponseObject<SessionResponse> updateSession(
            @PathVariable UUID id,
            @Valid @RequestBody SessionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.updateSession(id, request, userId))
                .message("Update session successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(
            summary = "Update Session Lifecycle Status (Cập nhật trạng thái buổi học)",
            description = "### 📅 Quy trình điều phối kế hoạch giảng dạy (Session Workflow):\n" +
                    "Trạng thái của Session kiểm soát việc lập lịch, nội dung giảng dạy và khả năng đồng bộ với thời khóa biểu:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) | Ràng buộc hệ thống |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** Giảng viên mới tạo tiêu đề và thứ tự buổi học (Session Order). | Chỉ giảng viên nhìn thấy. |\n" +
                    "| **PENDING_REVIEW** | **Chờ duyệt:** Nội dung buổi học đã xong, đang đợi HoD kiểm tra tính phù hợp với Syllabus. | Khóa toàn bộ quyền chỉnh sửa. |\n" +
                    "| **REVISION_REQUESTED**| **Yêu cầu sửa:** Cần điều chỉnh lại thời lượng hoặc mục tiêu bài học theo feedback của người duyệt. | Mở lại quyền chỉnh sửa nội dung. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Kế hoạch buổi học đạt yêu cầu, sẵn sàng để gán vào lịch trình giảng dạy. | Khóa nội dung, chuẩn bị ban hành. |\n" +
                    "| **PUBLISHED** | **Ban hành:** Nội dung buổi học chính thức hiển thị trên Portal cho sinh viên chuẩn bị bài. | Sinh viên bắt đầu thấy tài liệu & mục tiêu. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Buổi học thuộc các học kỳ cũ hoặc đã bị hủy/thay thế bằng bài giảng mới. | Chế độ Read-only vĩnh viễn. |\n\n"
    )
    public ResponseObject<SessionResponse> updateSessionStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        return ResponseObject.<SessionResponse>builder()
                .status(1000)
                .data(sessionService.updateSessionStatus(id, status))
                .message("Update session status successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete session by status rule")
    public ResponseObject<Boolean> deleteSession(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(sessionService.deleteSession(id, userId))
                .message("Delete session successfully")
                .build();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Delete session list by syllabus and session numbers")
    public ResponseObject<Boolean> deleteSessionList(
            @RequestParam UUID syllabusId,
            @Valid @RequestBody SessionNumberListRequest request
    ) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(sessionService.deleteSessionListBySyllabusAndSessionNumbers(syllabusId, request))
                .message("Delete session list successfully")
                .build();
    }

    @PatchMapping("/syllabus/{syllabusId}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(
            summary = "Update Material Lifecycle Status",
            description = "### Quy trình phê duyệt tài liệu học tập (Material Approval):\n" +
                    "Cập nhật trạng thái để kiểm soát quyền truy cập và hiển thị của tài liệu:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **DRAFT** | **Khởi tạo:** Giảng viên mới tạo định danh tài liệu (Tên tài liệu, Loại: PDF/Slide/Link). | Chỉ người tạo nhìn thấy. |\n" +
                    "| **REVISION_REQUESTED** | **Yêu cầu chỉnh sửa:** Tài liệu cần được điều chỉnh hoặc bổ sung theo feedback của người duyệt. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Nội dung tài liệu đạt yêu cầu, sẵn sàng để đưa vào Syllabus chính thức. |\n" +
                    "| **REJECTED** | **Từ chối:** Tài liệu không phù hợp với chương trình đào tạo hoặc vi phạm quy định. |\n" +
                    "| **PUBLISHED** | **Đã ban hành:** Tài liệu chính thức hiển thị cho sinh viên xem và tải về trên Portal. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Tài liệu của các học kỳ cũ, không còn áp dụng nhưng được giữ lại để đối soát lịch sử. |"
    )
    public ResponseObject<Void> updateStatusBySyllabus(
            @Parameter(description = "ID of the Syllabus")
            @PathVariable String syllabusId,

            @Parameter(description = "New status to apply (e.g., PUBLISHED)")
            @RequestParam String newStatus) {

        sessionService.updateSessionStatusBySyllabus(syllabusId, newStatus);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("All materials in syllabus " + syllabusId + " updated to " + newStatus)
                .build();
    }
}
