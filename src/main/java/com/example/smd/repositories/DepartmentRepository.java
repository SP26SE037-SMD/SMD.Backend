package com.example.smd.repositories;

import com.example.smd.entities.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    boolean existsByDepartmentCode(String departmentCode);

    Page<Department> findAllByDepartmentNameContainingIgnoreCaseOrDepartmentCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);
}
