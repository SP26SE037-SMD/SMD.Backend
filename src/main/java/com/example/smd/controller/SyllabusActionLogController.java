package com.example.smd.controller;

import com.example.smd.dto.request.SyllabusActionLogRequest;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SyllabusActionLogResponse;
import com.example.smd.services.SyllabusActionLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/syllabus-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Syllabus Action Logs", description = "Quản lý lịch sử tác động lên đề cương")
public class SyllabusActionLogController {
    SyllabusActionLogService logService;

//    @PostMapping
//    @Operation(summary = "Tạo log mới")
//    public ResponseObject<SyllabusActionLogResponse> create(@RequestBody SyllabusActionLogRequest request) {
//        return ResponseObject.<SyllabusActionLogResponse>builder()
//                .data(logService.createLog(request))
//                .message("Log created")
//                .build();
//    }

    @GetMapping("/syllabus/{syllabusId}")
    @Operation(summary = "Lấy danh sách log theo đề cương")
    public ResponseObject<List<SyllabusActionLogResponse>> getBySyllabus(@PathVariable UUID syllabusId) {
        return ResponseObject.<List<SyllabusActionLogResponse>>builder()
                .data(logService.getLogsBySyllabus(syllabusId))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết log")
    public ResponseObject<SyllabusActionLogResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<SyllabusActionLogResponse>builder()
                .data(logService.getDetail(id))
                .build();
    }

//    @PatchMapping("/{id}")
//    @Operation(summary = "Cập nhật ghi chú của log")
//    public ResponseObject<SyllabusActionLogResponse> updateNote(@PathVariable UUID id, @RequestParam String note) {
//        return ResponseObject.<SyllabusActionLogResponse>builder()
//                .data(logService.updateNote(id, note))
//                .message("Log updated")
//                .build();
//    }
//
//    @DeleteMapping("/{id}")
//    @Operation(summary = "Xóa log (Hạn chế dùng)")
//    public ResponseObject<Void> delete(@PathVariable UUID id) {
//        logService.deleteLog(id);
//        return ResponseObject.<Void>builder()
//                .message("Log deleted")
//                .build();
//    }
}
