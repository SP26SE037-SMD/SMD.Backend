package com.example.smd.controller;


import com.example.smd.dto.request.PoPloMappingRequest;
import com.example.smd.dto.response.PoPloMappingResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.PoPloMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/po-plo-mappings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "PO-PLO Mappings", description = "Matrix mapping between Program Outcomes (PO) and Program Learning Outcomes (PLO)")
@SecurityRequirement(name = "bearerAuth")
public class PoPloMappingController {
    PoPloMappingService service;

    @PostMapping
    @PreAuthorize("hasAuthority('MAPPING_CREATE')")
    @Operation(
            summary = "Create PO-PLO Mapping",
            description = "Establish a connection between a Program Outcome and a Program Learning Outcome."
    )
    public ResponseObject<PoPloMappingResponse> create(@RequestBody @Valid PoPloMappingRequest request) {
        return ResponseObject.<PoPloMappingResponse>builder()
                .status(1000)
                .data(service.createMapping(request))
                .message("Mapping PO to PLO created successfully")
                .build();
    }

    @GetMapping("/curriculum/{curriculumId}")
    @Operation(
            summary = "Get all mappings for a specific Curriculum",
            description = "Retrieve the matrix of PO-PLO mappings for all PLOs within a given curriculum."
    )
    public ResponseObject<List<PoPloMappingResponse>> getByCurriculum(@PathVariable String curriculumId) {
        return ResponseObject.<List<PoPloMappingResponse>>builder()
                .status(1000)
                .data(service.getByCurriculum(curriculumId))
                .message("Get curriculum PO-PLO matrix successfully")
                .build();
    }

    @GetMapping("/po/{poId}")
    @Operation(summary = "Get all PLOs mapped to a specific PO")
    public ResponseObject<List<PoPloMappingResponse>> getByPo(@PathVariable String poId) {
        return ResponseObject.<List<PoPloMappingResponse>>builder()
                .status(1000)
                .data(service.getByPo(poId))
                .message("Get PLOs mapped to PO successfully")
                .build();
    }

    @GetMapping("/plo/{ploId}")
    @Operation(summary = "Get all POs mapped to a specific PLO")
    public ResponseObject<List<PoPloMappingResponse>> getByPlo(@PathVariable String ploId) {
        return ResponseObject.<List<PoPloMappingResponse>>builder()
                .status(1000)
                .data(service.getByPlo(ploId))
                .message("Get POs mapped to PLO successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MAPPING_DELETE')")
    @Operation(summary = "Remove a PO-PLO Mapping")
    public ResponseObject<Void> delete(@PathVariable String id) {
        service.deleteMapping(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("PO-PLO Mapping deleted successfully")
                .build();
    }
}