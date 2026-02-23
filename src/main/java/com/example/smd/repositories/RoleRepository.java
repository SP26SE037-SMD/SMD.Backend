package com.example.smd.repositories;

import com.example.smd.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, java.util.UUID>, JpaSpecificationExecutor<Role> {
    // Tìm Role theo tên
    Optional<Role> findByRoleName(String roleName);

    // Kiểm tra tồn tại theo tên
    boolean existsByRoleName(String roleName);
}
