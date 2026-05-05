package com.example.smd.repositories;

import com.example.smd.entities.TaskV2;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskV2Specification {

    public static Specification<TaskV2> withFilters(String search, String status, UUID sprintId, String type, String action, UUID assignTo, UUID createdBy, UUID targetId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("taskName")), "%" + search.toLowerCase() + "%"));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("status")), "%" + status.toLowerCase() + "%"));
            }

            if (sprintId != null) {
                predicates.add(cb.equal(root.get("sprint").get("sprintId"), sprintId));
            }

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("type")), type.toLowerCase()));
            }

            if (action != null && !action.isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("action")), action.toLowerCase()));
            }

            if (assignTo != null) {
                predicates.add(cb.equal(root.get("account").get("accountId"), assignTo));
            }

            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy").get("accountId"), createdBy));
            }

            if (targetId != null) {
                predicates.add(cb.equal(root.get("targetId"), targetId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
