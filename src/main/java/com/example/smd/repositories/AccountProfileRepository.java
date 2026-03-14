package com.example.smd.repositories;

import com.example.smd.entities.Account_Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountProfileRepository extends JpaRepository<Account_Profile, UUID> {

    // Tìm profile theo account ID
    @Query("SELECT ap FROM Account_Profile ap WHERE ap.account.accountId = :accountId")
    Optional<Account_Profile> findByAccountId(@Param("accountId") UUID accountId);

    // Kiểm tra tồn tại profile theo account ID
    @Query("SELECT CASE WHEN COUNT(ap) > 0 THEN true ELSE false END FROM Account_Profile ap WHERE ap.account.accountId = :accountId")
    boolean existsByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT ap FROM Account_Profile ap WHERE ap.account.accountId IN :accountIds")
    List<Account_Profile> findByAccountIds(@Param("accountIds") List<UUID> accountIds);
}
