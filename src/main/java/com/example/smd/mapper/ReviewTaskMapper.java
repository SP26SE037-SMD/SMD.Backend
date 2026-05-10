//package com.example.smd.mapper;
//
//import com.example.smd.dto.request.reviewtask.ReviewTaskCreateHoCFDC;
//import com.example.smd.dto.request.reviewtask.ReviewTaskCreateRequest;
//import com.example.smd.dto.request.reviewtask.ReviewTaskRequest;
//import com.example.smd.dto.response.reviewtask.ReviewTaskResponse;
//import com.example.smd.entities.ReviewTask;
//import org.mapstruct.Mapper;
//import org.mapstruct.Mapping;
//import org.mapstruct.MappingTarget;
//
//@Mapper(componentModel = "spring")
//public interface ReviewTaskMapper {
//
//    @Mapping(target = "reviewId", ignore = true)
//    @Mapping(target = "task", ignore = true)
//    @Mapping(target = "reviewer", ignore = true)
//    ReviewTask toReviewTask(ReviewTaskRequest request);
//
//    @Mapping(target = "commentMaterial", ignore = true)
//    @Mapping(target = "commentSession", ignore = true)
//    @Mapping(target = "commentAssessment", ignore = true)
//    @Mapping(target = "reviewDate", ignore = true)
//    ReviewTaskRequest toReviewTaskRequest(ReviewTaskCreateRequest request);
//
//
//    @Mapping(target = "commentMaterial", ignore = true)
//    @Mapping(target = "commentSession", ignore = true)
//    @Mapping(target = "commentAssessment", ignore = true)
//    @Mapping(target = "reviewDate", ignore = true)
//    @Mapping(target = "dueDate", ignore = true)
//    ReviewTaskRequest toReviewTaskRequestHoCFDC(ReviewTaskCreateHoCFDC request);
//
//
//    @Mapping(target = "task.taskId", source = "task.taskId")
//    @Mapping(target = "task.taskName", source = "task.taskName")
//    @Mapping(target = "task.assignedToId", source = "task.account.accountId")
//    @Mapping(target = "reviewer.reviewerId", source = "reviewer.accountId")
//    @Mapping(target = "reviewer.fullName", source = "reviewer.fullName")
//    @Mapping(target = "reviewer.email", source = "reviewer.email")
//    @Mapping(target = "reviewer.avatarUrl", source = "reviewer.avatarUrl")
//    @Mapping(target = "reviewer.role", source = "reviewer.role.roleName")
//    ReviewTaskResponse toReviewTaskResponse(ReviewTask reviewTask);
//
//    @Mapping(target = "reviewId", ignore = true)
//    @Mapping(target = "task", ignore = true)
//    @Mapping(target = "reviewer", ignore = true)
//    @Mapping(target = "isAccepted", ignore = true)
//    void updateReviewTask(@MappingTarget ReviewTask reviewTask, ReviewTaskRequest request);
//}
