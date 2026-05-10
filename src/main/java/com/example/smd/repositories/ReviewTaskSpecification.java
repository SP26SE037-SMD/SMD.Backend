//package com.example.smd.repositories;
//
//import com.example.smd.entities.ReviewTask;
//import jakarta.persistence.criteria.Predicate;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//public class ReviewTaskSpecification {
//
//    public static Specification<ReviewTask> withFilters(String search, String status, UUID taskId, UUID reviewerId) {
//        return (root, query, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//
//            if (search != null && !search.isEmpty()) {
//                predicates.add(cb.like(cb.lower(root.get("titleTask")), "%" + search.toLowerCase() + "%"));
//            }
//
//            if (status != null && !status.isEmpty()) {
//                predicates.add(cb.like(cb.lower(root.get("status")), "%" + status.toLowerCase() + "%"));
//            }
//
//            if (taskId != null) {
//                predicates.add(cb.equal(root.get("task").get("taskId"), taskId));
//            }
//
//            if (reviewerId != null) {
//                predicates.add(cb.equal(root.get("reviewer").get("accountId"), reviewerId));
//            }
//
//            return cb.and(predicates.toArray(new Predicate[0]));
//        };
//    }
//}
