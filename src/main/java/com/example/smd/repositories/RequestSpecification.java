package com.example.smd.repositories;

import com.example.smd.entities.Request;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class RequestSpecification {
    public static Specification<Request> withFilters(String search, String status, UUID curriculumId, UUID majorId) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (StringUtils.hasText(search)) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (curriculumId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("curriculum").get("curriculumId"), curriculumId));
            }

            if (majorId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("major").get("majorId"), majorId));
            }

            return predicate;
        };
    }
}
