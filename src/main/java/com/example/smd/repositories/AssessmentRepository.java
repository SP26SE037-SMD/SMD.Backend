package com.example.smd.repositories;

import com.example.smd.entities.Assessment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID>, JpaSpecificationExecutor<Assessment> {

    List<Assessment> findBySyllabus_SyllabusIdOrderByPartAsc(UUID syllabusId);

    @EntityGraph(attributePaths = {"assessmentCategory", "assessmentType"})
    List<Assessment> findBySyllabus_SyllabusId(UUID syllabusId);

    @Query("SELECT COALESCE(SUM(a.weight), 0) FROM Assessment a WHERE a.syllabus.syllabusId = :syllabusId AND UPPER(COALESCE(a.status, '')) <> 'ARCHIVED'")
    Double sumWeightBySyllabusId(@Param("syllabusId") UUID syllabusId);

    @Query("SELECT COALESCE(SUM(a.weight), 0) FROM Assessment a WHERE a.syllabus.syllabusId = :syllabusId AND a.assessmentId <> :assessmentId AND UPPER(COALESCE(a.status, '')) <> 'ARCHIVED'")
    Double sumWeightBySyllabusIdAndAssessmentIdNot(@Param("syllabusId") UUID syllabusId,
                                                   @Param(
                                                           "assessmentId") UUID assessmentId);

    @Modifying
    @Query("UPDATE Assessment a SET a.status = :status WHERE a.syllabus.syllabusId = :syllabusId")
    int updateStatusBySyllabusId(@Param("status") String status, @Param("syllabusId") UUID syllabusId);
}
