package com.example.smd.repositories;

import com.example.smd.entities.Assessment_Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssessmentCategoryRepository extends JpaRepository<Assessment_Category, String>, JpaSpecificationExecutor<Assessment_Category> {
	boolean existsByCategoryName(String categoryName);

	Optional<Assessment_Category> findByCategoryName(String categoryName);
}
