package com.example.smd.repositories;

import com.example.smd.entities.ReviewTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTask, UUID> {

    @EntityGraph(attributePaths = {"task", "reviewer"})
    Page<ReviewTask> findByTitleTaskContainingIgnoreCase(String titleTask, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "reviewer"})
    Page<ReviewTask> findByStatusIgnoreCase(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "reviewer"})
    Page<ReviewTask> findByTask_TaskId(UUID taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "reviewer"})
    Page<ReviewTask> findByReviewer_AccountId(UUID reviewerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"task", "reviewer"})
    Page<ReviewTask> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"task", "reviewer"})
    Optional<ReviewTask> findById(UUID reviewId);
}
