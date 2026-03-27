package com.example.smd.repositories;

import com.example.smd.entities.Curriculum;
import com.example.smd.entities.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "curriculum"})
    Page<Task> findByTaskNameContainingIgnoreCase(String taskName, Pageable pageable);

    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "curriculum"})
    Page<Task> findByStatusIgnoreCase(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "curriculum"})
    Page<Task> findBySprint_SprintId(UUID sprintId, Pageable pageable);

    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "curriculum"})
    Page<Task> findByAccount_AccountId(UUID accountId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "curriculum"})
    Page<Task> findAll(Pageable pageable);

    @Query("SELECT DISTINCT t.curriculum.curriculumId FROM Task t " +
           "WHERE t.account.accountId = :accountId AND t.curriculum IS NOT NULL")
    Set<UUID> findDistinctCurriculumIdsByAccountId(@Param("accountId") UUID accountId);

        @Query("SELECT DISTINCT t.account.accountId FROM Task t " +
            "WHERE t.syllabus.syllabusId = :syllabusId " +
            "AND t.account.department.departmentId = :departmentId")
        Set<UUID> findDistinctAccountIdsBySyllabusIdAndDepartmentId(
            @Param("syllabusId") UUID syllabusId,
            @Param("departmentId") UUID departmentId
        );

        @Query("SELECT DISTINCT c FROM Task t " +
            "JOIN t.curriculum c " +
            "WHERE t.account.accountId = :accountId " +
            "AND (:status IS NULL OR :status = '' OR LOWER(c.status) = LOWER(:status))")
        List<Curriculum> findDistinctCurriculumsByAccountIdAndStatus(
             @Param("accountId") UUID accountId,
             @Param("status") String status
        );
}
