package com.example.smd.repositories;

import com.example.smd.entities.Regulation;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegulationRepository extends JpaRepository<Regulation, UUID>, JpaSpecificationExecutor<Regulation> {
    Optional<Regulation> findByCode(String code);

    @Transactional
    @Modifying
    @Query("DELETE FROM Regulation r WHERE r.major.majorId = :majorId")
    void deleteByMajorId(@Param("majorId") UUID majorId);

    boolean existsByCode(String code);

    boolean existsByCodeAndRegulationIdNot(String code, UUID regulationId);

    Optional<Regulation> findByCodeAndMajor_MajorId(String code, UUID majorId);

    /**
     * Kiểm tra xem mã code đã tồn tại trong cùng một Major hay chưa
     * (Ngoại trừ chính Regulation đang được cập nhật)
     *
     * @param code Mã Regulation cần kiểm tra
     * @param majorId ID của Major
     * @param id ID của Regulation đang được update (để loại trừ)
     * @return true nếu đã bị trùng, false nếu hợp lệ
     */
    boolean existsByCodeAndMajor_MajorIdAndRegulationIdNot(String code, UUID majorId, UUID id);
}
