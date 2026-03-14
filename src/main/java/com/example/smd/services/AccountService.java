package com.example.smd.services;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.AccountResponse;
import com.example.smd.entities.Account;
import com.example.smd.entities.Account_Profile;
import com.example.smd.entities.Role;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AccountMapper;
import com.example.smd.repositories.AccountProfileRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountProfileRepository accountProfileRepository;
    private final RoleRepository roleRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccountProfileService accountProfileService;

    // GetAll tài khoản có phân trang và tìm kiếm theo role name hoặc full name
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(String search, String searchBy, int page, int size, String[] sort) {
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

        // 2. Logic tìm kiếm dựa trên searchBy parameter
        Page<Account> accountPage;
        if (search == null || search.trim().isEmpty()) {
            // Không có search, lấy tất cả accounts
            accountPage = accountRepository.findAll(pagingSort);
        } else {
            String searchTerm = search.trim();
            // Xác định loại tìm kiếm dựa trên searchBy
            switch (searchBy != null ? searchBy.toLowerCase() : "all") {
                case "role":
                    // Tìm theo role name
                    accountPage = accountRepository.findByRoleNameContaining(searchTerm, pagingSort);
                    break;
                case "name":
                    // Tìm theo full name
                    accountPage = accountRepository.findByFullNameContaining(searchTerm, pagingSort);
                    break;
                case "all":
                default:
                    accountPage = accountRepository.findAll(pagingSort);
                    break;
            }
        }

        // 3. Map nguyên Page<Entity> sang Page<DTO>
        return mapAccountPageWithPhoneNumbers(accountPage);
    }

    // API tìm kiếm account theo khoảng thời gian createdAt
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsByDateRange(
            java.time.Instant fromDate,
            java.time.Instant toDate,
            int page,
            int size,
            String[] sort) {

        // 1. Xử lý sắp xếp
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

        // 2. Logic tìm kiếm theo khoảng thời gian
        Page<Account> accountPage;
        if (fromDate != null && toDate != null) {
            // Cả hai đều có: tìm trong khoảng
            accountPage = accountRepository.findByCreatedAtBetween(fromDate, toDate, pagingSort);
        } else if (fromDate != null) {
            // Chỉ có fromDate: tìm từ ngày này trở đi
            accountPage = accountRepository.findByCreatedAtAfter(fromDate, pagingSort);
        } else if (toDate != null) {
            // Chỉ có toDate: tìm đến ngày này
            accountPage = accountRepository.findByCreatedAtBefore(toDate, pagingSort);
        } else {
            // Không có gì: lấy tất cả
            accountPage = accountRepository.findAll(pagingSort);
        }

        // 3. Map sang DTO
        return mapAccountPageWithPhoneNumbers(accountPage);
    }

    // Lấy chi tiết tài khoản theo ID
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(String accountId) {
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        Account_Profile profile = accountProfileRepository.findByAccountId(convert).orElse(null);
        String phoneNumber = profile != null ? profile.getPhoneNumber() : null;
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        return accountMapper.toResponse(account, phoneNumber, avatarUrl);
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

        // 4. Lưu account
        account = accountRepository.save(account);
        log.info("Created new account with ID: {}", account.getAccountId());
        // 5. Tự động tạo account profile
        accountProfileService.createProfile(account);

        return accountMapper.toResponse(account);
    }

    // Cập nhật thông tin tài khoản
    @Transactional
    public AccountResponse updateAccount(String accountId,
                                         boolean status,
                                         String request) {
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.setFullName(request);
        account.setIsActive(status);
        account = accountRepository.save(account);
        return accountMapper.toResponse(account);
    }

    // Xóa isActive tài khoản
    @Transactional
    public boolean isActiveAccount(String accountId, boolean status) {
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.setIsActive(status);
        accountRepository.save(account);

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

    private Page<AccountResponse> mapAccountPageWithPhoneNumbers(Page<Account> accountPage) {
        List<UUID> accountIds = accountPage.getContent().stream()
                .map(Account::getAccountId)
                .toList();

        if (accountIds.isEmpty()) {
            return accountPage.map(account -> accountMapper.toResponse(account, null, null));
        }

        Map<UUID, Account_Profile> profileByAccountId = accountProfileRepository.findByAccountIds(accountIds)
                .stream()
                .collect(Collectors.toMap(
                        ap -> ap.getAccount().getAccountId(),
                        ap -> ap
                ));

        return accountPage.map(account -> {
            Account_Profile ap = profileByAccountId.get(account.getAccountId());
            return accountMapper.toResponse(
                    account,
                    ap != null ? ap.getPhoneNumber() : null,
                    ap != null ? ap.getAvatarUrl() : null
            );
        });
    }
}
