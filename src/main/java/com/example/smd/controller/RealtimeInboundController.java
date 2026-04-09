package com.example.smd.controller;

import com.example.smd.realtime.RealtimePayload;
import com.example.smd.realtime.RealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * STOMP Inbound Controller
 * Xử lý các incoming STOMP messages từ client
 * 
 * Endpoint: /app/* được định tuyến tới controller này
 * 
 * Client gửi tới: /app/notification/ping/{resourceId}
 * Server broadcast tới: /topic/notification/resourceId/{resourceId}
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RealtimeInboundController {

    private final RealtimePublisher realtimePublisher;

    /**
     * Ping endpoint để check connection
     * Client gửi: /app/notification/ping/{accountId}
     * Server trả về: /topic/notification/account/{accountId}
     */
    @MessageMapping("/notification/ping/{accountId}")
    @SendTo("/topic/notification/account/{accountId}")
    public RealtimePayload pingNotification(@DestinationVariable String accountId, String message) {
        log.info("Received ping from account: {}", accountId);
        return RealtimePayload.notification(
                "Pong - Connection alive",
                java.util.Map.of("ping_time", java.time.Instant.now()));
    }

    /**
     * Test endpoint - gửi notification test
     * Client gửi: /app/notification/test/{accountId}
     * Server trả về: /topic/notification/account/{accountId}
     */
    @MessageMapping("/notification/test/{accountId}")
    public void sendTestNotification(@DestinationVariable String accountId, String message) {
        log.info("Test notification request from account: {}", accountId);

        RealtimePayload payload = RealtimePayload.notification(
                "Test notification - " + message,
                java.util.Map.of("test_time", java.time.Instant.now()));

        realtimePublisher.publishToAccount(accountId, payload);
    }

    /**
     * Broadcast test message tới department
     * Client gửi: /app/event/broadcast-department/{departmentId}
     */
    @MessageMapping("/event/broadcast-department/{departmentId}")
    @PreAuthorize("hasAuthority('NOTIFICATION_CREATE')")
    public void broadcastToDepartment(@DestinationVariable String departmentId, String message) {
        log.info("Broadcast request to department: {}", departmentId);

        RealtimePayload payload = RealtimePayload.event(
                "BROADCAST_DEPARTMENT",
                "Thông báo cho khoa: " + message,
                java.util.Map.of("department_id", departmentId, "broadcast_time", java.time.Instant.now()));

        realtimePublisher.broadcastToDepartment(departmentId, payload);
    }

    /**
     * Broadcast system-wide message
     * Client gửi: /app/event/broadcast-system
     */
    @MessageMapping("/event/broadcast-system")
    @PreAuthorize("hasAuthority('NOTIFICATION_CREATE')")
    public void broadcastToSystem(String message) {
        log.info("System broadcast requested: {}", message);

        RealtimePayload payload = RealtimePayload.event(
                "BROADCAST_SYSTEM",
                "Thông báo hệ thống: " + message,
                java.util.Map.of("broadcast_time", java.time.Instant.now()));

        realtimePublisher.broadcastToSystem(payload);
    }

    /**
     * Subscribe acknowledge endpoint
     * Client gửi: /app/subscription/ack/{topicType}/{resourceId}
     * Phản hồi để log subscription
     */
    @MessageMapping("/subscription/ack/{topicType}/{resourceId}")
    public void subscriptionAck(@DestinationVariable String topicType,
            @DestinationVariable String resourceId,
            String data) {
        log.info("Subscription acknowledged: type={}, resourceId={}", topicType, resourceId);
    }
}
