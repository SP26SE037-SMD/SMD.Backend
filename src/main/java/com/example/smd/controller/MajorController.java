package com.example.smd.controller;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.major.ImportMajorResponse;
import com.example.smd.entities.Major;
import com.example.smd.services.MajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Major", description = "Major Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class MajorController {
    MajorService majorService;

    // API lấy danh sách chuyên ngành có phân trang và bộ lọc
    @GetMapping
    @Operation(
            summary = "Get majors with pagination and filters",
            description = "Retrieve a paginated list of majors. Filter by 'major_code', 'major_name', and lifecycle 'status'."
    )
    public ResponseObject<PagedResponse<MajorResponse>> getAllMajors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "all") String searchBy,
            @Parameter(
                    description = "Filter majors by their current lifecycle status. Valid values are:\n" +
                            "| Status | Description |\n" +
                            "| :--- | :--- |\n" +
                            "| **DRAFT** | **Bản nháp:** Major is under initial definition. Hidden from public and cannot have Curricula assigned. |\n" +
                            "| **INTERNAL_REVIEW** | **Kiểm tra nội bộ:** Content is shared with the Academic Board for auditing and feedback before launch. |\n" +
                            "| **PUBLISHED** | **Công bố:** Major is officially active. Students can enroll, and it is linked to active Curricula. |\n" +
                            "| **ARCHIVED** | **Lưu trữ:** No longer accepting new enrollments. Kept as Read-only for historical transcript verification. |"
            )
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean updatedYesterday,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "majorCode,asc") String[] sort,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getClaimAsString("accountId");
        Page<MajorResponse> majors;
        if (Boolean.TRUE.equals(updatedYesterday)) {
            majors = majorService.getMajorsUpdatedInLast24Hours(status, page, size);
        } else {
            // Gọi logic getAll cũ của bạn
            majors = majorService.getAllMajors(userId, search, "code", status, page, size, new String[]{"majorCode", "asc"});
        }

        return ResponseObject.<PagedResponse<MajorResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(majors))
                .message("Get all majors successfully")
                .build();
    }

    // API tạo chuyên ngành mới
    @PostMapping
    @PreAuthorize("hasAuthority('MAJOR_CREATE')")
    @Operation(
            summary = "Create a new major",
            description = "Add a new major to the system. The major code must be unique."
    )
    public ResponseObject<MajorResponse> create(@RequestBody @Valid MajorRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<MajorResponse>builder()
                .status(1000)
                .data(majorService.createMajor(request, userId))
                .message("Major created successfully")
                .build();
    }

    // API cập nhật chuyên ngành
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MAJOR_UPDATE')")
    @Operation(
            summary = "Update an existing major",
            description = "Update the details of an existing major based on its unique ID (UUID)."
    )
    public ResponseObject<MajorResponse> update(@PathVariable UUID id, @RequestBody @Valid MajorRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<MajorResponse>builder()
                .status(1000)
                .data(majorService.updateMajor(id, request, userId))
                .message("Major updated successfully")
                .build();
    }

    // API xóa chuyên ngành (Xóa mềm)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MAJOR_DELETE')")
    @Operation(
            summary = "Delete a major (Soft Delete)",
            description = "Mark a major as deleted. The record remains in the database but will be excluded from general listings."
    )
    public ResponseObject<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        majorService.deleteMajor(id, userId);
        return ResponseObject.<Void>builder()
                .status(1000)
                .message("Major deleted successfully")
                .build();
    }

    // API lấy chi tiết chuyên ngành theo mã chuyên ngành
    @GetMapping("/code/{majorCode}")
    @Operation(
            summary = "Get major details by major code",
            description = "Retrieve full information of a major using its unique code (e.g., SE, AI, CS)."
    )
    public ResponseObject<MajorResponse> getMajorDetail(
            @PathVariable String majorCode,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<MajorResponse>builder()
                .status(1000)
                .data(majorService.getMajorDetail(majorCode, userId))
                .message("Get major details successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Major details by ID",
            description = "Retrieves comprehensive information about a specific Major using its unique UUID."
    )
    public ResponseObject<MajorResponse> getDetail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");

        return ResponseObject.<MajorResponse>builder()
                .status(1000)
                .data(majorService.getMajorById(id, userId))
                .message("Major details retrieved successfully")
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('MAJOR_UPDATE_STATUS')")
    @Operation(
            summary = "Update Major status",
            description = "### Lifecycle Management for Academic Majors:\n" +
                    "Select one of the following statuses to coordinate the availability and enrollment of the major:\n\n" +
                    "| Status | Detailed Business Description |\n" +
                    "| :--- | :--- |\n" +
                    "| **DRAFT** | **Draft:** The major is under initial definition. It is hidden from public view and cannot have Curricula assigned yet. |\n" +
                    "| **INTERNAL_REVIEW** | **Internal Review:** Content and learning goals are shared with the Academic Board for internal auditing and feedback before official launch. |\n" +
                    "| **PUBLISHED** | **Published:** The major is officially active. Students can enroll, and it can be linked to active Curricula and Subjects. |\n" +
                    "| **ARCHIVED** | **Archived:** The major is no longer accepting new enrollments. Data is kept as Read-only for historical transcript and degree verification. |"
    )
    public ResponseObject<MajorResponse> changeStatus(
            @PathVariable String id,
            @RequestParam String newStatus
    ) {
        return ResponseObject.<MajorResponse>builder()
                .status(1000)
                .data(majorService.updateStatus(id, newStatus))
                .message("Cập nhật trạng thái Major thành công")
                .build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasAuthority('MAJOR_CREATE')")
    @Operation(
            summary = "Import Majors from Excel",
            description = "Import Major records (with POs) from an Excel file.\n\n" +
                    "**Sheet name:** `Major`\n\n" +
                    "**Column headers (row 1):**\n" +
                    "| Column | Required | Description |\n" +
                    "| :--- | :--- | :--- |\n" +
                    "| **Major Code** | Yes | Unique code for the major (e.g. SE, AI) |\n" +
                    "| **Name** | Yes | Full name of the major |\n" +
                    "| **Description** | No | Description of the major |\n" +
                    "| **PO Code** | Yes | Unique PO code (e.g. PO1, PO2). Each row represents one PO. |\n" +
                    "| **PO Description** | No | Description of the PO |\n\n" +
                    "**Notes:**\n" +
                    "- One Major can have multiple POs — repeat the Major Code on each row.\n" +
                    "- If a Major Code already exists in the system, all rows for that Major will be rejected.\n" +
                    "- PO Code must be globally unique across all Majors."
    )
    public ResponseObject<ImportMajorResponse> importMajors(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseObject.<ImportMajorResponse>builder()
                .status(1000)
                .data(majorService.importMajors(file))
                .message("Import majors completed")
                .build();
    }
}
