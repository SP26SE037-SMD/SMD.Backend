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
@Table(name = "elective")
public class Elective {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "elective_id")
    UUID electiveId;

    @Column(name = "elective_name", nullable = false, length = 100)
    String electiveName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "min_credits_required")
    Integer minCreditsRequired;

    @Column(name = "created_at")
    Instant createdAt;

    @OneToMany(mappedBy = "elective", fetch = FetchType.LAZY)
    List<Elective_Subject> electiveSubjects;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
