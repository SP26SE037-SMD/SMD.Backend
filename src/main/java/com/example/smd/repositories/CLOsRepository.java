package com.example.smd.repositories;

import com.example.smd.entities.CLOs;
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
public interface CLOsRepository extends JpaRepository<CLOs, UUID> {

    @Query("SELECT p FROM CLOs p JOIN FETCH p.subject WHERE p.cloId = :id") // Hoặc "syllabus" tùy tên field trong Entity
    Optional<CLOs> findById(UUID id);

    // Tìm các CLOs theo ID của môn học (Subject/Syllabus)
    @Query("SELECT p FROM CLOs p JOIN FETCH p.subject WHERE p.subject.subjectId = :subjectId")
    Page<CLOs> findBySubject_SubjectId(UUID subjectId, Pageable pageable);

    // Kiểm tra trùng mã CLO trong cùng một môn học
    boolean existsByCloCodeAndSubject_SubjectId(String cloCode, UUID subjectId);

    boolean existsByCloCodeInAndSubject_SubjectId(List<String> cloCodes, UUID subjectId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CLOs c SET c.status = :status WHERE c.subject.subjectId = :subjectId")
    int updateStatusBySubjectId(@Param("status") String status, @Param("subjectId") UUID subjectId);

    // Thêm hàm này để lọc theo status cho Role thấp
    Page<CLOs> findBySubject_SubjectIdAndStatus(UUID subjectId, String status, Pageable pageable);
}
