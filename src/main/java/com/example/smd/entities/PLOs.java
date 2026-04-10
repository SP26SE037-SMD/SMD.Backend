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
@Table(name = "plos")
public class PLOs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID ploId;

    @Column(name = "plo_code", nullable = false, length = 20)
    String ploCode;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "status")
    String status;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id")
    Curriculum curriculum;

    // @OneToMany(mappedBy = "plo", fetch = FetchType.LAZY)
    // List<CLO_PLO_Mapping> cloPloMappings;
}
