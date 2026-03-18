package com.example.smd.controller;

import com.example.smd.dto.request.ComboRequest;
import com.example.smd.dto.response.ComboResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.combo.ImportComboResponse;
import com.example.smd.services.ComboService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Combo", description = "Combo Management APIs")
@RestController
@RequestMapping("/api/combos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ComboController {

    private final ComboService comboService;

    // API lấy danh sách combo có phân trang và tìm kiếm
    @GetMapping
    @Operation(
        summary = "Get all combos with pagination and search (combo code and combo name)",
        description = "Search by combo code or combo name. " +
                "Sort format: field 1 là tên trường (comboCode, comboName, type), " +
                "field 2 là hướng sắp xếp (asc hoặc desc). " +
                "Ví dụ: sort=comboCode,asc"
    )
    public ResponseObject<PagedResponse<ComboResponse>> getAllCombos(
            @RequestParam(required = false, name = "search")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Search keyword for combo code or combo name"
            ) String search,

            @RequestParam(required = false, name = "searchBy")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Search type: 'code' (search by combo code), 'name' (search by combo name)"
            ) String searchBy,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "comboCode,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<ComboResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(comboService.getAllCombos(search, searchBy, page, size, sort)))
                .message("Get all combos successfully")
                .build();
    }

    // API lấy chi tiết combo theo ID
    @GetMapping("/{id}")
    @Operation(summary = "Get combo by ID")
    public ResponseObject<ComboResponse> getComboById(@PathVariable String id) {
        return ResponseObject.<ComboResponse>builder()
                .status(1000)
                .data(comboService.getComboById(id))
                .message("Get combo successfully")
                .build();
    }

    // API tạo combo mới
    @PostMapping
    @PreAuthorize("hasAuthority('COMBO_CREATE')")
    @Operation(summary = "Create new combo")
    public ResponseObject<ComboResponse> createCombo(@Valid @RequestBody ComboRequest request) {
        return ResponseObject.<ComboResponse>builder()
                .status(1000)
                .data(comboService.createCombo(request))
                .message("Create combo successfully")
                .build();
    }

    // API cập nhật combo theo ID
    @PreAuthorize("hasAuthority('COMBO_UPDATE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update combo by ID")
    public ResponseObject<ComboResponse> updateCombo(
            @PathVariable String id,
            @Valid @RequestBody ComboRequest request) {
        return ResponseObject.<ComboResponse>builder()
                .status(1000)
                .data(comboService.updateCombo(id, request))
                .message("Update combo successfully")
                .build();
    }

    @PostMapping(value = "/import", consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('COMBO_CREATE')")
    @Operation(summary = "Import combos from Excel")
    public ResponseObject<ImportComboResponse> importCombos(@RequestParam("file") MultipartFile file) {
        return ResponseObject.<ImportComboResponse>builder()
                .status(1000)
                .data(comboService.importCombos(file))
                .message("Import combos successfully")
                .build();
    }
}
