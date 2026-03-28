package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    Sprint sprint;

    // Người được giao việc
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = false)
    Account account;

    // Task này liên quan đến Syllabus nào? (Optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id")
    Syllabus syllabus;

    @Column(name = "task_name", nullable = false, length = 150)
    String taskName;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(length = 20)
    String status; // To Do, In Progress, Done

    @Column(name = "priority", length = 20)
    String priority;

    @Column(name = "due_date")
    LocalDate deadline;

    @Column(name = "completed_at")
    LocalDate completedAt;

    @Column(name = "created_at")
    LocalDate createdAt;

    @Column(name = "task_type", length = 50)
    String type;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    List<ReviewTask> reviewTaskList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    Subject subject;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }


}
