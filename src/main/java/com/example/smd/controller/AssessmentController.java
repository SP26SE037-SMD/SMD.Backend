package com.example.smd.controller;

import com.example.smd.dto.request.AssessmentRequest;
import com.example.smd.dto.request.session.SessionMaterialBlockBulkRequest;
import com.example.smd.dto.response.AssessmentResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.validate.AssessmentValidationResult;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.entities.Assessment;
import com.example.smd.services.AssessmentService;
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
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "part,asc") String[] sort) {
        return ResponseObject.<PagedResponse<AssessmentResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(assessmentService.getAllAssessments(syllabusId, search, page, size, sort)))
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

    @PostMapping("/bluk")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Create new assessment")
    public ResponseObject<List<AssessmentResponse>> createAssessmentBluk(@Valid @RequestBody List<AssessmentRequest> request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<List<AssessmentResponse>>builder()
                .status(1000)
                .data(assessmentService.createAssessmentsBluk(request, userId))
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



    @PostMapping("/syllabus/{syllabusId}/validate")
    @Operation(
            summary = "Kiểm tra tính hợp lệ của cấu trúc Điểm số / Đánh giá (Assessments)",
            description = "Hàm này tính toán và kiểm tra các ràng buộc của các bài đánh giá dựa trên danh sách Assessment truyền vào và các Assessment đã có sẵn trong Database. " +
                    "Logic xử lý được phân loại chi tiết dựa trên **Assessment Category** và **Assessment Type**:\n\n" +
                    "* **Assessment Category (Danh mục đánh giá)**: Hệ thống tra cứu cơ sở dữ liệu để phân loại chính xác các bài đánh giá thành 2 nhóm:\n" +
                    "  * **Summative (Tổng kết / Cuối kỳ)**: Bắt buộc phải có ít nhất 1 bài đánh giá và không được vượt quá số lượng tối đa cho phép (hiện tại là 2).\n" +
                    "  * **Formative (Quá trình)**: Bắt buộc phải có ít nhất 1 bài đánh giá quá trình nếu tổng trọng số của môn học chưa đạt mức 100% tuyệt đối.\n" +
                    "* **Assessment Type (Loại hình đánh giá)**: Thể hiện hình thức kiểm tra cụ thể của từng bài (VD: *Final Exam, Project, PE* thuộc nhóm Summative; *Quiz, Assignment, Lab* thuộc nhóm Formative), giúp hệ thống kiểm soát đúng tính chất của từng cột điểm.\n" +
                    "* **Trọng số (Weight)**: Tổng trọng số của tất cả bài đánh giá (cũ và mới) phải đạt chính xác 100%. Hệ thống báo lỗi nếu tỷ lệ này bị THIẾU hoặc DƯ.\n" +
                    "* **Tổng số lượng bài (Total Count)**: Khống chế tối đa 6 bài đánh giá trong một Syllabus để tránh tình trạng quá tải cho sinh viên.\n\n" +
                    "**Kết quả trả về:** Bao gồm bảng tóm tắt (`summary`) chứa thông tin tổng trọng số, tổng số bài, số lượng theo Category và danh sách mã lỗi cảnh báo (VD: `WEIGHT_SHORTAGE`, `MISSING_FINAL_ASSESSMENT`...) nếu vi phạm quy chế."
    )
    public ResponseObject<AssessmentValidationResult> validateAssessment(
            @RequestBody List<AssessmentRequest> inputs, UUID syllabusId) {
        return ResponseObject.<AssessmentValidationResult>builder()
                .status(1000)
                .data(assessmentService.validate(inputs, syllabusId))
                .message("Validate assessment successfully")
                .build();
    }
}
