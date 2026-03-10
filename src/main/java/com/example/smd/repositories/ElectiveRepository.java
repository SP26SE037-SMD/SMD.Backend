package com.example.smd.repositories;

import com.example.smd.entities.Elective;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectiveRepository extends JpaRepository<Elective, UUID> {
    // Tìm kiếm phân trang theo tên hoặc mã
    Page<Elective> findAllByElectiveNameContainingIgnoreCaseOrElectiveCodeContainingIgnoreCase(
            String name, String code, Pageable pageable);

    @EntityGraph(attributePaths = {"electiveSubjects.subject"})
    Optional<Elective> findById(UUID id);

    boolean existsByElectiveCode(String electiveCode);
}
