package com.example.smd.repositories;

import com.example.smd.entities.Request;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class RequestSpecification {

    /**
     * @param search     tìm theo title (like, case-insensitive)
     * @param status     lọc theo status chính xác
     * @param type       lọc theo type (SYLLABUS, CURRICULUM, MAJOR, SUBJECT, TASK)
     * @param createdById lọc theo người tạo
     * @param receivedById lọc theo người nhận
     * @param targetId   lọc theo ID đối tượng liên quan
     */
    public static Specification<Request> withFilters(
            String search,
            String status,
            String type,
            UUID createdById,
            UUID receivedById,
            UUID targetId) {

        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (StringUtils.hasText(search)) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(status)) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(type)) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }

            if (createdById != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("createdBy").get("accountId"), createdById));
            }

            if (receivedById != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("receivedBy").get("accountId"), receivedById));
            }

            if (targetId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("targetId"), targetId));
            }

            return predicate;
        };
    }
}
