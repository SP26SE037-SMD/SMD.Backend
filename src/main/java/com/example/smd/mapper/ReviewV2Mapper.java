package com.example.smd.mapper;

import com.example.smd.dto.response.ReviewV2Response;
import com.example.smd.entities.ReviewV2;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReviewV2Mapper {

    // ---- Response <- Entity ----

    @Mapping(target = "task.taskId",      source = "task.taskId")
    @Mapping(target = "task.taskName",    source = "task.taskName")
    @Mapping(target = "task.description", source = "task.description")
    @Mapping(target = "reviewer.accountId", source = "reviewer.accountId")
    @Mapping(target = "reviewer.fullName",  source = "reviewer.fullName")
    @Mapping(target = "reviewer.email",     source = "reviewer.email")
    ReviewV2Response toResponse(ReviewV2 review);

    // ---- Partial update: chỉ patch isAccepted & comment ----

    @Mapping(target = "reviewId",   ignore = true)
    @Mapping(target = "task",       ignore = true)
    @Mapping(target = "reviewer",   ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    void updateEntity(@MappingTarget ReviewV2 review,
                      com.example.smd.dto.request.reviewV2.ReviewV2UpdateRequest request);
}
