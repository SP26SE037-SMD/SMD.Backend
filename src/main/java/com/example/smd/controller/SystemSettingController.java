package com.example.smd.controller;

import com.example.smd.dto.request.SystemSettingRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.SystemSettingResponse;
import com.example.smd.services.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "System Setting", description = "Endpoints for managing system settings")
@SecurityRequirement(name = "bearerAuth")
public class SystemSettingController {
    SystemSettingService systemSettingService;

    @GetMapping
    @Operation(summary = "Get all settings with pagination and search")
    public ResponseObject<PagedResponse<SystemSettingResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SystemSettingResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(systemSettingService.getAll(search, page, size)))
                .message("Get all settings successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get setting details by ID")
    public ResponseObject<SystemSettingResponse> getDetail(@PathVariable UUID id) {
        return ResponseObject.<SystemSettingResponse>builder()
                .status(1000)
                .data(systemSettingService.getDetail(id))
                .message("Get setting detail successfully")
                .build();
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get setting details by code")
    public ResponseObject<SystemSettingResponse> getDetailByCode(@PathVariable String code) {
        return ResponseObject.<SystemSettingResponse>builder()
                .status(1000)
                .data(systemSettingService.getDetailByCode(code))
                .message("Get setting detail successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update setting information (only value)")
    public ResponseObject<SystemSettingResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid SystemSettingRequest request) {
        return ResponseObject.<SystemSettingResponse>builder()
                .status(1000)
                .data(systemSettingService.update(id, request))
                .message("Update setting successfully")
                .build();
    }
}
