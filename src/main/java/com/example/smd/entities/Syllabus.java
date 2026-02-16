package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "syllabus")
public class Syllabus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "syllabus_id")
    String syllabusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    Subject subject;

    @Column(name = "syllabus_name", nullable = false, length = 100)
    String syllabusName;

    @Column(name = "min_bloom_level")
    Integer minBloomLevel;

    @Column(name = "min_avg_grade")
    Double minAvgGrade; // Decimal(4,2)

    @Column(length = 20)
    String status; // 'Draft', 'Review', 'Approved', 'Active', 'Archived'

    @Column(name = "created_at")
    Instant createdAt;

    @Column(name = "approved_date")
    Instant approvedDate;

    @OneToMany(mappedBy = "syllabus", fetch = FetchType.LAZY)
    List<Session> sessions;

    @OneToMany(mappedBy = "syllabus", fetch = FetchType.LAZY)
    List<Assessment_Syllabus> assessmentsSyllabuses;

    @OneToMany(mappedBy = "syllabus", fetch = FetchType.LAZY)
    List<Syllabus_Source> syllabusSources;

    @OneToMany(mappedBy = "syllabus", fetch = FetchType.LAZY)
    List<Task> tasks;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
