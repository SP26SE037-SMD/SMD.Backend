package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import org.mapstruct.TargetType;

import java.time.Instant;
import java.util.*;

@Table(name = "feedback_submission")
public class FeedbackSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // One submission has many answers
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL)
    private List<FeedbackAnswer> answers;
}
