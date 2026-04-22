package com.example.smd.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration cho SMD System
 * - Hỗ trợ STOMP protocol
 * - SockJS fallback cho các trình duyệt không hỗ trợ WebSocket
 * - Simple in-memory message broker cho topics
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint cho Web client (SockJS fallback)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:3001",
                        "http://localhost:8081",
                        "http://localhost:8082",
                        "http://43.207.156.116",
                        "https://smd-syllabus-ebon.vercel.app",
                        "https://smdview.vercel.app"
                )
                .withSockJS();

        // Endpoint cho native clients (mobile, desktop)
        registry.addEndpoint("/ws-native")
                .setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cấu hình simple in-memory message broker
        // Các message gửi tới /topic/* sẽ được broker xử lý
        registry.enableSimpleBroker("/topic");

        // Cấu hình application destination prefix
        // Controller sẽ xử lý các message gửi tới /app/*
        registry.setApplicationDestinationPrefixes("/app");

        // Cấu hình user destination prefix
        // Dùng cho private messages: /user/{username}/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }
}
