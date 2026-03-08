package com.example.smd.repositories;

import com.example.smd.entities.Curriculum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, String> {

    // Kiểm tra tồn tại theo mã chương trình
    boolean existsByCurriculumCode(String curriculumCode);

    // Tìm theo curriculum code
    Optional<Curriculum> findByCurriculumCode(String curriculumCode);

    // Tìm theo tên chương trình (tìm kiếm không phân biệt hoa thường)
    Page<Curriculum> findByCurriculumNameContainingIgnoreCase(String name, Pageable pageable);

    // Tìm theo mã chương trình (tìm kiếm không phân biệt hoa thường)
    Page<Curriculum> findByCurriculumCodeContainingIgnoreCase(String code, Pageable pageable);

    // Tìm theo cả tên và mã chương trình
    Page<Curriculum> findByCurriculumNameContainingIgnoreCaseOrCurriculumCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);

    // Tìm theo status
    Page<Curriculum> findByStatus(String status, Pageable pageable);

    // Tìm kiếm kết hợp với filter
    @Query("SELECT c FROM Curriculum c WHERE " +
            "(:search IS NULL OR " +
            "LOWER(c.curriculumName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.curriculumCode) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:status IS NULL OR c.status = :status)")
    Page<Curriculum> findWithFilters(
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable);

}
