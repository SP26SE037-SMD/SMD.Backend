package com.example.smd.controller;

import com.example.smd.dto.request.task.BatchTaskRequest;
import com.example.smd.dto.request.task.TaskRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.task.TaskResponse;
import com.example.smd.services.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Task", description = "Endpoints for managing tasks within sprints")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task", description = "accountId và taskName là bắt buộc. sprintId bắt buộc cho tất cả người dùng.")
    public ResponseObject<TaskResponse> create(@RequestBody @Valid TaskRequest request) {
        return ResponseObject.<TaskResponse>builder()
                .data(taskService.create(request))
                .message("Task created successfully")
                .build();
    }

    @PostMapping("/batch/{sprintId}")
    @Operation(summary = "Create multiple tasks in a sprint")
    public ResponseObject<List<TaskResponse>> createBatch(
            @PathVariable UUID sprintId,
            @RequestBody @Valid BatchTaskRequest request) {
        return ResponseObject.<List<TaskResponse>>builder()
                .data(taskService.createBatch(sprintId, request))
                .message("Tasks created successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Search and filter tasks with pagination")
    public ResponseObject<PagedResponse<TaskResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deadline") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<TaskResponse>>builder()
                .data(PagedResponse.of(taskService.getAll(search, status, sprintId, accountId, pageable)))
                .message("Tasks retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task detail by ID")
    public ResponseObject<TaskResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<TaskResponse>builder()
                .data(taskService.getDetail(id))
                .message("Task detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task information")
    public ResponseObject<TaskResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid TaskRequest request) {
        return ResponseObject.<TaskResponse>builder()
                .data(taskService.update(id, request))
                .message("Task updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        taskService.delete(id);
        return ResponseObject.<Void>builder()
                .message("Task deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
        @Operation(summary = "Update task status", description = "Accepted values: TODO | TO_DO | To Do | IN_PROGRESS | In Progress | IN_REVIEW | In Review | DONE | Done | BLOCKED | Blocked | CANCELLED | Cancelled")
    public ResponseObject<TaskResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseObject.<TaskResponse>builder()
                .data(taskService.updateStatus(id, status))
                .message("Task status updated successfully")
                .build();
    }

        @GetMapping("/status-options")
        @Operation(summary = "Get normalized task status options", description = "Danh sách status chuẩn hóa và input alias được chấp nhận")
        public ResponseObject<List<String>> getStatusOptions() {
                return ResponseObject.<List<String>>builder()
                                .data(taskService.getNormalizedStatusOptions())
                                .message("Task status options retrieved successfully")
                                .build();
        }

        @GetMapping("/curriculums")
        @Operation(summary = "Get curriculum IDs by accountId", description = "Trả về danh sách curriculumId duy nhất từ các task theo accountId")
        public ResponseObject<Set<UUID>> getCurriculumIdsByAccountId(@RequestParam UUID accountId) {
                return ResponseObject.<Set<UUID>>builder()
                                .data(taskService.getCurriculumIdsByAccountId(accountId))
                                .message("Curriculum IDs retrieved successfully")
                                .build();
        }
}
