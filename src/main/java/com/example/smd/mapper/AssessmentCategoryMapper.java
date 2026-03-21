package com.example.smd.mapper;

import com.example.smd.dto.request.AssessmentCategoryRequest;
import com.example.smd.dto.response.AssessmentCategoryResponse;
import com.example.smd.entities.Assessment_Category;
import org.springframework.stereotype.Component;

@Component
public class AssessmentCategoryMapper {

    public AssessmentCategoryResponse toResponse(Assessment_Category entity) {
        if (entity == null) {
            return null;
        }

        return AssessmentCategoryResponse.builder()
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .description(entity.getDescription())
                .build();
    }

    public Assessment_Category toEntity(AssessmentCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return Assessment_Category.builder()
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .build();
    }

    public void updateEntity(Assessment_Category entity, AssessmentCategoryRequest request) {
        if (request.getCategoryName() != null) {
            entity.setCategoryName(request.getCategoryName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }
}
