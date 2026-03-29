package com.example.smd.repositories;

import com.example.smd.entities.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID>, JpaSpecificationExecutor<Sprint> {
    Page<Sprint> findBySprintNameContainingIgnoreCase(String sprintName, Pageable pageable);

    Page<Sprint> findByStatus(String status, Pageable pageable);

    Page<Sprint> findByAccount_AccountId(UUID accountId, Pageable pageable);

    Page<Sprint> findByCurriculum_CurriculumId(UUID curriculumId, Pageable pageable);
}
