package com.example.smd.repositories;

import com.example.smd.entities.Syllabus_Action_Logs;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyllabusActionLogRepository extends JpaRepository<Syllabus_Action_Logs, UUID> {
    @EntityGraph(attributePaths = {"actionBy", "syllabus"}) // Tải trước Account và Syllabus
    List<Syllabus_Action_Logs> findBySyllabus_SyllabusIdOrderByCreatedAtDesc(UUID syllabusId);

    @EntityGraph(attributePaths = {"actionBy", "syllabus"})
    Optional<Syllabus_Action_Logs> findById(UUID id);
}