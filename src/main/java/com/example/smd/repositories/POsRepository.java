package com.example.smd.repositories;

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
public interface POsRepository extends JpaRepository<PO, UUID> {

    boolean existsByPoCode(String poCode);

    boolean existsByPoCodeAndMajor_MajorId(String poCode, UUID majorId);

    boolean existsByPoCodeInAndMajor_MajorId(List<String> poCodes, UUID majorId);

    Page<PO> findByMajor_MajorId(UUID majorId, Pageable pageable);

    List<PO> findByMajor_MajorId(UUID majorId);

    Optional<PO> findByPoCodeAndMajor_MajorCode(String poCode, String majorCode);

    // Tìm PO của Major theo Status (Để phục vụ phân quyền Role)
    Page<PO> findByMajor_MajorIdAndStatus(UUID majorId, String status, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PO p SET p.status = :status WHERE p.major.majorId = :majorId")
    int updateStatusByMajorId(@Param("status") String status, @Param("majorId") UUID majorId);
}
