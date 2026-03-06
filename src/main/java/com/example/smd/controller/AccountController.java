package com.example.smd.controller;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.AccountResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account", description = "Account Management APIs")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")

public class AccountController {

    private final AccountService accountService;

    // API lấy danh sách tài khoản có phân trang và tìm kiếm
    @GetMapping
    @Operation(summary = "Get all accounts with pagination and search")
    public ResponseObject<PagedResponse<AccountResponse>> getAllAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<AccountResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(accountService.getAllAccounts(search, page, size, sort)))
                .message("Get all accounts successfully")
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
    @Operation(summary = "Create new account")
    public ResponseObject<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.createAccount(request))
                .message("Create account successfully")
                .build();
    }

    // API cập nhật tài khoản theo ID
    @PutMapping("/{id}")
    @Operation(summary = "Update account by ID")
    public ResponseObject<AccountResponse> updateInformationAccount(
            @PathVariable String id,
            @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.updateAccount(id, request))
                .message("Update account successfully")
                .build();
    }


    // API xóa tài khoản theo ID
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete account by ID")
    public ResponseObject<Boolean> deleteAccount(@PathVariable String id) {
        return ResponseObject.<Boolean>builder()
                .status(1000)
                .data(accountService.deleteAccount(id))
                .message("Delete account successfully")
                .build();
    }
}
