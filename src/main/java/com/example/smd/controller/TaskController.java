package com.example.smd.controller;

import com.example.smd.dto.request.task.TaskCreateRequest;
import com.example.smd.dto.request.task.TaskUpdateRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.task.TaskListResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
        @Operation(summary = "Create a new task", description = "taskName là bắt buộc. sprintId bắt buộc cho tất cả người dùng.")
        public ResponseObject<TaskResponse> create(
                        @RequestBody @Valid TaskCreateRequest request,

                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<TaskResponse>builder()
                                .data(taskService.create(request, userId))
                                .message("Task created successfully")
                                .build();
        }

        @PostMapping("/batch/{sprintId}")
        @Operation(summary = "Create multiple tasks in a sprint", description = "Auto-create tasks from subjects of sprint's curriculum and department")
        public ResponseObject<Boolean> createBatch(
                        @PathVariable UUID sprintId,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<Boolean>builder()
                                .data(taskService.createBatch(sprintId, userId))
                                .message("Tasks created successfully")
                                .build();
        }

        @GetMapping
        @Operation(summary = "Search by TaskName and filter tasks with " +
                        "pagination", description = "Lấy danh sách task theo " +
                                        "các trường ở dưới ")
        public ResponseObject<PagedResponse<TaskListResponse>> getAll(
                        @RequestParam(required = false) String search,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) UUID sprintId,
                        @RequestParam(required = false) UUID accountId,
                        @RequestParam(required = false) UUID departmentId,
                        @RequestParam(required = false) UUID syllabusId,

                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "deadline") String sortBy,
                        @RequestParam(defaultValue = "asc") String direction) {

                Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();
                Pageable pageable = PageRequest.of(page, size, sort);

                return ResponseObject.<PagedResponse<TaskListResponse>>builder()
                                .data(PagedResponse.of(taskService.getAll(search, status, sprintId, accountId,
                                                departmentId, syllabusId, pageable)))
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
                        @RequestBody @Valid TaskUpdateRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                return ResponseObject.<TaskResponse>builder()
                                .data(taskService.update(id, request, userId))
                                .message("Task updated successfully")
                                .build();
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete task")
        public ResponseObject<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
                String userId = jwt.getClaimAsString("accountId");
                taskService.delete(id, userId);
                return ResponseObject.<Void>builder()
                                .message("Task deleted successfully")
                                .build();
        }

        @PatchMapping("/{id}/status")
        @Operation(summary = "Update Task Lifecycle Status (Cập nhật trạng thái nhiệm vụ)", description = "### 📋 Quy trình quản lý thực hiện nhiệm vụ (Task Workflow):\n"
                        +
                        "Trạng thái này dùng để theo dõi tiến độ công việc và hiệu suất của các thành viên trong dự án:\n\n"
                        +
                        "| Status | Ý nghĩa nghiệp vụ (Chi tiết) | Ràng buộc & Tác động |\n" +
                        "| :--- | :--- | :--- |\n" +
                        "| **DRAFT** | **Bản nháp:** Nhiệm vụ đang được người quản lý khởi tạo, chưa ban hành chính thức. | Người thực hiện (Assignee) chưa nhìn thấy nhiệm vụ. |\n"
                        +
                        "| **TO_DO** | **Chưa bắt đầu:** Nhiệm vụ đã được giao nhưng người thực hiện chưa xác nhận hoặc chưa bắt tay vào làm. | Có thể thay đổi người phụ trách (Assignee). |\n"
                        +
                        "| **IN_PROGRESS** | **Đang thực hiện:** Người nhận việc đã bắt đầu triển khai các đầu mục (Syllabus/CLO/Material). | Khóa chức năng xóa nhiệm vụ. |\n"
                        +
                        "| **DONE** | **Hoàn thành:** Các đầu mục công việc đã hoàn tất và được nộp lên hệ thống. | Tự động cập nhật thời gian hoàn thành thực tế. |\n"
                        +
                        "| **CANCELLED** | **Đã hủy:** Nhiệm vụ không còn cần thiết hoặc bị thay đổi do điều chỉnh chương trình đào tạo. | Giữ lại bản ghi để thống kê lịch sử. |\n\n")
        public ResponseObject<TaskResponse> updateStatus(
                        @PathVariable UUID id,
                        @RequestParam String status) {
                return ResponseObject.<TaskResponse>builder()
                                .data(taskService.updateStatus(id, status))
                                .message("Task status updated successfully")
                                .build();
        }

}
