package com.example.smd.repositories;

import com.example.smd.entities.Major;
import com.example.smd.entities.PLOs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PLOsRepository extends JpaRepository<PLOs, UUID> {
    // Tìm danh sách PLOs theo Major ID (Hỗ trợ phân trang)
    @Query("SELECT p FROM PLOs p JOIN FETCH p.curriculum WHERE p.curriculum.curriculumId = :curriculumId")
    Page<PLOs> findByCurriculum_CurriculumId(UUID curriculumId, Pageable pageable);

    @Query("SELECT p FROM PLOs p WHERE p.ploId = :id") // Hibernate sẽ tự động LEFT JOIN bảng Major
    Optional<PLOs> findById(UUID id);

    // Kiểm tra trùng code trong cùng một Major
    boolean existsByPloCodeAndMajor_MajorId(String ploCode, UUID majorId);

    // Kiểm tra trùng mã PLO trong cùng một Khung chương trình
    boolean existsByPloCodeAndCurriculum_CurriculumId(String ploCode, UUID curriculumId);
}
