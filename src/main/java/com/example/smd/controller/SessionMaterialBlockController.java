package com.example.smd.controller;

import com.example.smd.dto.request.session.SessionMaterialBlockBulkRequest;
import com.example.smd.dto.request.session.SessionMaterialBlockBulkListRequest;
import com.example.smd.dto.request.session.SessionMaterialBlockUpdateRequest;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.validate.SessionValidationResult;
import com.example.smd.dto.response.session.BulkSessionMaterialBlockResponse;
import com.example.smd.dto.response.session.SessionMaterialBlockDetailResponse;
import com.example.smd.services.SessionMaterialBlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/session-material-blocks")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Session Material Block", description = "Bulk configure session-material-block mappings")
@SecurityRequirement(name = "bearerAuth")
public class SessionMaterialBlockController {

    SessionMaterialBlockService sessionMaterialBlockService;

    @PostMapping("/bulk-configure")
    // @PreAuthorize("hasAuthority('SYLLABUS_UPDATE')")
    @Operation(summary = "Bulk configure session-material-block mappings", description = "Create/reuse session and map material-block pairs into session_material_block. Session and material are required.")
    public ResponseObject<BulkSessionMaterialBlockResponse> bulkConfigure(
            @Valid @RequestBody SessionMaterialBlockBulkRequest request) {
        BulkSessionMaterialBlockResponse response = sessionMaterialBlockService
                .bulkConfigureSessionMaterialBlocks(request);

        return ResponseObject.<BulkSessionMaterialBlockResponse>builder()
                .status(response.isSuccess() ? 1000 : 400)
                .data(response)
                .message(response.isSuccess() ? "Bulk session-material-block mapping completed successfully"
                        : "Bulk session-material-block mapping completed with validation issues")
                .build();
    }

    @PostMapping("/bulk-configure-list")
    @Operation(summary = "Bulk configure session-material-block mappings by session list", description = "Apply the same bulk-configure logic for each item in sessions list under one syllabusId")
    public ResponseObject<List<BulkSessionMaterialBlockResponse>> bulkConfigureByList(
            @Valid @RequestBody SessionMaterialBlockBulkListRequest request) {
        List<BulkSessionMaterialBlockResponse> responses = sessionMaterialBlockService
                .bulkConfigureSessionMaterialBlocksByList(request);

        return ResponseObject.<List<BulkSessionMaterialBlockResponse>>builder()
                .status(1000)
                .data(responses)
                .message("Bulk session-material-block mapping by list completed successfully")
                .build();
    }

    @PutMapping("/update")
    @Operation(summary = "Update session info and replace all session-material-block mappings", description = "Update session fields by sessionId, delete all related rows in session_material_block, then insert the new material-block mappings.")
    public ResponseObject<BulkSessionMaterialBlockResponse> updateSessionMaterialBlocks(
            @Valid @RequestBody SessionMaterialBlockUpdateRequest request) {
        BulkSessionMaterialBlockResponse response = sessionMaterialBlockService.updateSessionMaterialBlocks(request);

        return ResponseObject.<BulkSessionMaterialBlockResponse>builder()
                .status(response.isSuccess() ? 1000 : 400)
                .data(response)
                .message(response.isSuccess() ? "Update session-material-block mapping completed successfully"
                        : "Update session-material-block mapping completed with validation issues")
                .build();
    }

    @GetMapping
    @Operation(summary = "Get session-material-block list by syllabusId with pagination")
    public ResponseObject<PagedResponse<SessionMaterialBlockDetailResponse>> getBySyllabus(
            @RequestParam UUID syllabusId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseObject.<PagedResponse<SessionMaterialBlockDetailResponse>>builder()
                .status(1000)
                .data(sessionMaterialBlockService.getBySyllabus(syllabusId, page, size))
                .message("Get session-material-block list successfully")
                .build();
    }

    @GetMapping("/detail")
    @Operation(summary = "Get session-material-block detail by sessionId")
    public ResponseObject<SessionMaterialBlockDetailResponse> getSessionDetail(
            @RequestParam UUID sessionId) {
        return ResponseObject.<SessionMaterialBlockDetailResponse>builder()
                .status(1000)
                .data(sessionMaterialBlockService.getSessionDetail(sessionId))
                .message("Get session-material-block detail successfully")
                .build();
    }

    @PostMapping("/subject/{subjectId}/validate")
    @Operation(summary = "Get session-material-block detail by sessionId")
    public ResponseObject<SessionValidationResult> validateSession(
            @RequestBody List<SessionMaterialBlockBulkRequest> inputs,
            @PathVariable("subjectId") UUID subjectId) {
        return ResponseObject.<SessionValidationResult>builder()
                .status(1000)
                .data(sessionMaterialBlockService.validate(inputs, subjectId))
                .message("Validate session-material-block successfully")
                .build();
    }
}
