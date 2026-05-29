package com.example.smd.repositories;

import com.example.smd.entities.System_Setting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<System_Setting, UUID> {
    boolean existsByCode(String code);
    Optional<System_Setting> findByCode(String code);
    Page<System_Setting> findAllByCodeContainingIgnoreCaseOrValueContainingIgnoreCase(
            String code, String value, Pageable pageable);
}
