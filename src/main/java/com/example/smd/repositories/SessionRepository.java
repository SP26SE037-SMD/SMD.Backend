package com.example.smd.repositories;

import com.example.smd.entities.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>, JpaSpecificationExecutor<Session> {
    @EntityGraph(attributePaths = {"syllabus"})
    List<Session> findBySyllabus_SyllabusIdOrderBySessionNumberAsc(UUID syllabusId);

    @EntityGraph(attributePaths = {"syllabus"})
    List<Session> findBySyllabus_SyllabusId(UUID syllabusId);

    boolean existsBySyllabus_SyllabusIdAndSessionNumber(UUID syllabusId, Integer sessionNumber);

    Optional<Session> findBySyllabus_SyllabusIdAndSessionNumber(UUID syllabusId, Integer sessionNumber);

    List<Session> findBySyllabus_SyllabusIdAndSessionNumberIn(UUID syllabusId, List<Integer> sessionNumbers);

    boolean existsBySyllabus_SyllabusIdAndSessionNumberAndSessionIdNot(
            UUID syllabusId,
            Integer sessionNumber,
            UUID sessionId
    );

    @EntityGraph(attributePaths = {"syllabus"})
    Page<Session> findBySyllabus_SyllabusId(UUID syllabusId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"syllabus"})
    Page<Session> findAll(@Nullable Specification<Session> spec, Pageable pageable);

    /**
     * Xóa toàn bộ Session thuộc về một Syllabus.
     * Dùng trong luồng Import Excel theo kiểu "Replace" (xóa cũ, thêm mới).
     * Được gọi bên trong @Transactional nên không cần @Modifying riêng.
     */
    @Modifying
    @Query("DELETE FROM Session s WHERE s.syllabus.syllabusId = :syllabusId")
    void deleteAllBySyllabus_SyllabusId(@Param("syllabusId") UUID syllabusId);
}
