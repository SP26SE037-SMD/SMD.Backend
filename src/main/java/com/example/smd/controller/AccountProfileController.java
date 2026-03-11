package com.example.smd.controller;

import com.example.smd.dto.request.AccountProfileUpdateRequest;
import com.example.smd.dto.response.AccountProfileResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.services.AccountProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account Profile", description = "Account Profile Management APIs - Quản lý thông tin cá nhân tài khoản")
@RestController
@RequestMapping("/api/account-profiles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AccountProfileController {

    private final AccountProfileService accountProfileService;

    /**
     * API lấy profile theo Account ID
     */
    @GetMapping("/{accountId}")
    @Operation(
        summary = "Get account profile by Account ID",
        description = "Retrieve account profile information including avatar URL and phone number."
    )
    public ResponseObject<AccountProfileResponse> getProfileByAccountId(
            @Parameter(description = "Account ID (UUID)")
            @PathVariable String accountId
    ) {
        return ResponseObject.<AccountProfileResponse>builder()
                .status(1000)
                .data(accountProfileService.getProfileByAccountId(accountId))
                .message("Get account profile successfully")
                .build();
    }

    /**
     * API cập nhật profile
     */
    @PutMapping("/{accountId}")
    @PreAuthorize("hasAuthority('ACCOUNT_PROFILE_UPDATE')")
    @Operation(
        summary = "Update account profile",
        description = "Update account profile information such as avatar URL and phone number. " +
                "Only provided fields will be updated."
    )
    public ResponseObject<AccountProfileResponse> updateProfile(
            @Parameter(description = "Account ID (UUID)")
            @PathVariable String accountId,

            @Valid @RequestBody AccountProfileUpdateRequest request
    ) {
        return ResponseObject.<AccountProfileResponse>builder()
                .status(1000)
                .data(accountProfileService.updateProfile(accountId, request))
                .message("Update account profile successfully")
                .build();
    }
}
