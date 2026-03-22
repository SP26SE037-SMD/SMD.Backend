package com.example.smd.repositories;

import com.example.smd.dto.response.SimilarityResult;
import com.example.smd.entities.Department;
import com.example.smd.entities.Vector_Embeddings;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    @Query(value = """
SELECT 
    b.block_style AS contentTitle, 
    v.content AS contentBody,
    m.material_name AS chapterTitle,
    (v.embedding_vector <=> CAST(:vectorStr AS vector)) AS distance,
    m.material_type AS type
FROM vector_embeddings v
JOIN blocks b ON v.block_id = b.block_id
JOIN material m ON b.material_id = m.material_id
JOIN syllabus s ON m.syllabus_id = s.syllabus_id -- Bắc cầu qua Syllabus
WHERE s.subject_id = CAST(:subjectId AS uuid)    -- Lọc theo Subject ID
ORDER BY v.embedding_vector <=> CAST(:vectorStr AS vector) ASC
LIMIT 3
""", nativeQuery = true)
    List<SimilarityResult> findTopSimilarContent(
            @Param("subjectId") UUID subjectId,
            @Param("vectorStr") String vectorStr
    );
}
