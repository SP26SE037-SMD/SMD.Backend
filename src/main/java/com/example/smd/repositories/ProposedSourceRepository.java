package com.example.smd.repositories;

import com.example.smd.entities.ProposedSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposedSourceRepository extends JpaRepository<ProposedSource, UUID> {

    boolean existsBySource_SourceIdAndSubject_SubjectId(UUID sourceId, UUID subjectId);

    List<ProposedSource> findAllBySubject_SubjectId(UUID subjectId);

    @Query("SELECT ps FROM ProposedSource ps JOIN FETCH ps.source WHERE ps.subject.subjectId = :subjectId")
    List<ProposedSource> fetchWithSourceBySubjectId(@Param("subjectId") UUID subjectId);

    @Query("SELECT ps FROM ProposedSource ps JOIN FETCH ps.source WHERE ps.source.sourceId = :sourceId")
    List<ProposedSource> fetchWithSourceBySourceId(@Param("sourceId") UUID sourceId);

    @Query("SELECT ps FROM ProposedSource ps JOIN FETCH ps.source WHERE ps.proposedSourceId = :id")
    java.util.Optional<ProposedSource> fetchWithSourceById(@Param("id") UUID id);

    List<ProposedSource> findAllBySource_SourceId(UUID sourceId);

    @Query("SELECT ps FROM ProposedSource ps " +
            "JOIN FETCH ps.source s " +
            "JOIN ps.subject sub " +
            "WHERE (:search IS NULL " +
            "   OR s.sourceName ILIKE %:search% " +
            "   OR s.sourceCode ILIKE %:search% " +
            "   OR sub.subjectCode ILIKE %:search% " +
            "   OR sub.subjectName ILIKE %:search%)")
    Page<ProposedSource> findByFilters(@Param("search") String search, Pageable pageable);
}
