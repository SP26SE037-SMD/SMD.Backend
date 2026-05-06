package com.example.smd.repositories;

import com.example.smd.entities.ReviewV2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewV2Repository extends JpaRepository<ReviewV2, UUID> {

    List<ReviewV2> findAllByTask_TaskId(UUID taskId);

    @Query("""
            SELECT r FROM ReviewV2 r
            LEFT JOIN r.task t
            WHERE (:taskId IS NULL OR t.taskId = :taskId)
              AND (:isAccepted IS NULL OR r.isAccepted = :isAccepted)
            ORDER BY r.createdAt DESC
            """)
    Page<ReviewV2> findAllWithFilters(
            @Param("taskId") UUID taskId,
            @Param("isAccepted") Boolean isAccepted,
            Pageable pageable
    );

    boolean existsByTask_TaskId(UUID taskId);
}
