package com.example.smd.repositories;

import com.example.smd.entities.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, java.util.UUID>, JpaSpecificationExecutor<Account> {

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<Account> findByUsername(String username);

    Optional<Account> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
