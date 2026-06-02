package com.example.smd.repositories;

import com.example.smd.entities.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, UUID> {
    List<Rubric> findBySyllabus_SyllabusId(UUID syllabusId);
    boolean existsByCode(String code);
    Optional<Rubric> findByCode(String code);
}
