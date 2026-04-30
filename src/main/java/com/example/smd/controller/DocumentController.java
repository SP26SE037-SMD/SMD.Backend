package com.example.smd.controller;

import com.example.smd.dto.request.DocumentRequest;
import com.example.smd.dto.response.DepartmentResponse;
import com.example.smd.dto.response.DocumentResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.entities.Document;
import com.example.smd.services.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Document", description = "Endpoints for managing Document")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {
    @Autowired
    private DocumentService service;
    @Autowired
    private DocumentService documentService;

    @GetMapping("/major/{majorId}")
    @Operation(summary = "Get all documents with pagination and search")
    public ResponseObject<List<DocumentResponse>> getAll(
            @RequestParam UUID majorId,
            @Parameter(description = "Trạng thái của tài liệu. Các giá trị hợp lệ: ACTIVE, DELETED",
                    example = "ACTIVE")
            @RequestParam(required = false) String status) {
        return ResponseObject.<List<DocumentResponse>>builder()
                .status(1000)
                .data(service.getAll(majorId, status))
                .message("Get all document successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document details by ID")
    public ResponseObject<DocumentResponse> getById(@PathVariable UUID id) {
        return ResponseObject.<DocumentResponse>builder()
                .status(1000)
                .data(documentService.getById(id))
                .message("Get document detail successfully")
                .build();
    }

    @PostMapping
    @Operation(summary = "Create a new document")
    public ResponseObject<DocumentResponse> create(@RequestBody DocumentRequest request) {
        return ResponseObject.<DocumentResponse>builder()
                .status(1000)
                .data(service.create(request))
                .message("Create document successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document information")
    public ResponseObject<DocumentResponse> update(@PathVariable UUID id, @RequestBody DocumentRequest request) {
        return ResponseObject.<DocumentResponse>builder()
                .status(1000)
                .data(service.update(id, request))
                .message("Update document successfully")
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document")
    public ResponseObject<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Delete document successfully")
                .build();
    }
}
