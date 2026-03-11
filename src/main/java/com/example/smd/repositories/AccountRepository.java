package com.example.smd.repositories;

import com.example.smd.entities.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, java.util.UUID>, JpaSpecificationExecutor<Account> {

    // Tìm Account theo email và lấy luôn Role và Permissions
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<Account> findByEmail(String email);

    // Kiểm tra tồn tại theo email
    boolean existsByEmail(String email);

    // Tìm kiếm Account theo role name (không phân biệt hoa thường)
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :roleName, '%'))")
    Page<Account> findByRoleNameContaining(@Param("roleName") String roleName, Pageable pageable);

    // Tìm kiếm Account theo full name (không phân biệt hoa thường)
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.role WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    Page<Account> findByFullNameContaining(@Param("fullName") String fullName, Pageable pageable);

    // Lấy tất cả Account với role được fetch luôn
    @EntityGraph(attributePaths = {"role"})
    Page<Account> findAll(Pageable pageable);

    // Tìm kiếm Account theo khoảng thời gian createdAt
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.role WHERE a.createdAt BETWEEN :fromDate AND :toDate")
    Page<Account> findByCreatedAtBetween(
        @Param("fromDate") java.time.Instant fromDate,
        @Param("toDate") java.time.Instant toDate,
        Pageable pageable
    );

    // Tìm kiếm Account từ một ngày cụ thể trở đi
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.role WHERE a.createdAt >= :fromDate")
    Page<Account> findByCreatedAtAfter(@Param("fromDate") java.time.Instant fromDate, Pageable pageable);

    // Tìm kiếm Account đến một ngày cụ thể
    @Query("SELECT a FROM Account a LEFT JOIN FETCH a.role WHERE a.createdAt <= :toDate")
    Page<Account> findByCreatedAtBefore(@Param("toDate") java.time.Instant toDate, Pageable pageable);
}
