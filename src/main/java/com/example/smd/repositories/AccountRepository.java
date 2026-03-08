package com.example.smd.repositories;

import com.example.smd.entities.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, java.util.UUID>, JpaSpecificationExecutor<Account> {

    // Tìm Account theo email và lấy luôn Role và Permissions
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<Account> findByEmail(String email);

    // Kiểm tra tồn tại theo email
    boolean existsByEmail(String email);
}
