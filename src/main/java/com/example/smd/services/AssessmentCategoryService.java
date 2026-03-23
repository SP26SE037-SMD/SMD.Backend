package com.example.smd.services;

import com.example.smd.dto.request.AssessmentCategoryRequest;
import com.example.smd.dto.response.AssessmentCategoryResponse;
import com.example.smd.entities.Assessment_Category;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AssessmentCategoryMapper;
import com.example.smd.repositories.AssessmentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssessmentCategoryService {

    private final AssessmentCategoryRepository assessmentCategoryRepository;
    private final AssessmentCategoryMapper assessmentCategoryMapper;

    @Transactional(readOnly = true)
    public Page<AssessmentCategoryResponse> getAllCategories(String search, int page, int size, String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        Specification<Assessment_Category> spec = (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("categoryName")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            );
        };

        return assessmentCategoryRepository.findAll(spec, pageable)
                .map(assessmentCategoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AssessmentCategoryResponse getCategoryById(UUID id) {
        Assessment_Category entity =
                assessmentCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_CATEGORY_NOT_FOUND));
        return assessmentCategoryMapper.toResponse(entity);
    }

    @Transactional
    public AssessmentCategoryResponse createCategory(AssessmentCategoryRequest request) {
        if (assessmentCategoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new AppException(ErrorCode.ASSESSMENT_CATEGORY_EXISTS);
        }

        Assessment_Category entity = assessmentCategoryMapper.toEntity(request);
        entity = assessmentCategoryRepository.save(entity);
        return assessmentCategoryMapper.toResponse(entity);
    }

    @Transactional
    public AssessmentCategoryResponse updateCategory(UUID id, AssessmentCategoryRequest request) {
        Assessment_Category entity = assessmentCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_CATEGORY_NOT_FOUND));

        if (request.getCategoryName() != null
                && !request.getCategoryName().equals(entity.getCategoryName())
                && assessmentCategoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new AppException(ErrorCode.ASSESSMENT_CATEGORY_EXISTS);
        }

        assessmentCategoryMapper.updateEntity(entity, request);
        entity = assessmentCategoryRepository.save(entity);
        return assessmentCategoryMapper.toResponse(entity);
    }

    @Transactional
    public boolean deleteCategory(UUID id) {
        Assessment_Category entity = assessmentCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_CATEGORY_NOT_FOUND));
        assessmentCategoryRepository.delete(entity);
        return true;
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
