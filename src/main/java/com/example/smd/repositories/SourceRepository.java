package com.example.smd.repositories;

import com.example.smd.entities.Source;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {
        // Tìm kiếm theo tên hoặc tác giả (phân trang)
        Page<Source> findBySourceNameContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                        String name, String author, Pageable pageable);

        @Query("SELECT s FROM Source s WHERE " +
                        "(:type IS NULL OR CAST(s.type AS string) = :type) AND " +
                        "(:search IS NULL OR s.sourceName ILIKE %:search% " +
                        "OR s.author ILIKE %:search%)")
        Page<Source> findByFilters(
                        @Param("type") String type,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT DISTINCT src FROM Source src " +
                        "JOIN Syllabus_Source ss ON src.sourceId = ss.source.sourceId " +
                        "JOIN Syllabus syl ON ss.syllabus.syllabusId = syl.syllabusId " +
                        "WHERE syl.subject.subjectId = :subjectId")
        List<Source> findAllBySubjectId(@Param("subjectId") UUID subjectId);
}
