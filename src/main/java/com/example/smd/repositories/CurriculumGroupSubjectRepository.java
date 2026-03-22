package com.example.smd.repositories;

import com.example.smd.entities.Curriculum_Group_Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurriculumGroupSubjectRepository extends JpaRepository<Curriculum_Group_Subject, UUID> {

    @Query("SELECT CASE WHEN COUNT(ccs) > 0 THEN true ELSE false END " +
           "FROM Curriculum_Group_Subject ccs " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.subject.subjectId = :subjectId")
    boolean existsByCurriculumAndSubject(
        @Param("curriculumId") UUID curriculumId,
        @Param("subjectId") UUID subjectId
    );

    // Kiểm tra xem đã tồn tại mapping chưa
    @Query("SELECT CASE WHEN COUNT(ccs) > 0 THEN true ELSE false END " +
           "FROM Curriculum_Group_Subject ccs " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.subject.subjectId = :subjectId " +
           "AND (:groupId IS NULL AND ccs.group IS NULL OR ccs.group.groupId = :groupId)")
    boolean existsByCurriculumAndSubjectAndGroup(
        @Param("curriculumId") UUID curriculumId,
        @Param("subjectId") UUID subjectId,
        @Param("groupId") UUID groupId
    );

    // Kiểm tra group có thuộc curriculum thông qua bảng Curriculum_Group_Subject
    @Query("SELECT CASE WHEN COUNT(ccs) > 0 THEN true ELSE false END " +
           "FROM Curriculum_Group_Subject ccs " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.group.groupId = :groupId")
    boolean existsByCurriculumAndGroup(
        @Param("curriculumId") UUID curriculumId,
        @Param("groupId") UUID groupId
    );

    // Tìm mapping cụ thể
    @Query("SELECT ccs FROM Curriculum_Group_Subject ccs " +
           "LEFT JOIN FETCH ccs.curriculum " +
           "LEFT JOIN FETCH ccs.group " +
           "LEFT JOIN FETCH ccs.subject " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.subject.subjectId = :subjectId " +
           "AND (:groupId IS NULL AND ccs.group IS NULL OR ccs.group.groupId = :groupId)")
    Optional<Curriculum_Group_Subject> findByCurriculumAndSubjectAndGroup(
        @Param("curriculumId") UUID curriculumId,
        @Param("subjectId") UUID subjectId,
        @Param("groupId") UUID groupId
    );

        // Tìm Curriculum_Group_Subject theo curriculum
    @Query("SELECT ccs FROM Curriculum_Group_Subject ccs " +
           "JOIN FETCH ccs.subject s " +
            "WHERE ccs.curriculum.curriculumId = :curriculumId")
        Page<Curriculum_Group_Subject> findByCurriculumId(
        @Param("curriculumId") UUID curriculumId,
        Pageable pageable
    );

        // Tìm Curriculum_Group_Subject theo group
    @Query("SELECT ccs FROM Curriculum_Group_Subject ccs " +
           "JOIN FETCH ccs.subject s " +
            "WHERE ccs.group.groupId = :groupId")
        Page<Curriculum_Group_Subject> findByGroupId(
        @Param("groupId") UUID groupId,
        Pageable pageable
    );

    @Query("SELECT ccs FROM Curriculum_Group_Subject ccs " +
           "JOIN FETCH ccs.subject s " +
           "LEFT JOIN FETCH ccs.group c " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "ORDER BY ccs.semester ASC, s.subjectCode ASC")
    List<Curriculum_Group_Subject> findAllByCurriculumIdOrderBySemester(
        @Param("curriculumId") UUID curriculumId
    );

    @Query("SELECT ccs FROM Curriculum_Group_Subject ccs " +
           "JOIN FETCH ccs.subject s " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND s.department.departmentId = :departmentId " +
           "ORDER BY ccs.semester ASC, s.subjectCode ASC")
    List<Curriculum_Group_Subject> findByCurriculumIdAndDepartmentId(
        @Param("curriculumId") UUID curriculumId,
        @Param("departmentId") UUID departmentId
    );

    @Modifying
    @Query("DELETE FROM Curriculum_Group_Subject ccs " +
           "WHERE ccs.curriculum.curriculumId = :curriculumId " +
           "AND ccs.subject.subjectId IN :subjectIds")
    int deleteByCurriculumIdAndSubjectIds(
        @Param("curriculumId") UUID curriculumId,
        @Param("subjectIds") List<UUID> subjectIds
    );
}
