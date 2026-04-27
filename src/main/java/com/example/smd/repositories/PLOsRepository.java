package com.example.smd.repositories;

import com.example.smd.entities.PLOs;
import com.example.smd.entities.PO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Kiểm tra trùng mã PLO trong cùng một Khung chương trình
    boolean existsByPloCodeAndCurriculum_CurriculumId(String ploCode, UUID curriculumId);

    boolean existsByPloCodeInAndCurriculum_CurriculumId(List<String> ploCodes, UUID curriculumId);

    // Cập nhật trạng thái hàng loạt - Tốc độ nhanh nhất
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PLOs p SET p.status = :status WHERE p.curriculum.curriculumId = :curriculumId")
    int updateStatusByCurriculumId(@Param("status") String status, @Param("curriculumId") UUID curriculumId);

    // Lấy PLO của Curriculum theo Status (Dành cho Student/Lecturer)
    Page<PLOs> findByCurriculum_CurriculumIdAndStatus(UUID curriculumId, String status, Pageable pageable);

    List<PLOs> findByCurriculum_CurriculumId(UUID curriculumId);
}
