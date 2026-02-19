package com.example.smd.mapper;

import com.example.smd.dto.request.AccountRequest;
import com.example.smd.dto.response.AccountResponse;
import com.example.smd.entities.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    private final RoleMapper roleMapper;

    public AccountMapper(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .username(account.getUsername())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .role(account.getRole() != null ? roleMapper.toResponse(account.getRole()) : null)
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .lastLogin(account.getLastLogin())
                .build();
    }

    public Account toEntity(AccountRequest request) {
        if (request == null) {
            return null;
        }

        return Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .isActive(request.getIsActive())
                .build();
    }

    public void updateEntity(Account account, AccountRequest request) {
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            account.setFullName(request.getFullName());
        }
        if (request.getIsActive() != null) {
            account.setIsActive(request.getIsActive());
        }
    }
}
