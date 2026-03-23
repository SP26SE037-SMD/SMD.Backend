package com.example.smd.repositories;

import com.example.smd.entities.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID>, JpaSpecificationExecutor<Session> {

    List<Session> findBySyllabus_SyllabusIdOrderBySessionNumberAsc(UUID syllabusId);

    boolean existsBySyllabus_SyllabusIdAndSessionNumber(UUID syllabusId, Integer sessionNumber);

    Optional<Session> findBySyllabus_SyllabusIdAndSessionNumber(UUID syllabusId, Integer sessionNumber);

    List<Session> findBySyllabus_SyllabusIdAndSessionNumberIn(UUID syllabusId, List<Integer> sessionNumbers);

    boolean existsBySyllabus_SyllabusIdAndSessionNumberAndSessionIdNot(
            UUID syllabusId,
            Integer sessionNumber,
            UUID sessionId
    );

    Page<Session> findBySyllabus_SyllabusId(UUID syllabusId, Pageable pageable);
}
