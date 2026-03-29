package com.example.smd.repositories;

import com.example.smd.entities.Sprint;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SprintSpecification {

    public static Specification<Sprint> withFilters(String search, String status, UUID curriculumId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("sprintName")), "%" + search.toLowerCase() + "%"));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("status")), "%" + status.toLowerCase() + "%"));
            }

            if (curriculumId != null) {
                predicates.add(cb.equal(root.get("curriculum").get("curriculumId"), curriculumId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
