package com.example.smd.repositories;

import com.example.smd.entities.CLO_Session;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CloSessionMappingRepository extends JpaRepository<CLO_Session, UUID> {

    @EntityGraph(attributePaths = {"clo", "session", "session.syllabus"})
    List<CLO_Session> findBySession_Syllabus_SyllabusId(UUID syllabusId);

    @EntityGraph(attributePaths = {"clo", "session", "session.syllabus"})
    List<CLO_Session> findByClo_CloId(UUID cloId);

    @EntityGraph(attributePaths = {"clo", "session", "session.syllabus"})
    List<CLO_Session> findBySession_SessionId(UUID sessionId);

    boolean existsByClo_CloIdAndSession_SessionId(UUID cloId, UUID sessionId);
}
