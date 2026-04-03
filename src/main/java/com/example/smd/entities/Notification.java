package com.example.smd.entities;

import com.example.smd.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    UUID notificationId;

    @Column(nullable = false)
    String title;

    @Column(name = "message", columnDefinition = "TEXT")
    String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20)
    NotificationType type;

    @Column(name = "is_read")
    Boolean isRead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    Account account;

    @Column(name = "task_id")
    UUID taskId;

    @Column(name = "review_id")
    UUID reviewId;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (isRead == null) {
            isRead = false;
        }
    }
}
