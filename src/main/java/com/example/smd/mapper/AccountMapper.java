package com.example.smd.mapper;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.account.AccountLoginResponse;
import com.example.smd.dto.response.account.AccountResponse;
import com.example.smd.entities.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    private final RoleMapper roleMapper;

    public AccountMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    // Chuyển đổi từ Entity Account sang DTO AccountResponse
    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phoneNumber(account.getPhoneNumber())
                .avatarUrl(account.getAvatarUrl())
                .role(account.getRole() != null ? roleMapper.toResponse(account.getRole()) : null)
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .departmentName(account.getDepartment() != null ? account.getDepartment().getDepartmentName() : null)
                .build();
    }

    public AccountLoginResponse toLoginResponse(Account account) {
        if (account == null) {
            return null;
        }

        return AccountLoginResponse.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .role(account.getRole().getRoleName())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .build();
    }

    // Chuyển đổi từ DTO AccountRequest sang Entity Account
    public Account toEntity(AccountRequest request) {
        if (request == null) {
            return null;
        }

        return Account.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .isActive(request.getIsActive())
                .build();
    }

    // Cập nhật thông tin Entity Account từ DTO AccountUpdateRequest
    public void updateEntity(Account account, AccountUpdateRequest request) {
        if (request.getFullName() != null) {
            account.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl());
        }
    }
}
