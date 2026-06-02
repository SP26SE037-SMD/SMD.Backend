package com.example.smd.repositories;

import com.example.smd.entities.RubricLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RubricLevelRepository extends JpaRepository<RubricLevel, UUID> {
    Optional<RubricLevel> findByLevelCode(String levelCode);
}
