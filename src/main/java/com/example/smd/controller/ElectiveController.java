package com.example.smd.controller;

import com.example.smd.dto.request.ElectiveRequest;
import com.example.smd.dto.response.ElectiveResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.ElectiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/electives")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Elective", description = "Endpoints for managing elective groups in the SMD system")
public class ElectiveController {
    ElectiveService electiveService;

    @PostMapping
    @Operation(summary = "Create a new elective group",
            description = "Requires electiveCode, electiveName and minCreditsRequired")
    @PreAuthorize("hasAuthority('ELECTIVE_CREATE')")
    public ResponseObject<ElectiveResponse> create(@RequestBody @Valid ElectiveRequest request) {
        return ResponseObject.<ElectiveResponse>builder()
                .status(1000)
                .data(electiveService.create(request))
                .message("Create elective group successfully")
                .build();
    }

    @GetMapping
    @Operation(summary = "Get list of elective groups",
            description = "Support searching by code/name and pagination (page, size)")
    public ResponseObject<Page<ElectiveResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<Page<ElectiveResponse>>builder()
                .status(1000)
                .data(electiveService.getAll(search, page, size))
                .message("Get all electives successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get elective group details by ID")
    public ResponseObject<ElectiveResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<ElectiveResponse>builder()
                .status(1000)
                .data(electiveService.getDetails(id))
                .message("Get elective detail successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update elective group information",
            description = "Update specific fields of an existing elective group")
    @PreAuthorize("hasAuthority('ELECTIVE_UPDATE')")
    public ResponseObject<ElectiveResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid ElectiveRequest request) {
        return ResponseObject.<ElectiveResponse>builder()
                .status(1000)
                .data(electiveService.update(id, request))
                .message("Update elective successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an elective group",
            description = "Delete by ID. System will check for related subjects before deletion")
    @PreAuthorize("hasAuthority('ELECTIVE_DELETE')")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        electiveService.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Delete elective successfully")
                .build();
    }
}

