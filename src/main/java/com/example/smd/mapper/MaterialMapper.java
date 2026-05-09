package com.example.smd.mapper;

import com.example.smd.dto.request.MaterialRequest;
import com.example.smd.dto.response.MaterialResponse;
import com.example.smd.entities.Material;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    // Ánh xạ từ Entity sang Response
    @Mapping(target = "syllabusId", source = "syllabus.syllabusId")
    @Mapping(target = "status", source = "syllabus.status")
    MaterialResponse toResponse(Material material);

    // Ánh xạ từ Request sang Entity (Bỏ qua syllabus vì sẽ set thủ công ở Service)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "materialId", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "blocks", ignore = true)
    Material toEntity(MaterialRequest request);

    // Cập nhật Entity từ Request (Dùng cho hàm Update)
    @Mapping(target = "syllabus", ignore = true)
    @Mapping(target = "materialId", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "blocks", ignore = true)
    void updateMaterial(@MappingTarget Material material, MaterialRequest request);
}