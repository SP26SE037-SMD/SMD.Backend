package com.example.smd.controller;

import com.example.smd.dto.request.sprint.SprintRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.sprint.SprintResponse;
import com.example.smd.services.SprintService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Sprint", description = "Endpoints for managing agile sprints")
@SecurityRequirement(name = "bearerAuth")
public class SprintController {

    SprintService sprintService;

    @PostMapping
    @Operation(summary = "Create a new sprint")
    public ResponseObject<SprintResponse> create(@RequestBody @Valid SprintRequest request) {
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.create(request))
                .message("Sprint created successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Advanced search sprints with pagination")
    public ResponseObject<PagedResponse<SprintResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<SprintResponse>>builder()
                .data(PagedResponse.of(sprintService.getAll(search, status, pageable)))
                .message("Sprints retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sprint detail by ID")
    public ResponseObject<SprintResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.getDetail(id))
                .message("Sprint detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update sprint information")
    public ResponseObject<SprintResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid SprintRequest request) {
        return ResponseObject.<SprintResponse>builder()
                .data(sprintService.update(id, request))
                .message("Sprint updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete sprint")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        sprintService.delete(id);
        return ResponseObject.<Void>builder()
                .message("Sprint deleted successfully")
                .build();
    }
}
