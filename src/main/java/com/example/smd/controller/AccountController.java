package com.example.smd.controller;

import com.example.smd.dto.request.AccountRequest;
import com.example.smd.dto.response.AccountResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account", description = "Account Management APIs")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")

public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Get all accounts with pagination and search")
    public ResponseObject<Page<AccountResponse>> getAllAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Page<AccountResponse> accounts = accountService.getAllAccounts(search, page, size, sort);
        return ResponseObject.<Page<AccountResponse>>builder()
                .status(1000)
                .data(accounts)
                .message("Get all accounts successfully")
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseObject<AccountResponse> getAccountById(@PathVariable String id) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.getAccountById(id))
                .message("Get account successfully")
                .build();
    }

    @PostMapping
    @Operation(summary = "Create new account")
    public ResponseObject<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.createAccount(request))
                .message("Create account successfully")
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account by ID")
    public ResponseObject<AccountResponse> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody AccountRequest request) {
        return ResponseObject.<AccountResponse>builder()
                .status(1000)
                .data(accountService.updateAccount(id, request))
                .message("Update account successfully")
                .build();
    }

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
