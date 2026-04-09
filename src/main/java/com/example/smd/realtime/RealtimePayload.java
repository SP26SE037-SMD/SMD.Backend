package com.example.smd.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Payload chuẩn cho tất cả realtime messages
 * Định dạng chung giúp client dễ xử lý và mở rộng
 *
 * Cấu trúc:
 * {
 *   "code": "NOTIFICATION_CREATED",
 *   "message": "Thông báo mới",
 *   "timestamp": "2026-04-08T10:20:30Z",
 *   "data": { ... payload cụ thể ... }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RealtimePayload {

    /**
     * Mã sự kiện (enum-like string)
     * Ví dụ: NOTIFICATION_CREATED, SYLLABUS_SUBMITTED, TASK_ASSIGNED
     */
    private String code;

    /**
     * Thông điệp mô tả
     */
    private String message;

    /**
     * Thời gian phát hành message
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Dữ liệu payload, loại generic có thể là bất kỳ object nào
     */
    private Object data;

    /**
     * Meta thông tin (optional)
     */
    private Object meta;

    // ===== Static factory methods để tạo payload dễ dàng =====

    /**
     * Tạo payload cho notification
     */
    public static RealtimePayload notification(String message, Object data) {
        return RealtimePayload.builder()
                .code("NOTIFICATION")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Tạo payload cho event
     */
    public static RealtimePayload event(String code, String message, Object data) {
        return RealtimePayload.builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Tạo payload cho status update
     */
    public static RealtimePayload status(String status, Object data) {
        return RealtimePayload.builder()
                .code("STATUS_UPDATE")
                .message("Status updated: " + status)
                .data(data)
                .build();
    }

    /**
     * Tạo payload lỗi
     */
    public static RealtimePayload error(String code, String message) {
        return RealtimePayload.builder()
                .code(code)
                .message(message)
                .build();
    }
}
