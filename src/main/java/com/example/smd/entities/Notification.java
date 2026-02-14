package com.example.smd.entities;

import com.example.smd.enums.NotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificationID")
    private int notificationID;
    @Column(name = "message")
    private String message;

    @Column(name = "createAt")
    private LocalDateTime createAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10)
    private NotificationType type; //start, end, special

    @Column(name = "is_read")
    Boolean isRead;

    @Column(nullable = false)
    String title;
}
