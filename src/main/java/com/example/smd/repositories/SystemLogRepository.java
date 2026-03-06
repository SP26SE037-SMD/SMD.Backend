package com.example.smd.repositories;

import com.example.smd.entities.System_Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SystemLogRepository extends JpaRepository<System_Log, UUID> {

    // Lấy log theo account ID
    Page<System_Log> findByAccountAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    // Lấy log theo action
    Page<System_Log> findByActionContainingIgnoreCaseOrderByCreatedAtDesc(String action, Pageable pageable);

    // Tìm kiếm log theo action
    @Query("SELECT s FROM System_Log s WHERE " +
           "LOWER(s.action) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY s.createdAt DESC")
    Page<System_Log> searchLogs(@Param("search") String search, Pageable pageable);

    // Lấy log theo targetId
    Page<System_Log> findByTargetIdOrderByCreatedAtDesc(UUID targetId, Pageable pageable);

    // Lấy log trong khoảng thời gian
    Page<System_Log> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant startDate, Instant endDate, Pageable pageable);

    // Lấy log của user trong khoảng thời gian
    Page<System_Log> findByAccountAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID accountId, Instant startDate, Instant endDate, Pageable pageable);
}
