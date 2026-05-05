package com.example.smd.controller;

import com.example.smd.dto.request.taskV2.TaskV2CreateRequest;
import com.example.smd.dto.request.taskV2.TaskV2UpdateRequest;
import com.example.smd.dto.response.TaskV2Response;
import com.example.smd.services.TaskV2Service;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks-v2")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "TaskV2", description = "Endpoints for managing tasks " +
        "within sprints")
@SecurityRequirement(name = "bearerAuth")

public class TaskV2Controller {

    private final TaskV2Service taskV2Service;

    @GetMapping
    public ResponseEntity<com.example.smd.dto.response.PagedResponse<TaskV2Response>> getAllTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID sprintId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID assignTo,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TaskV2Response> responsePage = taskV2Service.getAllTasks(
                search, status, sprintId, type, action, assignTo, createdBy, targetId, pageable
        );
        return ResponseEntity.ok(com.example.smd.dto.response.PagedResponse.of(responsePage));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskV2Response> getTaskById(@PathVariable UUID taskId) {
        return ResponseEntity.ok(taskV2Service.getTaskById(taskId));
    }

    @PostMapping
    public ResponseEntity<TaskV2Response> createTask(
            @RequestBody TaskV2CreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseEntity.ok(taskV2Service.createTask(request, userId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskV2Response> updateTask(@PathVariable UUID taskId, @RequestBody TaskV2UpdateRequest request) {
        return ResponseEntity.ok(taskV2Service.updateTask(taskId, request));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<TaskV2Response> updateTaskStatus(@PathVariable UUID taskId, @RequestParam String request) {
        return ResponseEntity.ok(taskV2Service.updateTaskStatus(taskId, request));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        taskV2Service.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
