package com.example.smd.repositories;

import com.example.smd.entities.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID>, JpaSpecificationExecutor<Sprint> {
    @EntityGraph(attributePaths = {"account", "account.department"})
    Page<Sprint> findBySprintNameContainingIgnoreCase(String sprintName, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "account.department"})
    Page<Sprint> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "account.department"})
    Page<Sprint> findByAccount_AccountId(UUID accountId, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "account.department"})
    Page<Sprint> findByCurriculum_CurriculumId(UUID curriculumId, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "account.department"})
    Optional<Sprint> findById(UUID id);

    @EntityGraph(attributePaths = {"account", "account.department"})
    Page<Sprint> findAll(org.springframework.data.jpa.domain.Specification<Sprint> spec, Pageable pageable);
}
