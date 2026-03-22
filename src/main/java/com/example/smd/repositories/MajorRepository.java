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

    @Query("SELECT m FROM Major m WHERE " +
            "(:status IS NULL OR CAST(m.status AS string) = :status) AND " +
            "m.updatedAt >= :startTime AND " +
            "m.updatedAt < :endTime")
    Page<Major> findByStatusAndUpdatedBetween(
            @Param("status") String status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    @Query("SELECT m FROM Major m LEFT JOIN FETCH m.curriculums WHERE m.majorId = :id")
    Optional<Major> findByIdWithCurriculums(@Param("id") UUID id);

    // 1. Chỉ lọc Status
    @Query("SELECT m FROM Major m WHERE m.status = :status")
    Page<Major> findByStatus(@Param("status") String status, Pageable pageable);

    // 2. Search Code + Status (AND)
    @Query("SELECT m FROM Major m WHERE LOWER(m.majorCode) LIKE LOWER(CONCAT('%', :search, '%')) AND m.status = :status")
    Page<Major> findByMajorCodeContainingIgnoreCaseAndStatus(String search, String status, Pageable pageable);

    // 3. Search Name + Status (AND)
    @Query("SELECT m FROM Major m WHERE LOWER(m.majorName) LIKE LOWER(CONCAT('%', :search, '%')) AND m.status = :status")
    Page<Major> findByMajorNameContainingIgnoreCaseAndStatus(String search, String status, Pageable pageable);

    // 4. Search All Fields + Status (Sử dụng OR cho Search, nhưng AND cho Status)
    @Query("SELECT m FROM Major m WHERE (LOWER(m.majorName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(m.majorCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND m.status = :status")
    Page<Major> searchAllFieldsWithStatus(String search, String status, Pageable pageable);
}
