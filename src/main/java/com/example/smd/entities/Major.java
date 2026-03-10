package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name = "major")
public class Major {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID majorId;

    @Column(name = "major_code", unique = true, nullable = false, length = 20)
    String majorCode;

    @Column(name = "major_name", nullable = false, length = 100)
    String majorName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "status")
    Boolean status = true;

    @Column(name = "created_at")
    Instant createdAt;

    @OneToMany(mappedBy = "major", fetch = FetchType.LAZY)
    List<PLOs> plos;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
