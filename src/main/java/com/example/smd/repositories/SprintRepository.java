package com.example.smd.repositories;

import com.example.smd.entities.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, String> {
    Page<Sprint> findBySprintNameContainingIgnoreCase(String sprintName, Pageable pageable);
    
    Page<Sprint> findByStatus(String status, Pageable pageable);
}
