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
@Table(name = "document")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "document_id")
    UUID documentId;

    @Column(name = "document_url", unique = true, nullable = false, length = 20)
    String documentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = true)
    Major major;

    @Column(name = "status")
    String status;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
