package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "clo_session",
       uniqueConstraints = @UniqueConstraint(columnNames = {"clo_id", "session_id"}))
public class CLO_Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clo_id", nullable = false)
    CLOs clo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    Session session;

    @Column(name = "coverage_level", length = 20)
    String coverageLevel; // 'Introduced', 'Reinforced', 'Mastered'

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
