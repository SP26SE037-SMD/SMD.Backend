package com.example.smd.controller;

import com.example.smd.dto.request.RegulationRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.RegulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Regulation", description = "Regulation Management APIs")
@RestController
@RequestMapping("/api/regulations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RegulationController {

    private final RegulationService regulationService;

    @GetMapping
    @Operation(summary = "Get all regulations with pagination")
    public ResponseObject<PagedResponse<RegulationResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<RegulationResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(regulationService.getAll(search, page, size, sort)))
                .message("Get regulations successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get regulation by ID")
    public ResponseObject<RegulationResponse> getById(@PathVariable UUID id) {
        return ResponseObject.<RegulationResponse>builder()
                .status(1000)
                .data(regulationService.getById(id))
                .message("Get regulation successfully")
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    @Operation(summary = "Create regulation")
    public ResponseObject<RegulationResponse> create(@Valid @RequestBody RegulationRequest request) {
        return ResponseObject.<RegulationResponse>builder()
                .status(1000)
                .data(regulationService.create(request))
                .message("Create regulation successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    @Operation(summary = "Update regulation")
    public ResponseObject<RegulationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody RegulationRequest request
    ) {
        return ResponseObject.<RegulationResponse>builder()
                .status(1000)
                .data(regulationService.update(id, request))
                .message("Update regulation successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CURRICULUM_UPDATE')")
    @Operation(summary = "Delete regulation")
    public ResponseObject<Boolean> delete(@PathVariable UUID id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(regulationService.delete(id))
                .message("Delete regulation successfully")
                .build();
    }
}
