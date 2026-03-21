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

        // Tìm kiếm Combo theo code hoặc name (không phân biệt hoa thường)
        @Query("SELECT c FROM Combo c WHERE LOWER(c.comboCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.comboName) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<Combo> findByCodeOrNameContaining(@Param("search") String search, Pageable pageable);

    // Lấy tất cả Combo với phân trang
    Page<Combo> findAll(Pageable pageable);

    // Tìm kiếm Combo theo type (Elective/Combo)
    @Query("SELECT c FROM Combo c WHERE LOWER(c.type) = LOWER(:type)")
    Page<Combo> findByType(@Param("type") String type, Pageable pageable);

        // Tìm kiếm Combo theo type và combo code
        @Query("SELECT c FROM Combo c WHERE LOWER(c.type) = LOWER(:type) " +
            "AND LOWER(c.comboCode) LIKE LOWER(CONCAT('%', :comboCode, '%'))")
        Page<Combo> findByTypeAndComboCodeContaining(
            @Param("type") String type,
            @Param("comboCode") String comboCode,
            Pageable pageable
        );

        // Tìm kiếm Combo theo type và combo name
        @Query("SELECT c FROM Combo c WHERE LOWER(c.type) = LOWER(:type) " +
            "AND LOWER(c.comboName) LIKE LOWER(CONCAT('%', :comboName, '%'))")
        Page<Combo> findByTypeAndComboNameContaining(
            @Param("type") String type,
            @Param("comboName") String comboName,
            Pageable pageable
        );

        // Tìm kiếm Combo theo type và keyword (code hoặc name)
        @Query("SELECT c FROM Combo c WHERE LOWER(c.type) = LOWER(:type) AND (" +
            "LOWER(c.comboCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.comboName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<Combo> findByTypeAndCodeOrNameContaining(
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable
        );
}
