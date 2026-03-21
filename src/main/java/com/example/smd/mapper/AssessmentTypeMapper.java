package com.example.smd.mapper;

import com.example.smd.dto.request.AssessmentTypeRequest;
import com.example.smd.dto.response.AssessmentTypeResponse;
import com.example.smd.entities.Assessment_Type;
import org.springframework.stereotype.Component;

@Component
public class AssessmentTypeMapper {

    public AssessmentTypeResponse toResponse(Assessment_Type entity) {
        if (entity == null) {
            return null;
        }

        return AssessmentTypeResponse.builder()
                .typeId(entity.getTypeId())
                .typeName(entity.getTypeName())
                .build();
    }

    public Assessment_Type toEntity(AssessmentTypeRequest request) {
        if (request == null) {
            return null;
        }

        return Assessment_Type.builder()
                .typeName(request.getTypeName())
                .build();
    }

    public void updateEntity(Assessment_Type entity, AssessmentTypeRequest request) {
        if (request.getTypeName() != null) {
            entity.setTypeName(request.getTypeName());
        }
    }
}
