package com.example.smd.controller;

import com.example.smd.dto.request.reviewtask.ReviewTaskAcceptanceRequest;
import com.example.smd.dto.request.reviewtask.ReviewTaskCreateHoCFDC;
import com.example.smd.dto.request.reviewtask.ReviewTaskCreateRequest;
import com.example.smd.dto.request.reviewtask.ReviewTaskRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.reviewtask.ReviewTaskResponse;
import com.example.smd.services.ReviewTaskService;
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
@RequestMapping("/api/review-tasks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Review Task", description = "Endpoints for managing review tasks")
@SecurityRequirement(name = "bearerAuth")
public class ReviewTaskController {

    ReviewTaskService reviewTaskService;

    @PostMapping
    @Operation(summary = "Create a new review task")
    public ResponseObject<ReviewTaskResponse> create(
            @RequestBody @Valid ReviewTaskCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String reviewerId = jwt.getClaimAsString("accountId");

        return ResponseObject.<ReviewTaskResponse>builder()
                .data(reviewTaskService.create(request, reviewerId))
                .message("Review task created successfully")
                .build();
    }

    @PostMapping("/hocfdc")
    @Operation(summary = "Create a new review task")
    public ResponseObject<ReviewTaskResponse> createHoCFDC(
            @RequestBody @Valid ReviewTaskCreateHoCFDC request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String reviewerId = jwt.getClaimAsString("accountId");

        return ResponseObject.<ReviewTaskResponse>builder()
                .data(reviewTaskService.createByHoCFDC(request, reviewerId))
                .message("Review task created successfully for HoCFDC")
                .build();
    }

    @GetMapping
    @Operation(summary = "Search by TitleTask and filter review tasks with " +
            "pagination")
    public ResponseObject<PagedResponse<ReviewTaskResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) UUID reviewerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reviewDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<ReviewTaskResponse>>builder()
                .data(PagedResponse.of(reviewTaskService.getAll(search, status, taskId, reviewerId, pageable)))
                .message("Review tasks retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review task detail by ID")
    public ResponseObject<ReviewTaskResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<ReviewTaskResponse>builder()
                .data(reviewTaskService.getDetail(id))
                .message("Review task detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review task")
    public ResponseObject<ReviewTaskResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid ReviewTaskRequest request
    ) {
        return ResponseObject.<ReviewTaskResponse>builder()
                .data(reviewTaskService.update(id, request))
                .message("Review task updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review task")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        reviewTaskService.delete(id);

        return ResponseObject.<Void>builder()
                .message("Review task deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update review task status")
    public ResponseObject<ReviewTaskResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        return ResponseObject.<ReviewTaskResponse>builder()
                .data(reviewTaskService.updateStatus(id, status))
                .message("Review task status updated successfully")
                .build();
    }

    @PatchMapping("/{id}/acceptance")
    @Operation(summary = "Update review task acceptance status - triggers cascading status updates")
    public ResponseObject<ReviewTaskResponse> updateAcceptance(
            @PathVariable UUID id,
            @RequestBody @Valid ReviewTaskAcceptanceRequest request
    ) {
        return ResponseObject.<ReviewTaskResponse>builder()
                .data(reviewTaskService.updateAcceptance(id, request))
                .message("Review task acceptance updated successfully with cascading status changes")
                .build();
    }
}
