package com.example.smd.controller;

import com.example.smd.dto.request.request.RequestRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.request.RequestResponse;
import com.example.smd.services.RequestService;
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
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Request", description = "Endpoints for managing requests")
@SecurityRequirement(name = "bearerAuth")
public class RequestController {

    RequestService requestService;

    @PostMapping
    @Operation(summary = "Create a new request")
    public ResponseObject<RequestResponse> create(@RequestBody @Valid RequestRequest request) {
        return ResponseObject.<RequestResponse>builder()
                .data(requestService.create(request))
                .message("Request created successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all requests with pagination and filtering")
    public ResponseObject<PagedResponse<RequestResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID curriculumId,
            @RequestParam(required = false) UUID majorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseObject.<PagedResponse<RequestResponse>>builder()
                .data(PagedResponse.of(requestService.getAll(search, status, curriculumId, majorId, pageable)))
                .message("Requests retrieved successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request detail by ID")
    public ResponseObject<RequestResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<RequestResponse>builder()
                .data(requestService.getDetail(id))
                .message("Request detail retrieved successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update request information")
    public ResponseObject<RequestResponse> update(@PathVariable UUID id, @RequestBody @Valid RequestRequest request) {
        return ResponseObject.<RequestResponse>builder()
                .data(requestService.update(id, request))
                .message("Request updated successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete request")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        requestService.delete(id);
        return ResponseObject.<Void>builder()
                .message("Request deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update request status")
    public ResponseObject<RequestResponse> updateStatus(@PathVariable UUID id, @RequestParam String status, @RequestParam String comment) {
        return ResponseObject.<RequestResponse>builder()
                .data(requestService.updateStatus(id, status, comment))
                .message("Request status updated successfully")
                .build();
    }
}
