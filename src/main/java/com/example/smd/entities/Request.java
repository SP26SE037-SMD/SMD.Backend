package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "request")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id")
    UUID requestId;

    @Column(name = "title", nullable = false, length = 50)
    String title;

    @Column(name = "content", columnDefinition = "TEXT")
    String content;

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment;

    @Column(name = "status", length = 50)
    String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    Account createdBy;

    @Column(name = "request_type", length = 50)
    String type; //SYLLABUS | CURRICULUM | MAJOR|SUBJECT|TASK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    Account receivedBy;

    @Column(name = "target_id")
    UUID targetId; // ID của Syllabus, Curriculum, Major, Subject, Task tương ứng

    @Column(name = "created_at")
    Instant createdAt;

    @Column(name = "updated_at")
    Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

}
