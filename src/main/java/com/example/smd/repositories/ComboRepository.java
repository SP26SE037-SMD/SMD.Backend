package com.example.smd.repositories;

import com.example.smd.entities.Combo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ComboRepository extends JpaRepository<Combo, UUID> {

    // Kiểm tra tồn tại theo combo code
    boolean existsByComboCode(String comboCode);

    // Tìm kiếm Combo theo combo code (không phân biệt hoa thường)
    @Query("SELECT c FROM Combo c WHERE LOWER(c.comboCode) LIKE LOWER(CONCAT('%', :comboCode, '%'))")
    Page<Combo> findByComboCodeContaining(@Param("comboCode") String comboCode, Pageable pageable);

    // Tìm kiếm Combo theo combo name (không phân biệt hoa thường)
    @Query("SELECT c FROM Combo c WHERE LOWER(c.comboName) LIKE LOWER(CONCAT('%', :comboName, '%'))")
    Page<Combo> findByComboNameContaining(@Param("comboName") String comboName, Pageable pageable);

    // Lấy tất cả Combo với phân trang
    Page<Combo> findAll(Pageable pageable);
}
