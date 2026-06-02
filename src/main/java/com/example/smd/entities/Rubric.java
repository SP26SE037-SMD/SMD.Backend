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
@Table(name = "rubric")
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rubric_id")
    UUID rubricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    String code;

    @Column(name = "name", length = 200)
    String name;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @OneToMany(mappedBy = "rubric", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    List<RubricCriterion> criteria;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
