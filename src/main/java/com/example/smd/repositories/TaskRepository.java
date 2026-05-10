//package com.example.smd.repositories;
//
//import com.example.smd.entities.Task;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.data.jpa.repository.EntityGraph;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.UUID;
//
//@Repository
//public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
//
//    @Override
//    @EntityGraph(attributePaths = {"subject"})
//    Optional<Task> findById(UUID id);
//
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findByTaskNameContainingIgnoreCase(String taskName, Pageable pageable);
//
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findByStatusIgnoreCase(String status, Pageable pageable);
//
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findBySprint_SprintId(UUID sprintId, Pageable pageable);
//
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findByAccount_AccountId(UUID accountId, Pageable pageable);
//
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findBySyllabus_SyllabusId(UUID syllabusId, Pageable pageable);
//
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findBySubject_SubjectIdIn(List<UUID> subjectIds, Pageable pageable);
//
//        long countBySprint_SprintId(UUID sprintId);
//
//        @Query("SELECT DISTINCT t.subject.subjectId FROM Task t " +
//            "WHERE t.sprint.sprintId = :sprintId " +
//            "AND t.subject.subjectId IN :subjectIds")
//        Set<UUID> findExistingSubjectIdsInSprint(
//            @Param("sprintId") UUID sprintId,
//            @Param("subjectIds") Set<UUID> subjectIds
//        );
//
//    @Override
//    @EntityGraph(attributePaths = {"sprint", "account", "syllabus", "subject"})
//    Page<Task> findAll(Specification<Task> spec, Pageable pageable);
//
//        @Query("SELECT DISTINCT t.account.accountId FROM Task t " +
//            "WHERE t.syllabus.syllabusId = :syllabusId " +
//            "AND t.account.department.departmentId = :departmentId")
//        Set<UUID> findDistinctAccountIdsBySyllabusIdAndDepartmentId(
//            @Param("syllabusId") UUID syllabusId,
//            @Param("departmentId") UUID departmentId
//        );
//}
