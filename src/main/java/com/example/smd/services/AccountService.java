package com.example.smd.services;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.AccountResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Role;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AccountMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    // GetAll tài khoản có phân trang
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(String search, int page, int size, String[] sort) {
        // 1. Xử lý sắp xếp (Sắp xếp theo field CamelCase của Java)
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        // 2. Logic tìm kiếm để lấy Page<Entity>
        if (search == null || search.trim().isEmpty()) {
            return accountRepository.findAll(pagingSort)
                    .map(accountMapper::toResponse);
        }

        // 3. Map nguyên List Entity sang DTO bằng method reference của MapStruct
        return accountRepository.findAll(pagingSort)
                .map(accountMapper::toResponse);
    }

    // Lấy chi tiết tài khoản theo ID
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(String accountId) {
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        return accountMapper.toResponse(account);
    }

    // Tạo tài khoản mới
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        // 1. Kiểm tra email đã tồn tại chưa
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        } else if (!roleRepository.existsByRoleName(request.getRoleName().toUpperCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 2. Tạo entity Account và mã hóa password
        Account account = accountMapper.toEntity(request);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // 3. Gán role cho account nếu có
        if (request.getRoleName() != null) {
            Role role = roleRepository.findByRoleName(request.getRoleName().toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            account.setRole(role);
        }

        account = accountRepository.save(account);
        return accountMapper.toResponse(account);
    }

    // Cập nhật thông tin tài khoản
    @Transactional
    public AccountResponse updateAccount(String accountId, AccountUpdateRequest request) {
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));


        accountMapper.updateEntity(account, request);


        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        account = accountRepository.save(account);
        return accountMapper.toResponse(account);
    }

    // Xóa tài khoản
    @Transactional
    public boolean deleteAccount(String accountId) {
        var convert = UUID.fromString(accountId);

        if (!accountRepository.existsById(convert)) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        accountRepository.deleteById(convert);
        return true;
    }

    // Helper method để xử lý hướng sắp xếp
    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }
}
