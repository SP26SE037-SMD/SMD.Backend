package com.example.smd.services;

import com.example.smd.dto.request.AssessmentTypeRequest;
import com.example.smd.dto.response.AssessmentTypeResponse;
import com.example.smd.entities.Assessment_Type;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AssessmentTypeMapper;
import com.example.smd.repositories.AssessmentTypeRepository;
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
public class AssessmentTypeService {

    private final AssessmentTypeRepository assessmentTypeRepository;
    private final AssessmentTypeMapper assessmentTypeMapper;

    @Transactional(readOnly = true)
    public Page<AssessmentTypeResponse> getAllTypes(String search, int page, int size, String[] sort) {
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

        Specification<Assessment_Type> spec = (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("typeName")), pattern);
        };

        return assessmentTypeRepository.findAll(spec, pageable)
                .map(assessmentTypeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public AssessmentTypeResponse getTypeById(UUID id) {
        Assessment_Type entity =
                assessmentTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_TYPE_NOT_FOUND));
        return assessmentTypeMapper.toResponse(entity);
    }

    @Transactional
    public AssessmentTypeResponse createType(AssessmentTypeRequest request) {
        if (assessmentTypeRepository.existsByTypeName(request.getTypeName())) {
            throw new AppException(ErrorCode.ASSESSMENT_TYPE_EXISTS);
        }

        Assessment_Type entity = assessmentTypeMapper.toEntity(request);
        entity = assessmentTypeRepository.save(entity);
        return assessmentTypeMapper.toResponse(entity);
    }

    @Transactional
    public AssessmentTypeResponse updateType(UUID id, AssessmentTypeRequest request) {
        Assessment_Type entity =
                assessmentTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_TYPE_NOT_FOUND));

        if (request.getTypeName() != null
                && !request.getTypeName().equals(entity.getTypeName())
                && assessmentTypeRepository.existsByTypeName(request.getTypeName())) {
            throw new AppException(ErrorCode.ASSESSMENT_TYPE_EXISTS);
        }

        assessmentTypeMapper.updateEntity(entity, request);
        entity = assessmentTypeRepository.save(entity);
        return assessmentTypeMapper.toResponse(entity);
    }

    @Transactional
    public boolean deleteType(UUID id) {
        Assessment_Type entity = assessmentTypeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_TYPE_NOT_FOUND));
        assessmentTypeRepository.delete(entity);
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
