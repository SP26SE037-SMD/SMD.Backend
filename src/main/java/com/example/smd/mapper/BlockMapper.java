package com.example.smd.mapper;

import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.request.UpdateBlockRequest;
import com.example.smd.dto.response.BlockResponse;
import com.example.smd.entities.Blocks;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BlockMapper {

    // Ánh xạ từ Entity sang Response (Trả về cho Frontend)
    BlockResponse toResponse(Blocks block);

    // Ánh xạ từ Request sang Entity để lưu DB
    @Mapping(target = "blockId", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "idx", ignore = true) // Sẽ được set thủ công bằng vòng lặp ở Service
    Blocks toEntity(BlockRequest request);

    // Cập nhật nội dung Block hiện có
    @Mapping(target = "blockId", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "idx", ignore = true)
    void updateBlock(@MappingTarget Blocks block, BlockRequest request);

    @Mapping(target = "blockId", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "idx", ignore = true)
    void updateBlockList(@MappingTarget Blocks block, UpdateBlockRequest request);

    @Mapping(target = "blockId", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Blocks cloneBlock(Blocks block);
}