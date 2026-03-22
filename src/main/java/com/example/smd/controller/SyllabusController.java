package com.example.smd.controller;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.request.SyllabusRequest;
import com.example.smd.dto.response.ComparisonResult;
import com.example.smd.dto.response.ImpactResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.syllabus.SyllabusResponse;
import com.example.smd.enums.SyllabusActionType;
import com.example.smd.services.EmbeddingService;
import com.example.smd.services.SyllabusActionLogService;
import com.example.smd.services.SyllabusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/account/{email}")
    @Operation(summary = "Create a new syllabus", description = "Initializes a syllabus for a specific subject with status 'DRAFT'")
//    @PreAuthorize("hasAuthority('SYLLABUS_CREATE')")
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
    @Operation(summary = "Get all syllabuses by Subject ID", description = "Retrieves all versions of syllabuses associated with a specific subject")
    public ResponseObject<List<SyllabusResponse>> getAllBySubject(@PathVariable UUID subjectId) {
        return ResponseObject.<List<SyllabusResponse>>builder()
                .data(syllabusService.getAllBySubject(subjectId))
                .message("Syllabuses retrieved successfully for subject: " + subjectId)
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get syllabus detail", description = "Retrieves full details of a syllabus including sessions and assessments")
    public ResponseObject<SyllabusResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<SyllabusResponse>builder()
                .data(syllabusService.getDetail(id))
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

    @PatchMapping("/{id}/account/{email}/status")
    @PreAuthorize("hasAuthority('SYLLABUS_UPDATE_STATUS')")
    @Operation(
            summary = "Change Syllabus Status",
            description = "### Quy trình cập nhật trạng thái của CLO:\n" +
                    "Chọn một trong các giá trị sau từ danh sách thả xuống:\n\n" +
                    "| Status | Mô tả chi tiết (Nghiệp vụ) |\n" +
                    "| :--- | :--- |\n" +
                    "| **PENDING_REVIEW** | Chờ duyệt: Đã gửi yêu cầu và đợi HoD phê duyệt. |\n" +
                    "| **REVISION_REQUESTED** | Yêu cầu chỉnh sửa: Cần sửa lại theo feedback của người duyệt. |\n" +
                    "| **APPROVED** | Đã duyệt: Nội dung đạt yêu cầu, sẵn sàng để xuất bản. |\n" +
                    "| **REJECTED** | Bị từ chối: Nội dung không đạt yêu cầu hệ thống. |\n" +
                    "| **PUBLISHED** | Đã xuất bản: CLO chính thức có hiệu lực cho môn học. |\n"
    )
    public ResponseObject<SyllabusResponse> updateStatus(
            @PathVariable UUID id,
            @PathVariable String email,
            @RequestParam String status) {

        SyllabusResponse response = syllabusService.getDetail(id);

        SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
        logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
        logRequest.setActionByEmail(email); // Truyền email vào đây

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

    @DeleteMapping("/{id}/account/{email}")
    @Operation(summary = "Delete syllabus", description = "Sets status to 'ARCHIVED' instead of physical deletion")
//    @PreAuthorize("hasAuthority('SYLLABUS_DELETE')")
    public ResponseObject<Void> delete(@PathVariable UUID id, @PathVariable String email) {
        syllabusService.delete(id);

        SyllabusResponse response = syllabusService.getDetail(id);

        SyllabusActionLogRequest logRequest = new SyllabusActionLogRequest();
        logRequest.setSyllabusId(UUID.fromString(response.getSyllabusId()));
        logRequest.setActionByEmail(email); // Truyền email vào đây
        logRequest.setActionType(SyllabusActionType.ARCHIVE.toString());
        logRequest.setNote("Hệ thống: Ẩn đề cương cho môn học.");

        // 4. Lưu Log
        syllabusActionLogService.createLog(logRequest);
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
