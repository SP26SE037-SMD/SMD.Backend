package com.example.smd.repositories;

import com.example.smd.entities.Elective_Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectiveSubjectRepository extends JpaRepository<Elective_Subject, UUID> {
    // Xóa tất cả liên kết của một môn học (dùng khi update môn học)
    void deleteBySubject_SubjectId(UUID subjectId);

    @EntityGraph(attributePaths = {"elective"}) // Ép Hibernate tải kèm dữ liệu Elective
    List<Elective_Subject> findBySubject_SubjectId(UUID subjectId);

    // Tìm các môn thuộc một nhóm tự chọn
    List<Elective_Subject> findByElective_ElectiveId(UUID electiveId);

    boolean existsByElective_ElectiveIdAndSubject_SubjectId(UUID electiveId, UUID subjectId);
    boolean existsByElective_ElectiveId(UUID electiveId);

    Optional<Elective_Subject> findByElective_ElectiveIdAndSubject_SubjectId(UUID electiveId, UUID subjectId);
}
