package com.example.smd.repositories;

import com.example.smd.entities.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByTaskNameContainingIgnoreCase(String taskName, Pageable pageable);
    
    Page<Task> findByStatus(String status, Pageable pageable);
    
    Page<Task> findBySprint_SprintId(UUID sprintId, Pageable pageable);
    
    Page<Task> findByAccount_AccountId(UUID accountId, Pageable pageable);
}
