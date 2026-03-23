package com.example.smd.repositories;

import com.example.smd.entities.Assessment_Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentTypeRepository extends JpaRepository<Assessment_Type, UUID>, JpaSpecificationExecutor<Assessment_Type> {
	boolean existsByTypeName(String typeName);

	Optional<Assessment_Type> findByTypeName(String typeName);
}
