package com.example.smd.controller;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.account.AccountResponse;
import com.example.smd.dto.response.account.AvailableAccountResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.account.ImportResult;
import com.example.smd.entities.Account;
import com.example.smd.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Tag(name = "Account", description = "Account Management APIs")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")

public class AccountController {

    private final AccountService accountService;

    // API lấy danh sách tài khoản có phân trang và tìm kiếm
    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_ALL')")
    @Operation(
        summary = "Get all accounts with pagination and search " +
                "(role and full name)",
        description = "Search by role name, full name. " +
                "Sort format: field 1 là tên trường (createdAt, fullName), " +
                "field 2 là hướng sắp xếp (asc hoặc desc). " +
                "Ví dụ: sort=createdAt,desc"
    )
    public ResponseObject<PagedResponse<AccountResponse>> getAllAccounts(
            @RequestParam(required = false, name = "search")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Search keyword for role name or full name"
            ) String search,

            @RequestParam(required = false, defaultValue = "role",
                    name = "searchBy")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Search type: 'role' (search by role name), 'name' (search by full name)"
            ) String searchBy,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<PagedResponse<AccountResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(accountService.getAllAccounts(search, searchBy, page, size, sort, userId)))
                .message("Get all accounts successfully")
                .build();
    }

    // API tìm kiếm account theo khoảng thời gian createdAt
    @GetMapping("/by-date-range")
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_ALL')")
    @Operation(
        summary = "Search accounts by creation date range",
        description = "Search accounts created between fromDate and toDate. " +
                "Date format: yyyy-MM-dd (e.g., '2024-01-01'). " +
                "Both parameters are optional. If only fromDate is provided, returns accounts created from that date onwards. " +
                "If only toDate is provided, returns accounts created up to that date."
    )
    public ResponseObject<PagedResponse<AccountResponse>> getAccountsByDateRange(
            @RequestParam(required = false, name = "fromDate")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Start date (Format: yyyy-MM-dd)"
            ) String fromDateStr,

            @RequestParam(required = false, name = "toDate")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "End date (Format: yyyy-MM-dd)"
            ) String toDateStr,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        // Parse date strings to Instant
        java.time.Instant fromDate = null;
        java.time.Instant toDate = null;

        try {
            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                // Parse date và set time về đầu ngày (00:00:00)
                java.time.LocalDate localDate = java.time.LocalDate.parse(fromDateStr);
                fromDate = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            }
            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                // Parse date và set time về cuối ngày (23:59:59.999)
                java.time.LocalDate localDate = java.time.LocalDate.parse(toDateStr);
                toDate = localDate.atTime(23, 59, 59, 999_999_999)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant();
            }
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseObject.<PagedResponse<AccountResponse>>builder()
                    .status(400)
                    .message("Invalid date format. Please use yyyy-MM-dd format (e.g., 2024-01-01)")
                    .build();
        }

        return ResponseObject.<PagedResponse<AccountResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(accountService.getAccountsByDateRange(fromDate, toDate, page, size, sort, userId)))
                .message("Get accounts by date range successfully")
                .build();
    }

    // API lấy chi tiết tài khoản theo ID
    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseObject<AccountResponse> getAccountById(@PathVariable String id) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.getAccountById(id))
                .message("Get account successfully")
                .build();
    }

    // API tạo tài khoản mới
    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @Operation(summary = "Create new account")
    public ResponseObject<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.createAccount(request, userId))
                .message("Create account successfully")
                .build();
    }

    // API cập nhật tài khoản theo ID
    @PutMapping
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @Operation(summary = "Update account by ID")
    public ResponseObject<AccountResponse> updateInformationAccount(
            @RequestParam String id,
            @Valid @RequestBody AccountUpdateRequest request,
            @RequestParam Boolean status
    ) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.updateAccount(id, status,
                        request))
                .message("Update account successfully")
                .build();
    }


    // API xóa tài khoản theo ID
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @Operation(summary = "Update status account by ID")
    public ResponseObject<Boolean> deleteAccount(@PathVariable String id, @RequestParam Boolean status) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(accountService.isActiveAccount(id, status))
                .message("Update account successfully")
                .build();
    }

    @PutMapping("/Department/{id}")
    @PreAuthorize("hasAuthority('ACCOUNT_UPDATE')")
    @Operation(summary = "Update Department account by ID")
    public ResponseObject<Boolean> changeDepartment(@PathVariable String id, @RequestParam String departmentCode, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("accountId");
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(accountService.changeDepartment(id, departmentCode, userId))
                .message("Update account successfully")
                .build();
    }

    @PostMapping(value = "/import", consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseObject<ImportResult> importAccounts(
            @RequestParam("file") MultipartFile file,
            @RequestParam String role
    ) {

        return ResponseObject.<ImportResult>builder()
                .status(1000)
                .data(accountService.importAccounts(file, role))
                .message("Update account successfully")
                .build();
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportAccounts() throws Exception {
        ByteArrayInputStream excel = accountService.exportAccounts();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=accounts.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(excel));
    }

    @GetMapping("/department/{departmentId}")
    @Operation(
            summary = "Get accounts by department",
            description = "Returns a list of all accounts belonging to a specific department ID, including their roles."
    )
    public ResponseObject<List<AccountResponse>> getByDepartment(@PathVariable UUID departmentId) {
        return ResponseObject.<List<AccountResponse>>builder()
                .status(1000)
                .data(accountService.getAccountsByDepartment(departmentId))
                .message("Accounts retrieved successfully for department: " + departmentId)
                .build();
    }

    @GetMapping("/department/available-account-ids")
    @Operation(
            summary = "Get available accounts in department that " +
                    "don't have tasks for a specific syllabus")
    public ResponseObject<List<AvailableAccountResponse>> getAvailableAccountIdsInMyDepartmentBySyllabus(
            @RequestParam UUID syllabusId,
            @AuthenticationPrincipal Jwt jwt) {
        String currentAccountId = jwt.getClaimAsString("accountId");

        return ResponseObject.<List<AvailableAccountResponse>>builder()
                .status(1000)
                .data(accountService.getAvailableAccountIdsInMyDepartmentBySyllabus(syllabusId, currentAccountId))
                .message("Available accounts retrieved successfully")
                .build();
    }
}
