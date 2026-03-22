package com.example.smd.repositories;

import com.example.smd.entities.Curriculum;
import com.example.smd.entities.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByTaskNameContainingIgnoreCase(String taskName, Pageable pageable);

    Page<Task> findByStatusIgnoreCase(String status, Pageable pageable);

    Page<Task> findBySprint_SprintId(UUID sprintId, Pageable pageable);

    Page<Task> findByAccount_AccountId(UUID accountId, Pageable pageable);

    @Query("SELECT DISTINCT t.curriculum.curriculumId FROM Task t " +
           "WHERE t.account.accountId = :accountId AND t.curriculum IS NOT NULL")
    Set<UUID> findDistinctCurriculumIdsByAccountId(@Param("accountId") UUID accountId);

        @Query("SELECT DISTINCT c FROM Task t " +
            "JOIN t.curriculum c " +
            "WHERE t.account.accountId = :accountId " +
            "AND (:status IS NULL OR :status = '' OR LOWER(c.status) = LOWER(:status))")
        List<Curriculum> findDistinctCurriculumsByAccountIdAndStatus(
             @Param("accountId") UUID accountId,
             @Param("status") String status
        );
}
