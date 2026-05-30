package com.example.smd.repositories;

import com.example.smd.entities.Syllabus;
import com.example.smd.entities.SyllabusComparisonHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyllabusComparisonHistoryRepository extends JpaRepository<SyllabusComparisonHistory, UUID> {
    List<SyllabusComparisonHistory> findByOldSyllabusIdAndNewSyllabusIdOrderByCreatedAtDesc(UUID oldSyllabusId, UUID newSyllabusId);

    List<SyllabusComparisonHistory> findByNewSyllabusIdOrderByCreatedAtDesc(UUID newSyllabusId);

    Optional<SyllabusComparisonHistory> findFirstByNewSyllabusIdAndIsSelectedTrueOrderByCreatedAtDesc(UUID newSyllabusId);
}
