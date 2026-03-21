package com.example.smd.repositories;

import com.example.smd.entities.Blocks;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Blocks, UUID> {
    // Lấy danh sách block và sắp xếp theo thứ tự idx tăng dần
    List<Blocks> findAllByMaterial_MaterialIdOrderByIdxAsc(UUID materialId);

    // Xóa tất cả blocks cũ của một material để ghi đè (thường dùng khi cập nhật bài viết)
    void deleteAllByMaterial_MaterialId(UUID materialId);

    Page<Blocks> findAllByMaterial_MaterialId(UUID materialId, Pageable pageable);

    @Query("SELECT b.contentText FROM Blocks b " +
            "WHERE b.material.materialId = :materialId " +
            "AND b.blockStyle IN ('H1', 'H2') " +
            "ORDER BY b.idx ASC")
    List<String> findTitlesByMaterialId(@Param("materialId") UUID materialId);
}
