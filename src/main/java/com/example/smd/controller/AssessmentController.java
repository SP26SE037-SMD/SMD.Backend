package com.example.smd.controller;

import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.response.AssessmentResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Assessment", description = "Assessment Management APIs")
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @GetMapping
    @Operation(summary = "Get all assessments with pagination and filters")
    public ResponseObject<PagedResponse<AssessmentResponse>> getAllAssessments(
            @RequestParam(required = false) UUID syllabusId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "part,asc") String[] sort,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<PagedResponse<AssessmentResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(assessmentService.getAllAssessments(syllabusId, status, search, page, size, sort, userId)))
                .message("Get all assessments successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment by ID")
        public ResponseObject<AssessmentResponse> getAssessmentById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.getAssessmentById(id, userId))
                .message("Get assessment successfully")
                .build();
    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Get all assessments by syllabus")
    public ResponseObject<List<AssessmentResponse>> getAssessmentsBySyllabus(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<AssessmentResponse>>builder()
                .status(1000)
                .data(assessmentService.getAssessmentsBySyllabus(syllabusId))
                .message("Get assessments by syllabus successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new assessment")
    public ResponseObject<AssessmentResponse> createAssessment(@Valid @RequestBody AssessmentRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.createAssessment(request, userId))
                .message("Create assessment successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Update assessment by ID")
    public ResponseObject<AssessmentResponse> updateAssessment(
            @PathVariable UUID id,
            @Valid @RequestBody AssessmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.updateAssessment(id, request, userId))
                .message("Update assessment successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(
            summary = "Update Assessment Lifecycle Status (Cập nhật trạng thái bài đánh giá)",
            description = "### Quy trình quản lý đánh giá và đo lường (Assessment Workflow):\n" +
                    "Trạng thái của Assessment kiểm soát quyền biên soạn đề bài, ma trận đáp án (Rubric) và thời điểm công bố điểm:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) | Ràng buộc hệ thống |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **DRAFT** | **Biên soạn:** Giảng viên đang thiết kế hình thức đánh giá, trọng số (%) và gán CLO. | Chỉ giảng viên thấy. |\n" +
                    "| **PENDING_REVIEW** | **Chờ thẩm định:** Đề bài và ma trận đánh giá đã xong, đang đợi HoD/FDC duyệt tính phù hợp. | Khóa chỉnh sửa nội dung. |\n" +
                    "| **REVISION_REQ** | **Yêu cầu chỉnh sửa:** Cần điều chỉnh lại trọng số hoặc nội dung câu hỏi theo feedback. | Mở lại quyền chỉnh sửa. |\n" +
                    "| **APPROVED** | **Đã duyệt:** Cấu trúc bài đánh giá đạt yêu cầu, sẵn sàng để đưa vào Syllabus chính thức. | Không được sửa trọng số. |\n" +
                    "| **OPEN** | **Đang diễn ra:** Bài kiểm tra đã được kích hoạt, sinh viên bắt đầu làm bài hoặc nộp bài. | Cấm sửa đề/đáp án. |\n" +
                    "| **GRADING** | **Đang chấm điểm:** Thời gian làm bài đã hết, giảng viên đang thực hiện chấm điểm và nhận xét. | Sinh viên chưa thấy điểm. |\n" +
                    "| **PUBLISHED** | **Đã công bố:** Điểm số và nhận xét chính thức hiển thị trên Portal cho sinh viên. | Khóa toàn bộ dữ liệu điểm. |\n" +
                    "| **ARCHIVED** | **Lưu trữ:** Dữ liệu đánh giá của học kỳ cũ, dùng để đối soát và hậu kiểm (Audit). | Chế độ Read-only vĩnh viễn. |\n\n"
    )
    public ResponseObject<AssessmentResponse> updateAssessmentStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        return ResponseObject.<AssessmentResponse>builder()
                .status(1000)
                .data(assessmentService.updateAssessmentStatus(id, status))
                .message("Update assessment status successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Soft delete assessment")
        public ResponseObject<Boolean> deleteAssessment(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(assessmentService.deleteAssessment(id, userId))
                .message("Delete assessment successfully")
                .build();
    }
}
