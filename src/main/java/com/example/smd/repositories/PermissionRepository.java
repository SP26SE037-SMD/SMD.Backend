package com.example.smd.repositories;

import com.example.smd.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, java.util.UUID>, JpaSpecificationExecutor<Permission> {
    Optional<Permission> findByPermissionName(String permissionName);
    boolean existsByPermissionName(String permissionName);
}
