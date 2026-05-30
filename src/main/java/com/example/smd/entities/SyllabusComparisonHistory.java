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
@Table(name = "syllabus_comparison_history")
public class SyllabusComparisonHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id")
    UUID historyId;

    @Column(name = "old_syllabus_id", nullable = false)
    UUID oldSyllabusId;

    @Column(name = "new_syllabus_id", nullable = false)
    UUID newSyllabusId;

    @Column(name = "assessment_diff_json", columnDefinition = "TEXT")
    String assessmentDiffJson;

    @Column(name = "concept_diff_json", columnDefinition = "TEXT")
    String conceptDiffJson;

    @Column(name = "is_selected_compare")
    boolean isSelectedCompare;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
