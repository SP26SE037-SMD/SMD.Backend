package com.example.smd.repositories;

import com.example.smd.entities.Material;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialRepository extends JpaRepository<Material, UUID> {
    // Lấy danh sách Material theo Syllabus
    @EntityGraph(attributePaths = {"syllabus"})
    List<Material> findAllBySyllabus_SyllabusId(UUID syllabusId);
}