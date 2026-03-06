package com.example.smd.services;

import com.example.smd.dto.request.NotificationRequest;
import com.example.smd.dto.response.NotificationResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Notification;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.NotificationMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.NotificationRepository;
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

    /**
     * Tạo và gửi thông báo cho một user cụ thể
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        // Kiểm tra account có tồn tại không
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Tạo notification
        Notification notification = notificationMapper.toNotification(request);
        notification.setAccount(account);

        // Lưu notification
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created notification {} for user {}", savedNotification.getNotificationId(), account.getUsername());

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

        return notificationMapper.toNotificationResponse(updatedNotification);
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
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
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        return account.getAccountId();
    }
}
