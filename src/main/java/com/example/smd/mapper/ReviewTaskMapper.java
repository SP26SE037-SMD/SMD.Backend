package com.example.smd.mapper;

import com.example.smd.dto.request.reviewtask.ReviewTaskRequest;
import com.example.smd.dto.response.reviewtask.ReviewTaskResponse;
import com.example.smd.entities.ReviewTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReviewTaskMapper {

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    ReviewTask toReviewTask(ReviewTaskRequest request);

    @Mapping(target = "task.taskId", source = "task.taskId")
    @Mapping(target = "task.taskName", source = "task.taskName")
    @Mapping(target = "reviewer.reviewerId", source = "reviewer.accountId")
    @Mapping(target = "reviewer.fullName", source = "reviewer.fullName")
    @Mapping(target = "reviewer.email", source = "reviewer.email")
    @Mapping(target = "reviewer.avatarUrl", source = "reviewer.avatarUrl")
    ReviewTaskResponse toReviewTaskResponse(ReviewTask reviewTask);

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    void updateReviewTask(@MappingTarget ReviewTask reviewTask, ReviewTaskRequest request);
}
