package com.example.smd.repositories;

import com.example.smd.entities.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID>, JpaSpecificationExecutor<Subject> {

    // Giữ lại hàm kiểm tra trùng mã môn học hiện có
    boolean existsBySubjectCode(String subjectCode);

    // JPQL sử dụng JOIN FETCH để lấy kèm Department và CLOs
    @Query("SELECT DISTINCT s FROM Subject s " +
            "LEFT JOIN FETCH s.department " +
            "LEFT JOIN FETCH s.clos " +
            "WHERE s.subjectId = :id")
    Optional<Subject> findDetailById(@Param("id") UUID id);

    // Đối với danh sách, dùng EntityGraph là cách an toàn nhất để phân trang ở mức Database
    @EntityGraph(attributePaths = {"department", "clos"})
    @Override
    Page<Subject> findAll(Specification<Subject> spec, Pageable pageable);

    @Query("SELECT s FROM Subject s JOIN FETCH s.department WHERE s.department.departmentId = :deptId")
    List<Subject> findAllByDepartmentId(@Param("deptId") UUID deptId);

    @Query("SELECT s FROM Subject s WHERE s.subjectId IN " +
            "(SELECT es.subject.subjectId FROM Elective_Subject es WHERE es.elective.electiveId = :electiveId)")
    List<Subject> findSubjectsByElectiveId(@Param("electiveId") UUID electiveId);
}
