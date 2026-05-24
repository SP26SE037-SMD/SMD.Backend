package com.example.smd.repositories;

import com.example.smd.entities.Syllabus;
import com.example.smd.entities.SyllabusComparisonHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SyllabusComparisonHistoryRepository extends JpaRepository<SyllabusComparisonHistory, UUID> {
    Optional<SyllabusComparisonHistory> findFirstByOldSyllabusIdAndNewSyllabusIdOrderByCreatedAtDesc(UUID oldSyllabusId, UUID newSyllabusId);
}
