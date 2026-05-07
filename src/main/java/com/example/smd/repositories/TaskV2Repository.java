package com.example.smd.repositories;

import com.example.smd.entities.TaskV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV2, UUID>, JpaSpecificationExecutor<TaskV2> {

    /**
     * Trả về tập tất cả targetId đã tồn tại dưới dạng task trong sprint này,
     * dùng để tránh tạo task trùng lặp khi chạy createBatch.
     */
    @Query("SELECT t.targetId FROM TaskV2 t WHERE t.sprint.sprintId = :sprintId AND t.targetId IS NOT NULL")
    Set<UUID> findAllTargetIdsInSprint(@Param("sprintId") UUID sprintId);
}
