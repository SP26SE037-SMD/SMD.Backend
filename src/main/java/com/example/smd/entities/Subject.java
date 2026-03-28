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

    @Column(name = "degree_level", length = 20)
    String degreeLevel;

    @Column(name = "time_allocation", length = 50)
    String timeAllocation;

    @Column(columnDefinition = "TEXT")
    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    Department department;

    @Column(name = "student_limit")
    Integer studentLimit;

    @Column(name = "student_tasks", length = 100)
    String studentTasks;

    @Column(name = "scoring_scale")
    Integer scoringScale;

    @Column(name = "decision_no", length = 50)
    String decisionNo;

    @Column(name = "min_to_pass")
    Integer minToPass;

    @Column(name = "is_approved")
    Boolean isApproved = false;

    @Column(name = "status")
    String status;

    @Column(name = "approved_date")
    Instant approvedDate;

    @Column(name = "created_at")
    Instant createdAt;

    @Column(name = "tool")
    String tool;

    // Relationships
    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<CLOs> clos;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<Syllabus> syllabuses;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<Task> tasks;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    List<Subject_Prerequisite> prerequisites;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    private List<Curriculum_Group_Subject> curriculumGroupSubjects;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }


}
