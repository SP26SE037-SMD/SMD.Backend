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

    @Modifying
    @Query("UPDATE Material m SET m.status = :status WHERE m.syllabus.syllabusId = :syllabusId")
    int updateStatusBySyllabusId(@Param("status") String status, @Param("syllabusId") UUID syllabusId);

    List<Material> findAllBySyllabus_SyllabusIdAndStatus(UUID syllabusId, String status);
}