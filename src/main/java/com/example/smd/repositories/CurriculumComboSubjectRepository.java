package com.example.smd.repositories;

import com.example.smd.entities.Curriculum_Combo_Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurriculumComboSubjectRepository extends JpaRepository<Curriculum_Combo_Subject, UUID> {

    // Kiểm tra xem đã tồn tại mapping chưa
    @Query("SELECT CASE WHEN COUNT(ccs) > 0 THEN true ELSE false END " +
           "FROM Curriculum_Combo_Subject ccs " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.subject.subjectId = :subjectId " +
           "AND (:comboId IS NULL AND ccs.combo IS NULL OR ccs.combo.comboId = :comboId)")
    boolean existsByCurriculumAndSubjectAndCombo(
        @Param("curriculumId") UUID curriculumId,
        @Param("subjectId") UUID subjectId,
        @Param("comboId") UUID comboId
    );

    // Tìm mapping cụ thể
    @Query("SELECT ccs FROM Curriculum_Combo_Subject ccs " +
           "LEFT JOIN FETCH ccs.curriculum " +
           "LEFT JOIN FETCH ccs.combo " +
           "LEFT JOIN FETCH ccs.subject " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.subject.subjectId = :subjectId " +
           "AND (:comboId IS NULL AND ccs.combo IS NULL OR ccs.combo.comboId = :comboId)")
    Optional<Curriculum_Combo_Subject> findByCurriculumAndSubjectAndCombo(
        @Param("curriculumId") UUID curriculumId,
        @Param("subjectId") UUID subjectId,
        @Param("comboId") UUID comboId
    );

        // Tìm Curriculum_Combo_Subject theo curriculum
    @Query("SELECT ccs FROM Curriculum_Combo_Subject ccs " +
           "JOIN FETCH ccs.subject s " +
            "WHERE ccs.curriculum.curriculumId = :curriculumId")
        Page<Curriculum_Combo_Subject> findByCurriculumId(
        @Param("curriculumId") UUID curriculumId,
        Pageable pageable
    );

        // Tìm Curriculum_Combo_Subject theo combo
    @Query("SELECT ccs FROM Curriculum_Combo_Subject ccs " +
           "JOIN FETCH ccs.subject s " +
            "WHERE ccs.combo.comboId = :comboId")
        Page<Curriculum_Combo_Subject> findByComboId(
        @Param("comboId") UUID comboId,
        Pageable pageable
    );
}
