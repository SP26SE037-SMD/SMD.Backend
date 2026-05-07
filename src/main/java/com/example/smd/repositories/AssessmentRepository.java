package com.example.smd.repositories;

import com.example.smd.entities.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID>, JpaSpecificationExecutor<Assessment> {

    @EntityGraph(attributePaths = {"assessmentCategory", "assessmentType", "syllabus"})
    List<Assessment> findBySyllabus_SyllabusIdOrderByPartAsc(UUID syllabusId);

    @EntityGraph(attributePaths = {"assessmentCategory", "assessmentType", "syllabus"})
    List<Assessment> findBySyllabus_SyllabusId(UUID syllabusId);

    @Override
    @EntityGraph(attributePaths = {"assessmentCategory", "assessmentType", "syllabus"})
    Page<Assessment> findAll(@Nullable Specification<Assessment> spec, Pageable pageable);

    @Query("SELECT COALESCE(SUM(a.weight), 0) FROM Assessment a WHERE a.syllabus.syllabusId = :syllabusId")
    Double sumWeightBySyllabusId(@Param("syllabusId") UUID syllabusId);

    @Query("SELECT COALESCE(SUM(a.weight), 0) FROM Assessment a WHERE a.syllabus.syllabusId = :syllabusId AND a.assessmentId <> :assessmentId")
    Double sumWeightBySyllabusIdAndAssessmentIdNot(@Param("syllabusId") UUID syllabusId,
                                                   @Param("assessmentId") UUID assessmentId);
}
