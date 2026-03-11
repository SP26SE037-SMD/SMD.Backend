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
@Table(name = "combo")
public class Combo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID comboId;

    @Column(name = "combo_code", nullable = false, length = 20)
    String comboCode;

    @Column(name = "combo_name", length = 100)
    String comboName;

    @Column(name = "description")
    String description;

    @Column(name = "combo_type",length = 20)
    String type; // Elective / Mandatory

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @OneToMany(mappedBy = "combo", fetch = FetchType.LAZY)
    private List<Curriculum_Combo_Subject> curriculumComboSubjects;
}
