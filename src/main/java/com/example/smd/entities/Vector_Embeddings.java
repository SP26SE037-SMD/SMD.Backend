package com.example.smd.entities;

import com.pgvector.PGvector;
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
@Table(name = "vector_embeddings")
public class Vector_Embeddings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "embedding_id")
    UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    Blocks block;

    // Sử dụng class PGvector để Hibernate tự động map xuống kiểu vector(768)
    @Column(name = "embedding_vector", columnDefinition = "vector(3072)")
    String embedding;

    // BỎ @JdbcTypeCode(SqlTypes.JSON) đi nếu bạn chỉ lưu text bình thường
    @Column(name = "content", columnDefinition = "TEXT")
    String content;

    @Column(name = "created_at")
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}