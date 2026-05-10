//package com.example.smd.repositories;
//
//import com.example.smd.entities.Subject;
//import com.example.smd.entities.Task;
//import jakarta.persistence.criteria.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//public class TaskSpecification {
//
//    public static Specification<Task> withFilters(String search, String status, UUID sprintId, UUID accountId, UUID departmentId, UUID syllabusId) {
//        return (root, query, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//
//            if (search != null && !search.isEmpty()) {
//                predicates.add(cb.like(cb.lower(root.get("taskName")), "%" + search.toLowerCase() + "%"));
//            }
//
//            if (status != null && !status.isEmpty()) {
//                predicates.add(cb.like(cb.lower(root.get("status")), "%" + status.toLowerCase() + "%"));
//            }
//
//            if (sprintId != null) {
//                predicates.add(cb.equal(root.get("sprint").get("sprintId"), sprintId));
//            }
//
//            if (accountId != null) {
//                predicates.add(cb.equal(root.get("account").get("accountId"), accountId));
//            }
//
//            if (syllabusId != null) {
//                predicates.add(cb.equal(root.get("syllabus").get("syllabusId"), syllabusId));
//            }
//
//            if (departmentId != null) {
//                Join<Task, Subject> subjectJoin = root.join("subject");
//                predicates.add(cb.equal(subjectJoin.get("department").get("departmentId"), departmentId));
//            }
//
//            return cb.and(predicates.toArray(new Predicate[0]));
//        };
//    }
//}
