package com.example.smd.services;

import com.example.smd.dto.request.NotificationRequest;
import com.example.smd.dto.response.NotificationResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Notification;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.NotificationMapper;
import com.example.smd.realtime.RealtimePayload;
import com.example.smd.realtime.RealtimePublisher;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.NotificationRepository;
import com.example.smd.repositories.ReviewTaskRepository;
import com.example.smd.repositories.TaskRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {

    NotificationRepository notificationRepository;
    AccountRepository accountRepository;
    NotificationMapper notificationMapper;
    TaskRepository taskRepository;
    ReviewTaskRepository reviewTaskRepository;
    RealtimePublisher realtimePublisher;

    /**
     * Tạo và gửi thông báo cho một user cụ thể
     * Ngoài lưu vào DB, còn publish realtime message qua WebSocket
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        // Kiểm tra account có tồn tại không
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (request.getTaskId() != null && !taskRepository.existsById(request.getTaskId())) {
            throw new AppException(ErrorCode.TASK_NOT_FOUND);
        }

        if (request.getReviewId() != null && !reviewTaskRepository.existsById(request.getReviewId())) {
            throw new AppException(ErrorCode.REVIEW_TASK_NOT_FOUND);
        }

        // Tạo notification
        Notification notification = notificationMapper.toNotification(request);
        notification.setAccount(account);

        // Lưu notification
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created notification {} for user {}", savedNotification.getNotificationId(), account.getEmail());

        // Publish realtime notification qua WebSocket
        try {
            RealtimePayload payload = RealtimePayload.notification(
                    savedNotification.getTitle(),
                    notificationMapper.toNotificationResponse(savedNotification)
            );
            realtimePublisher.publishToAccount(account.getAccountId().toString(), payload);
            log.debug("Published notification {} to WebSocket", savedNotification.getNotificationId());
        } catch (Exception e) {
            log.error("Error publishing notification to WebSocket: {}", e.getMessage(), e);
            // Không throw exception, notification vẫn được lưu
        }

        return notificationMapper.toNotificationResponse(savedNotification);
    }

    /**
     * Lấy tất cả thông báo của user hiện tại (đã đăng nhập)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(int page, int size, Boolean isRead) {
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notifications;
        if (isRead != null) {
            // Lọc theo trạng thái đã đọc/chưa đọc
            notifications = notificationRepository.findByAccountAccountIdAndIsReadOrderByCreatedAtDesc(
                    currentUserId, isRead, pageable);
        } else {
            // Lấy tất cả
            notifications = notificationRepository.findByAccountAccountIdOrderByCreatedAtDesc(
                    currentUserId, pageable);
        }

        return notifications.map(notificationMapper::toNotificationResponse);
    }

    /**
     * Lấy tất cả thông báo của user
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByAccountId(String id, int page,
                                                        int size, Boolean isRead) {
        UUID currentUserId = accountRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND))
                .getAccountId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> notifications;
        if (isRead != null) {
            // Lọc theo trạng thái đã đọc/chưa đọc
            notifications = notificationRepository.findByAccountAccountIdAndIsReadOrderByCreatedAtDesc(
                    currentUserId, isRead, pageable);
        } else {
            // Lấy tất cả
            notifications = notificationRepository.findByAccountAccountIdOrderByCreatedAtDesc(
                    currentUserId, pageable);
        }

        return notifications.map(notificationMapper::toNotificationResponse);
    }

    /**
     * Lấy chi tiết một thông báo
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationDetail(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // Kiểm tra xem notification có thuộc về user hiện tại không
        UUID currentUserId = getCurrentUserId();
        if (!notification.getAccount().getAccountId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return notificationMapper.toNotificationResponse(notification);
    }

    /**
     * Đánh dấu một thông báo là đã đọc
     * Publish event realtime
     */
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // Kiểm tra quyền truy cập
        UUID currentUserId = getCurrentUserId();
        if (!notification.getAccount().getAccountId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        notification.setIsRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        // Publish event qua WebSocket
        try {
            RealtimePayload payload = RealtimePayload.event(
                    "NOTIFICATION_READ",
                    "Notification marked as read",
                    notificationMapper.toNotificationResponse(updatedNotification)
            );
            realtimePublisher.publishToAccount(currentUserId.toString(), payload);
        } catch (Exception e) {
            log.error("Error publishing read event to WebSocket: {}", e.getMessage(), e);
        }

        return notificationMapper.toNotificationResponse(updatedNotification);
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     * Publish event realtime
     */
    @Transactional
    public void markAllAsRead() {
        UUID currentUserId = getCurrentUserId();

        // Lấy tất cả thông báo chưa đọc
        Page<Notification> unreadNotifications = notificationRepository
                .findByAccountAccountIdAndIsReadOrderByCreatedAtDesc(
                        currentUserId, false, PageRequest.of(0, Integer.MAX_VALUE));

        // Đánh dấu tất cả là đã đọc
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);

        log.info("Marked all notifications as read for user {}", currentUserId);

        // Publish event qua WebSocket
        try {
            RealtimePayload payload = RealtimePayload.event(
                    "ALL_NOTIFICATIONS_READ",
                    "All notifications marked as read",
                    java.util.Map.of("count", unreadNotifications.getContent().size())
            );
            realtimePublisher.publishToAccount(currentUserId.toString(), payload);
        } catch (Exception e) {
            log.error("Error publishing all-read event to WebSocket: {}", e.getMessage(), e);
        }
    }

    /**
     * Đếm số lượng thông báo chưa đọc của user hiện tại
     */
    @Transactional(readOnly = true)
    public long countUnreadNotifications() {
        UUID currentUserId = getCurrentUserId();
        return notificationRepository.countByAccountAccountIdAndIsRead(currentUserId, false);
    }

    /**
     * Tìm kiếm thông báo của user hiện tại
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchMyNotifications(String search, int page, int size) {
        UUID currentUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<Notification> notifications = notificationRepository.searchNotifications(
                currentUserId, search, pageable);

        return notifications.map(notificationMapper::toNotificationResponse);
    }

    /**
     * Lấy user ID từ SecurityContext
     */
    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        return account.getAccountId();
    }
}
