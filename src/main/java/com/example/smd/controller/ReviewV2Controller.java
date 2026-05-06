package com.example.smd.controller;

import com.example.smd.dto.request.reviewV2.ReviewV2CreateRequest;
import com.example.smd.dto.request.reviewV2.ReviewV2UpdateRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.ReviewV2Response;
import com.example.smd.services.ReviewV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews-v2")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "ReviewV2", description = "Endpoints for managing task reviews (V2)")
@SecurityRequirement(name = "bearerAuth")
public class ReviewV2Controller {

    ReviewV2Service reviewV2Service;

    // ----------------------------------------------------------------
    // GET ALL (paged, filterable by taskId & isAccepted)
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(summary = "Get all reviews with pagination",
               description = "Filter by taskId and/or isAccepted. Sorted by createdAt desc by default.")
    public ResponseObject<PagedResponse<ReviewV2Response>> getAll(
            @RequestParam(required = false) UUID taskId,
            @RequestParam(required = false) Boolean isAccepted,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReviewV2Response> resultPage = reviewV2Service.getAll(taskId, isAccepted, pageable);

        return ResponseObject.<PagedResponse<ReviewV2Response>>builder()
                .data(PagedResponse.of(resultPage))
                .message("Reviews retrieved successfully")
                .build();
    }

    // ----------------------------------------------------------------
    // GET ALL BY TASK (flat list, no pagination)
    // ----------------------------------------------------------------
    @GetMapping("/by-task/{taskId}")
    @Operation(summary = "Get all reviews of a specific task",
               description = "Returns all review records linked to the given taskId.")
    public ResponseObject<List<ReviewV2Response>> getAllByTask(@PathVariable UUID taskId) {
        return ResponseObject.<List<ReviewV2Response>>builder()
                .data(reviewV2Service.getAllByTask(taskId))
                .message("Reviews for task retrieved successfully")
                .build();
    }

    // ----------------------------------------------------------------
    // GET BY ID
    // ----------------------------------------------------------------
    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review detail by ID")
    public ResponseObject<ReviewV2Response> getById(@PathVariable UUID reviewId) {
        return ResponseObject.<ReviewV2Response>builder()
                .data(reviewV2Service.getById(reviewId))
                .message("Review retrieved successfully")
                .build();
    }

    // ----------------------------------------------------------------
    // CREATE
    // ----------------------------------------------------------------
    @PostMapping
    @Operation(summary = "Create a new review for a TaskV2",
               description = "Requires taskId. isAccepted can be null (pending), true (accepted), false (rejected).")
    public ResponseObject<ReviewV2Response> create(@RequestBody ReviewV2CreateRequest request) {
        return ResponseObject.<ReviewV2Response>builder()
                .data(reviewV2Service.create(request))
                .message("Review created successfully")
                .build();
    }

    // ----------------------------------------------------------------
    // UPDATE
    // ----------------------------------------------------------------
    @PutMapping("/{reviewId}")
    @Operation(summary = "Update an existing review",
               description = "Only non-null fields in the request body will be applied.")
    public ResponseObject<ReviewV2Response> update(
            @PathVariable UUID reviewId,
            @RequestBody ReviewV2UpdateRequest request
    ) {
        return ResponseObject.<ReviewV2Response>builder()
                .data(reviewV2Service.update(reviewId, request))
                .message("Review updated successfully")
                .build();
    }

    // ----------------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------------
    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete a review by ID")
    public ResponseObject<Void> delete(@PathVariable UUID reviewId) {
        reviewV2Service.delete(reviewId);
        return ResponseObject.<Void>builder()
                .message("Review deleted successfully")
                .build();
    }
}
