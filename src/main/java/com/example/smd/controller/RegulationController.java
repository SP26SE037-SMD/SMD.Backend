package com.example.smd.controller;

import com.example.smd.dto.request.RegulationRequest;
import com.example.smd.dto.response.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.services.GeminiService;
import com.example.smd.services.RegulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Regulation", description = "Regulation Management APIs")
@RestController
@RequestMapping("/api/regulations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RegulationController {

    private final RegulationService regulationService;

    @GetMapping("/major/{majorId}")
    @Operation(summary = "Get all regulations with pagination")
    public ResponseObject<PagedResponse<RegulationResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            @PathVariable UUID majorId
    ) {
        return ResponseObject.<PagedResponse<RegulationResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(regulationService.getAll(search, page, size, sort, majorId)))
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

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MajorResponse> extractMasterDataFromPdf(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        // 1. Validate file đầu vào (Basic)
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED, "File tải lên không được để trống.");
        }

        // 2. Gọi Service xử lý luồng AI (Upload -> Prompt -> Parse JSON)
        var response = regulationService.importMajorAndAddRegulation(file, userId);

        // 3. Trả về kết quả cho Frontend
        return ResponseEntity.ok(response);
    }
}
