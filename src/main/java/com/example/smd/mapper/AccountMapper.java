package com.example.smd.mapper;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.AccountLoginResponse;
import com.example.smd.dto.response.AccountResponse;
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
                .role(account.getRole() != null ? roleMapper.toResponse(account.getRole()) : null)
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
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
    }
}
