package com.example.smd.mapper;

import com.example.smd.dto.request.RegulationRequest;
import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.entities.Regulation;
import org.springframework.stereotype.Component;

@Component
public class RegulationMapper {

    public RegulationResponse toResponse(Regulation entity) {
        if (entity == null) {
            return null;
        }

        return RegulationResponse.builder()
                .regulationId(entity.getRegulationId())
                .code(entity.getCode())
                .name(entity.getName())
                .value(entity.getValue())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public Regulation toEntity(RegulationRequest request) {
        if (request == null) {
            return null;
        }

        return Regulation.builder()
                .code(request.getCode())
                .name(request.getName())
                .value(request.getValue())
                .build();
    }

    public void updateEntity(Regulation entity, RegulationRequest request) {
        if (request.getCode() != null) {
            entity.setCode(request.getCode());
        }
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getValue() != null) {
            entity.setValue(request.getValue());
        }
    }
}
