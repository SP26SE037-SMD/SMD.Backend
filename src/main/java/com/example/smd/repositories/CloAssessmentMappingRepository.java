package com.example.smd.repositories;

import com.example.smd.entities.CLO_Assessment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CloAssessmentMappingRepository extends JpaRepository<CLO_Assessment, UUID> {

    @EntityGraph(attributePaths = {"clo", "assessment", "assessment.syllabus"})
    List<CLO_Assessment> findByAssessment_Syllabus_SyllabusId(UUID syllabusId);

    @EntityGraph(attributePaths = {"clo", "assessment", "assessment.syllabus"})
    List<CLO_Assessment> findByClo_CloId(UUID cloId);

    @EntityGraph(attributePaths = {"clo", "assessment", "assessment.syllabus"})
    List<CLO_Assessment> findByAssessment_AssessmentId(UUID assessmentId);

    boolean existsByClo_CloIdAndAssessment_AssessmentId(UUID cloId, UUID assessmentId);
}
