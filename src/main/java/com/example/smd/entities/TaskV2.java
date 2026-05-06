package com.example.smd.entities;

import jakarta.persistence.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "task_v2")

public class TaskV2 {
    @Id
    @Column(name = "task_v2_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    Sprint sprint;

    // Người được giao việc
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    Account createdBy;

    @Column(name = "task_name", nullable = false, length = 150)
    String taskName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "action", length = 100)
    String action;// CREATE, UPDATE, REVIEW

    @Column(length = 20)
    String status; // To Do, In Progress, Done

    @Column(name = "priority", length = 20)
    String priority;

    @Column(name = "comment", columnDefinition = "TEXT")
    String comment;

    @Column(name = "due_date")
    LocalDate dueDate;

    @Column(name = "completed_at")
    LocalDate completedAt;

    @Column(name = "created_at")
    LocalDate createdAt;

    @Column(name = "task_type", length = 50)
    String type; //SYLLABUS | CURRICULUM | MAJOR

    @Column(name = "target_id")
    UUID targetId; // ID của Syllabus, Curriculum, Document

    @Column(name = "root_task_id")
    UUID rootTaskId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }

}
