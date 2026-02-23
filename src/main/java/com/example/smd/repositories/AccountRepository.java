package com.example.smd.repositories;

import com.example.smd.entities.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, java.util.UUID>, JpaSpecificationExecutor<Account> {

    // Tìm Account theo username và lấy luôn Role và Permissions
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<Account> findByUsername(String username);

    // Tìm Account theo email
    Optional<Account> findByEmail(String email);

    // Kiểm tra tồn tại theo username
    boolean existsByUsername(String username);

    // Kiểm tra tồn tại theo email
    boolean existsByEmail(String email);
}
