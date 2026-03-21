package com.example.smd.repositories;

import com.example.smd.entities.Department;
import com.example.smd.entities.Vector_Embeddings;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface EmbeddingRepository extends JpaRepository<Vector_Embeddings, UUID> {

    @Modifying
    @Query(value = "INSERT INTO vector_embeddings (embedding_id, block_id, content, created_at, embedding_vector) " +
            "VALUES (:id, :blockId, :content, :createdAt, CAST(:vector AS vector))",
            nativeQuery = true)
    void insertVector(
            @Param("id") UUID id,
            @Param("blockId") UUID blockId,
            @Param("content") String content,
            @Param("createdAt") Instant createdAt,
            @Param("vector") String vectorStr // Truyền chuỗi dạng "[0.1, 0.2]"
    );
}
