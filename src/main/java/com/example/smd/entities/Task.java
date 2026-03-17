package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

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
    String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    Sprint sprint;

    // Người được giao việc
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
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

    @Column(name = "deadline")
    java.time.Instant deadline;
}
