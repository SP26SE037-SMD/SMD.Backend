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
@Table(name = "assessments")
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID assessmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Assessment_Category assessmentCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    Assessment_Type assessmentType;

    @Column(name = "part")
    Integer part;

    @Column(name = "weight")
    Double weight; // Decimal

    @Column(name = "completion_criteria", columnDefinition = "TEXT")
    String completionCriteria;

    @Column(name = "duration")
    Integer duration;

    @Column(name = "question_type", length = 150)
    String questionType;

    @Column(name = "knowledge_skill", length = 150)
    String knowledgeSkill;

    @Column(name = "grading_guide", columnDefinition = "TEXT")
    String gradingGuide;

    @Column(columnDefinition = "TEXT")
    String note;

    //bổ sung cột
    @Column(name = "status")
    String status;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;
}
