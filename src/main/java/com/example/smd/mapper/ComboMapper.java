package com.example.smd.mapper;

import com.example.smd.dto.request.ComboRequest;
import com.example.smd.dto.response.ComboResponse;
import com.example.smd.entities.Combo;
import org.springframework.stereotype.Component;

@Component
public class ComboMapper {

    // Chuyển đổi từ Entity Combo sang DTO ComboResponse
    public ComboResponse toResponse(Combo combo) {
        if (combo == null) {
            return null;
        }

        return ComboResponse.builder()
                .comboId(combo.getComboId())
                .comboCode(combo.getComboCode())
                .comboName(combo.getComboName())
                .description(combo.getDescription())
                .createdAt(combo.getCreatedAt())
                .type(combo.getType())
                .build();
    }

    // Chuyển đổi từ DTO ComboRequest sang Entity Combo
    public Combo toEntity(ComboRequest request) {
        if (request == null) {
            return null;
        }

        return Combo.builder()
                .comboCode(request.getComboCode())
                .comboName(request.getComboName())
                .description(request.getDescription())
                .createdAt(java.time.Instant.now())
                .type(request.getType())
                .build();
    }

    // Cập nhật entity từ request
    public void updateEntityFromRequest(Combo combo, ComboRequest request) {
        if (combo == null || request == null) {
            return;
        }

        combo.setComboCode(request.getComboCode());
        combo.setComboName(request.getComboName());
        combo.setDescription(request.getDescription());
        combo.setType(request.getType());
    }
}
