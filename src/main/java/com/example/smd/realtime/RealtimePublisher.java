package com.example.smd.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service để publish realtime messages tới WebSocket clients
 * Wrapper bên trên SimpMessagingTemplate để đơn giản hóa API
 *
 * Sử dụng:
 * - realtimePublisher.publishNotification(accountId, payload)
 * - realtimePublisher.publish(destination, payload)
 * - realtimePublisher.sendToUser(username, payload)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // ===== Public/Broadcast messages =====

    /**
     * Publish notification tới một account cụ thể
     * Topic: /topic/notification/account/{accountId}
     */
    public void publishToAccount(String accountId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forAccount(accountId);
        publish(destination, payload);
    }

    /**
     * Publish notification tới một department (tất cả members)
     * Topic: /topic/notification/department/{departmentId}
     */
    public void publishToDepartment(String departmentId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forDepartment(departmentId);
        publish(destination, payload);
    }

    /**
     * Publish notification cho một syllabus
     * Topic: /topic/notification/syllabus/{syllabusId}
     */
    public void publishToSyllabus(String syllabusId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forSyllabus(syllabusId);
        publish(destination, payload);
    }

    /**
     * Publish notification cho một task
     * Topic: /topic/notification/task/{taskId}
     */
    public void publishToTask(String taskId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forTask(taskId);
        publish(destination, payload);
    }

    /**
     * Publish notification cho một review
     * Topic: /topic/notification/review/{reviewId}
     */
    public void publishToReview(String reviewId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.forReview(reviewId);
        publish(destination, payload);
    }

    /**
     * Broadcast notification tới tất cả users của một khoa
     * Topic: /topic/notification/broadcast/department/{departmentId}
     */
    public void broadcastToDepartment(String departmentId, RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.broadcastDepartment(departmentId);
        publish(destination, payload);
    }

    /**
     * Broadcast notification tới toàn bộ hệ thống
     * Topic: /topic/notification/broadcast/system
     */
    public void broadcastToSystem(RealtimePayload payload) {
        String destination = NotificationTopicRegistry.Notification.broadcastSystem();
        publish(destination, payload);
    }

    /**
     * Publish event cho một subject
     */
    public void publishSubjectEvent(String subjectId, String eventCode, String message, Object data) {
        String destination = NotificationTopicRegistry.Event.subjectEvent(subjectId);
        RealtimePayload payload = RealtimePayload.event(eventCode, message, data);
        publish(destination, payload);
    }

    /**
     * Publish event cho một syllabus
     */
    public void publishSyllabusEvent(String syllabusId, String eventCode, String message, Object data) {
        String destination = NotificationTopicRegistry.Event.syllabusEvent(syllabusId);
        RealtimePayload payload = RealtimePayload.event(eventCode, message, data);
        publish(destination, payload);
    }

    /**
     * Publish event cho một sprint
     */
    public void publishSprintEvent(String sprintId, String eventCode, String message, Object data) {
        String destination = NotificationTopicRegistry.Event.sprintEvent(sprintId);
        RealtimePayload payload = RealtimePayload.event(eventCode, message, data);
        publish(destination, payload);
    }

    /**
     * Publish event cho một task
     */
    public void publishTaskEvent(String taskId, String eventCode, String message, Object data) {
        String destination = NotificationTopicRegistry.Event.taskEvent(taskId);
        RealtimePayload payload = RealtimePayload.event(eventCode, message, data);
        publish(destination, payload);
    }

    /**
     * Publish event cho một review
     */
    public void publishReviewEvent(String reviewId, String eventCode, String message, Object data) {
        String destination = NotificationTopicRegistry.Event.reviewEvent(reviewId);
        RealtimePayload payload = RealtimePayload.event(eventCode, message, data);
        publish(destination, payload);
    }

    // ===== Generic methods =====

    /**
     * Publish tới một destination bất kỳ
     * @param destination STOMP destination (ví dụ: /topic/notification/account/123)
     * @param payload Message payload
     */
    public void publish(String destination, RealtimePayload payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Published to {}: {}", destination, payload.getCode());
        } catch (Exception e) {
            log.error("Error publishing to {}: {}", destination, e.getMessage(), e);
        }
    }

    /**
     * Gửi private message tới một user cụ thể
     * Topic: /user/{username}/queue/notifications
     *
     * @param username Username của user nhận message
     * @param destination Queue destination (ví dụ: /queue/notifications)
     * @param payload Message payload
     */
    public void sendToUser(String username, String destination, RealtimePayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
            log.debug("Sent to user {}{}: {}", username, destination, payload.getCode());
        } catch (Exception e) {
            log.error("Error sending to user {}: {}", username, e.getMessage(), e);
        }
    }

    /**
     * Gửi private notification tới user
     * @param username Username
     * @param payload Payload
     */
    public void sendToUserNotification(String username, RealtimePayload payload) {
        sendToUser(username, "/queue/notifications", payload);
    }

    /**
     * Publish raw message (không wrap trong RealtimePayload)
     * Dùng khi cần flexibility
     */
    public void publishRaw(String destination, Object message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Published raw to {}", destination);
        } catch (Exception e) {
            log.error("Error publishing raw to {}: {}", destination, e.getMessage(), e);
        }
    }
}
