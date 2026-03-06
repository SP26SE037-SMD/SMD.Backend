package com.example.smd.repositories;

import com.example.smd.entities.Subject_Prerequisite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrerequisiteRepository extends JpaRepository<Subject_Prerequisite, String> {
    // Tìm các môn tiên quyết của môn X
    @EntityGraph(attributePaths = {"subject", "prerequisiteSubject"})
    List<Subject_Prerequisite> findBySubject_SubjectId(UUID subjectId);

    // Tìm các môn mà môn X là tiên quyết của chúng (Môn phụ thuộc)
    @EntityGraph(attributePaths = {"subject", "prerequisiteSubject"})
    List<Subject_Prerequisite> findByPrerequisiteSubject_SubjectId(UUID prerequisiteId);

    boolean existsBySubject_SubjectIdAndPrerequisiteSubject_SubjectId(UUID sId, UUID pId);
}
