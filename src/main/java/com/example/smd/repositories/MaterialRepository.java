package com.example.smd.repositories;

import com.example.smd.entities.Material;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    // Lấy danh sách Material theo Syllabus
    @EntityGraph(attributePaths = {"syllabus"})
    List<Material> findAllBySyllabus_SyllabusId(UUID syllabusId);

    @EntityGraph(attributePaths = {"syllabus"})
    List<Material> findBySyllabus_SyllabusId(UUID syllabusId);



    @Query(value = """
        SELECT DISTINCT ON (m.id) m.* FROM material m
        WHERE m.syllabus_id = :syllabusId
        ORDER BY m.id, m.version DESC
        """, nativeQuery = true)
    List<Material> findLatestMaterialsBySyllabusId(@Param("syllabusId") UUID syllabusId);

    @Query(value = """
        SELECT m.* FROM material m
        WHERE m.syllabus_id = :syllabusId
        AND m.id = :id
        ORDER BY m.version DESC
        """, nativeQuery = true)
    List<Material> findMaterialByIdAndSyllabusId(@Param("id") int id, @Param("syllabusId")UUID syllabusId);

    @Query(value = """
        SELECT m.* FROM material m
        WHERE m.syllabus_id = :syllabusId
        AND m.id = :id
        ORDER BY m.version DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Material> findLatestMaterialByIdAndSyllabusId(@Param("id") int id, @Param("syllabusId")UUID syllabusId);
}
