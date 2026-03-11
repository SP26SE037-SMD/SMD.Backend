package com.example.smd.controller;

import com.example.smd.dto.request.NotificationRequest;
import com.example.smd.dto.response.NotificationResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notification", description = "Endpoints for managing in-app notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAuthority('NOTIFICATION_CREATE')")
    @Operation(summary = "Create and send a notification to a user")
    public ResponseObject<NotificationResponse> createNotification(
            @RequestBody @Valid NotificationRequest request) {
        return ResponseObject.<NotificationResponse>builder()
                .status(1000)
                .data(notificationService.createNotification(request))
                .message("Notification created successfully")
                .build();
    }

    @GetMapping("/my-notifications")
    @Operation(summary = "Get all notifications of current user with optional filter")
    public ResponseObject<PagedResponse<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead) {
        return ResponseObject.<PagedResponse<NotificationResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(notificationService.getMyNotifications(page, size, isRead)))
                .message("Get notifications successfully")
                .build();
    }

    @GetMapping("/account-notifications")
    @Operation(summary = "Get all notifications of  user with optional filter")
    public ResponseObject<PagedResponse<NotificationResponse>> getNotificationsByAccountId(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead) {
        return ResponseObject.<PagedResponse<NotificationResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(notificationService.getMyNotifications(page, size, isRead)))
                .message("Get notifications successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification detail by ID")
    public ResponseObject<NotificationResponse> getNotificationDetail(
            @PathVariable("id") UUID notificationId) {
        return ResponseObject.<NotificationResponse>builder()
                .status(1000)
                .data(notificationService.getNotificationDetail(notificationId))
                .message("Get notification detail successfully")
                .build();
    }

    @PutMapping("/{id}/mark-as-read")
    @PreAuthorize("hasAuthority('NOTIFICATION_UPDATE')")
    @Operation(summary = "Mark a notification as read")
    public ResponseObject<NotificationResponse> markAsRead(
            @PathVariable("id") UUID notificationId) {
        return ResponseObject.<NotificationResponse>builder()
                .status(1000)
                .data(notificationService.markAsRead(notificationId))
                .message("Notification marked as read")
                .build();
    }

    @PostMapping("/mark-all-as-read")
    @PreAuthorize("hasAuthority('NOTIFICATION_UPDATE')")
    @Operation(summary = "Mark all notifications as read")
    public ResponseObject<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("All notifications marked as read")
                .build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseObject<Long> getUnreadCount() {
        return ResponseObject.<Long>builder()
                .status(1000)
                .data(notificationService.countUnreadNotifications())
                .message("Get unread count successfully")
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search notifications by title or message")
    public ResponseObject<PagedResponse<NotificationResponse>> searchNotifications(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<NotificationResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(notificationService.searchMyNotifications(search, page, size)))
                .message("Search notifications successfully")
                .build();
    }
}
