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
@Table(name = "subjects")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subject_id")
    UUID subjectId;

    @Column(name = "subject_code", unique = true, nullable = false, length = 20)
    String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 100)
    String subjectName;

    @Column(nullable = false)
    Integer credits;

    @Column(length = 20)
    String semester;

    @Column(columnDefinition = "TEXT")
    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    Department department;

    @Column(name = "student_limit")
    Integer studentLimit;

    @Column(name = "is_approved")
    Boolean isApproved = false;

    @Column(name = "status")
    Boolean status = true; // Active/Inactive

    @Column(name = "created_at")
    Instant createdAt;

    // Relationships
    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<CLOs> clos;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<Syllabus> syllabuses;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<Subject_Prerequisite> prerequisites;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
