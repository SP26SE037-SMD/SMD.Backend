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

    @EntityGraph(attributePaths = {"task", "reviewer", "reviewer.role"})
    Page<ReviewTask> findByTitleTaskContainingIgnoreCase(String titleTask, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "reviewer", "reviewer.role"})
    Page<ReviewTask> findByStatusIgnoreCase(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "reviewer", "reviewer.role"})
    Page<ReviewTask> findByTask_TaskId(UUID taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "reviewer", "reviewer.role"})
    Page<ReviewTask> findByReviewer_AccountId(UUID reviewerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"task", "reviewer", "reviewer.role"})
    Page<ReviewTask> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"task", "reviewer", "reviewer.role"})
    Optional<ReviewTask> findById(UUID reviewId);
}
