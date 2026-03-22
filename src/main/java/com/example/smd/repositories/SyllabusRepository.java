package com.example.smd.repositories;

import com.example.smd.entities.Syllabus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, UUID> {
    @EntityGraph(attributePaths = {"subject"})
    List<Syllabus> findBySubject_SubjectId(UUID subjectId);

    List<Syllabus> findBySubject_SubjectIdAndStatus(UUID subjectId, String status);
}
