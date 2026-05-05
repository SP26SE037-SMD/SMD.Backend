package com.example.smd.repositories;

import com.example.smd.entities.TaskV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TaskV2Repository extends JpaRepository<TaskV2, UUID>, JpaSpecificationExecutor<TaskV2> {
}
