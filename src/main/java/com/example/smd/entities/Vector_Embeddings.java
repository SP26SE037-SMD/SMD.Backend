package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

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
        String id;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "block_id", nullable = false)
        Blocks block;

        // Nếu dùng PostgreSQL + PGVector, bạn có thể dùng @Column(columnDefinition = "vector")
        // Nếu dùng DB thường, bạn có thể lưu dạng JSON hoặc Binary.
        // Dưới đây là cách dùng JSON để lưu mảng float (tương thích nhiều DB)
        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "embedding", columnDefinition = "jsonb")
        List<Double> embedding;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "meta_data", columnDefinition = "jsonb")
        String metaData; // Lưu metadata dạng JSON (source, page, etc.)
    }

