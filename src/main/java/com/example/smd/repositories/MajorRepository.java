package com.example.smd.repositories;

import com.example.smd.entities.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MajorRepository extends JpaRepository<Major, UUID> {
    // Kiểm tra tồn tại theo mã chuyên ngành
    boolean existsByMajorCode(String majorCode);

    // Tìm theo tên
    Page<Major> findByMajorNameContainingIgnoreCase(String name, Pageable pageable);

    // Tìm theo mã
    Page<Major> findByMajorCodeContainingIgnoreCase(String code, Pageable pageable);

    // Tìm theo cả tên và mã chuyên ngành
    Page<Major> findByMajorNameContainingIgnoreCaseOrMajorCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);

    // Tìm Major theo mã chuyên ngành chính xác
    Optional<Major> findByMajorCode(String majorCode);

    Page<Major> findByStatus(String status, Pageable pageable);

    Page<Major> findByMajorNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

    Page<Major> findByMajorCodeContainingIgnoreCaseAndStatus(String code, String status, Pageable pageable);

    // Trường hợp search cả name và code kèm status
    @Query("SELECT m FROM Major m WHERE (LOWER(m.majorName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.majorCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND m.status = :status")
    Page<Major> searchAllFieldsWithStatus(@Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query("SELECT m FROM Major m WHERE " +
            "(:status IS NULL OR CAST(m.status AS string) = :status) AND " +
            "m.updatedAt >= :startTime AND " +
            "m.updatedAt < :endTime")
    Page<Major> findByStatusAndUpdatedBetween(
            @Param("status") String status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);
}
