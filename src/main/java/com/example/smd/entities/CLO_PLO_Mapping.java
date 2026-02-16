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
@Table(name = "clo_plo_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"clo_id", "plo_id"}))
public class CLO_PLO_Mapping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clo_id", nullable = false)
    CLOs clo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plo_id", nullable = false)
    PLOs plo;

    @Column(name = "contribution_level", length = 20)
    String contributionLevel; // 'Low', 'Medium', 'High'

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
