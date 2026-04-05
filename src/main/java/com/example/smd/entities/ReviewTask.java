package com.example.smd.entities;

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
@Table(name = "review_task")
public class ReviewTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "review_id")
    private UUID reviewId;

    @Column(name = "tiltle_task", length = 50)
    private String titleTask;

    @Column(name = "comment_material", columnDefinition = "text")
    private String commentMaterial;

    @Column(name = "comment_session", columnDefinition = "text")
    private String commentSession;

    @Column(name = "comment_assessment", columnDefinition = "text")
    private String commentAssessment;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "is_accepted")
    private Boolean isAccepted;

    @Column(name = "review_date")
    Instant reviewDate;

    @Column(name = "due_to")
    Instant dueDate;

    @Column(name = "status", length = 50)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private Account reviewer;
}

