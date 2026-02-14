package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    String subjectId;

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

    @Column(name = "student_limit")
    Integer studentLimit;

    @Column(name = "is_approved")
    Boolean isApproved;

    @Column(name = "status")
    Boolean status; // Active/Inactive

    // Relationships
//    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
//    List<Combo_subject> comboSubjects;
//
//    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
//    List<Syllabus> syllabus;
}
