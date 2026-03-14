package com.example.smd.services;

import com.example.smd.dto.request.AccountProfileUpdateRequest;
import com.example.smd.dto.response.AccountProfileResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Account_Profile;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AccountProfileMapper;
import com.example.smd.repositories.AccountProfileRepository;
import com.example.smd.repositories.AccountRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountProfileService {

    AccountProfileRepository accountProfileRepository;
    AccountRepository accountRepository;
    AccountProfileMapper accountProfileMapper;

    /**
     * Lấy profile theo Account ID
     */
    @Transactional(readOnly = true)
    public AccountProfileResponse getProfileByAccountId(String accountId) {


        UUID accountUuid = UUID.fromString(accountId);

        // Kiểm tra account tồn tại
        if (!accountRepository.existsById(accountUuid)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        } else {
        }

        // Tìm profile
        Account_Profile profile = accountProfileRepository.findByAccountId(accountUuid)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_PROFILE_NOT_FOUND));

        return accountProfileMapper.toResponse(profile);
    }

    /**
     * Cập nhật profile
     */
    @Transactional
    public AccountProfileResponse updateProfile(String accountId, AccountProfileUpdateRequest request) {

        UUID accountUuid = UUID.fromString(accountId);
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var account = accountRepository.findById(accountUuid);
        if (account.isEmpty()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        Account_Profile profile =
                accountProfileRepository.findByAccountId(accountUuid).orElse(createProfile(account.get()));


        var checkAccount = accountRepository.findByEmail(email);

        log.info("Account {} is trying to update profile of account {}", email, accountId);
        if("ADMIN".equalsIgnoreCase(checkAccount.get().getRole().getRoleName())) {
        } else if (!profile.getAccount().getAccountId().equals(checkAccount.get().getAccountId())) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_OWNER);
        }
        // Cập nhật các trường
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber());
        } else {
            profile.setPhoneNumber("Unknown");
        }

        // Lưu lại
        Account_Profile updatedProfile = accountProfileRepository.save(profile);

        return accountProfileMapper.toResponse(updatedProfile);
    }

    /**
     * Tạo profile mới cho account (internal use - được gọi từ AccountService)
     */
    @Transactional
    public Account_Profile createProfile(Account account) {


        Account_Profile profile = Account_Profile.builder()
                .account(account)
                .phoneNumber("Unknown") // Mặc định nếu không có số điện thoại
                .avatarUrl("Unknown") // Mặc định nếu không có avatar
                .build();

        return accountProfileRepository.save(profile);
    }
}
