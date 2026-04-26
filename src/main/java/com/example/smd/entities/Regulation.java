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
@Table(name = "regulation")
public class Regulation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    UUID regulationId;

    @Column(name = "code", unique = true, length = 50)
    String code;

    @Column(name = "name_regulation", length = 100)
    String name;

    @Column(name = "value", columnDefinition = "TEXT")
    String value; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    Major major;

    @Column(name = "created_at")
    Instant createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
