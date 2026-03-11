package com.example.smd.controller;

import com.example.smd.dto.request.PLOsRequest;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.PLOsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "PLOs", description = "Program Learning Outcomes Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PLOsController {
    PLOsService ploService;

    @PostMapping
    @PreAuthorize("hasAuthority('PLOS_CREATE')")
    @Operation(summary = "Create a new PLO", description = "Create a PLO linked to a specific Major.")
    public ResponseObject<PLOsResponse> create(@RequestBody @Valid PLOsRequest request) {
        return ResponseObject.<PLOsResponse>builder()
                .status(1000)
                .data(ploService.createPlo(request))
                .message("PLO created successfully")
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLOS_UPDATE')")
    @Operation(summary = "Update PLO", description = "Update plo_code and description.")
    public ResponseObject<PLOsResponse> update(@PathVariable String id, @RequestBody @Valid PLOsRequest request) {
        return ResponseObject.<PLOsResponse>builder()
                .status(1000)
                .data(ploService.updatePlo(id, request))
                .message("PLO updated successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PLO Detail")
    public ResponseObject<PLOsResponse> getDetail(@PathVariable String id) {
        return ResponseObject.<PLOsResponse>builder()
                .status(1000)
                .data(ploService.getPloDetail(id))
                .message("Get PLO detail successfully")
                .build();
    }

    @GetMapping("/curriculum/{curriculumId}")
    @Operation(summary = "Get PLOs by Major ID")
    public ResponseObject<PagedResponse<PLOsResponse>> getByCurriculum(
            @PathVariable String curriculumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<PLOsResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(ploService.getPlosByCurriculum(curriculumId, page, size)))
                .message("Get PLOs by major successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLOS_DELETE')")
    @Operation(summary = "Delete PLO")
    public ResponseObject<Void> delete(@PathVariable String id) {
        ploService.deletePlo(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("PLO deleted successfully")
                .build();
    }
}
