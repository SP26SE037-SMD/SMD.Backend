package com.example.smd.controller;

import com.example.smd.dto.request.SystemLogRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SystemLogResponse;
import com.example.smd.services.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/system-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "System Log", description = "Endpoints for tracking user activities in the system")
@SecurityRequirement(name = "bearerAuth")
public class SystemLogController {

    SystemLogService systemLogService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_LOG_CREATE')")
    @Operation(summary = "Create a system log entry")
    public ResponseObject<SystemLogResponse> createLog(
            @RequestBody @Valid SystemLogRequest request) {
        return ResponseObject.<SystemLogResponse>builder()
                .status(1000)
                .data(systemLogService.createLog(request))
                .message("Log created successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all system logs with pagination")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW_ALL')")
    public ResponseObject<PagedResponse<SystemLogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.getAllLogs(page, size)))
                .message("Get all logs successfully")
                .build();
    }

    @GetMapping("/my-logs")
    @Operation(summary = "Get logs of current user")
    public ResponseObject<PagedResponse<SystemLogResponse>> getMyLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.getMyLogs(page, size)))
                .message("Get my logs successfully")
                .build();
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get logs by account ID")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW_ALL')")
    public ResponseObject<PagedResponse<SystemLogResponse>> getLogsByAccount(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.getLogsByAccount(accountId, page, size)))
                .message("Get logs by account successfully")
                .build();
    }

    @GetMapping("/target/{targetId}")
    @Operation(summary = "Get logs by target ID (all activities related to an object)")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW_ALL')")
    public ResponseObject<PagedResponse<SystemLogResponse>> getLogsByTargetId(
            @PathVariable UUID targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.getLogsByTargetId(targetId, page, size)))
                .message("Get logs by target successfully")
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search logs by action")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW_ALL')")
    public ResponseObject<PagedResponse<SystemLogResponse>> searchLogs(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.searchLogs(search, page, size)))
                .message("Search logs successfully")
                .build();
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get logs within a date range")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW_ALL')")
    public ResponseObject<PagedResponse<SystemLogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.getLogsByDateRange(startDate, endDate, page, size)))
                .message("Get logs by date range successfully")
                .build();
    }

    @GetMapping("/account/{accountId}/date-range")
    @Operation(summary = "Get logs of a user within a date range")
    @PreAuthorize("hasAuthority('SYSTEM_LOG_VIEW_ALL')")
    public ResponseObject<PagedResponse<SystemLogResponse>> getLogsByAccountAndDateRange(
            @PathVariable UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemLogResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemLogService.getLogsByAccountAndDateRange(
                        accountId, startDate, endDate, page, size)))
                .message("Get logs by account and date range successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get log detail by ID")
    public ResponseObject<SystemLogResponse> getLogDetail(
            @PathVariable("id") UUID logId) {
        return ResponseObject.<SystemLogResponse>builder()
                .status(1000)
                .data(systemLogService.getLogDetail(logId))
                .message("Get log detail successfully")
                .build();
    }
}
