package com.example.smd.repositories;

import com.example.smd.entities.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
