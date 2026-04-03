package com.example.smd.repositories;

import com.example.smd.entities.Material;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    // Lấy danh sách Material theo Syllabus
    @EntityGraph(attributePaths = {"syllabus"})
    List<Material> findAllBySyllabus_SyllabusId(UUID syllabusId);

    @EntityGraph(attributePaths = {"syllabus"})
    List<Material> findBySyllabus_SyllabusId(UUID syllabusId);

    @Modifying
    @Query(value = """
    UPDATE material m 
    SET status = :status 
    WHERE m.material_id IN (
        SELECT DISTINCT ON (m2.id) m2.material_id 
        FROM material m2 
        WHERE m2.syllabus_id = :syllabusId 
        ORDER BY m2.id, m2.version DESC
    )
    """, nativeQuery = true)
    int updateStatusBySyllabusId(@Param("status") String status, @Param("syllabusId") UUID syllabusId);

    @Query(value = """
        SELECT DISTINCT ON (m.id) m.* FROM material m 
        WHERE m.syllabus_id = :syllabusId 
          AND m.status = :status 
        ORDER BY m.id, m.version DESC
        """, nativeQuery = true)
    List<Material> findLatestMaterialsBySyllabus(
            @Param("syllabusId") UUID syllabusId,
            @Param("status") String status
    );

    @Query(value = """
        SELECT DISTINCT ON (m.id) m.* FROM material m 
        WHERE m.syllabus_id = :syllabusId 
        ORDER BY m.id, m.version DESC
        """, nativeQuery = true)
    List<Material> findLatestMaterialsBySyllabusId(@Param("syllabusId") UUID syllabusId);
}