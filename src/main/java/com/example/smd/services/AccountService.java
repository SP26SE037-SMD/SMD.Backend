package com.example.smd.services;

import com.example.smd.dto.excel.AccountExportDTO;
import com.example.smd.dto.excel.AccountImportDTO;
import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.request.account.AccountUpdateRequest;
import com.example.smd.dto.response.account.AvailableAccountResponse;
import com.example.smd.dto.response.account.AccountResponse;
import com.example.smd.dto.response.account.ImportAccountResult;
import com.example.smd.dto.response.account.ImportResult;
import com.example.smd.entities.Account;
import com.example.smd.entities.Department;
import com.example.smd.entities.Role;
import com.example.smd.enums.RoleName;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AccountMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.DepartmentRepository;
import com.example.smd.repositories.RoleRepository;
import com.example.smd.repositories.SyllabusRepository;
import com.example.smd.services.excelService.ExcelExporter;
import com.example.smd.services.excelService.ExcelImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final EmailService emailService;
    private final SyllabusRepository syllabusRepository;

    // GetAll tài khoản có phân trang và tìm kiếm theo role name hoặc full name
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAllAccounts(String search, String searchBy, int page, int size, String[] sort,
            String accountId) {

        var account = getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.ADMIN.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

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
        return accountPage.map(accountMapper::toResponse);
    }

    // API tìm kiếm account theo khoảng thời gian createdAt
    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsByDateRange(
            java.time.Instant fromDate,
            java.time.Instant toDate,
            int page,
            int size,
            String[] sort,
            String accountId) {
        var account = getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.ADMIN.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }
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
        return accountPage.map(accountMapper::toResponse);
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
    public AccountResponse createAccount(AccountRequest request, String accountId) {
        var checkRole = getAccountById(accountId);
        String roleName = checkRole.getRole().getRoleName();
        if (!(RoleName.ADMIN.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Kiểm tra email đã tồn tại chưa
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        } else if (!roleRepository.existsByRoleName(request.getRoleName().toUpperCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.ROLE_NOT_FOUND);
        } else if (!departmentRepository.existsByDepartmentCode(request.getDepartmentCode().toUpperCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        // 2. Tạo entity Account và mã hóa password
        Account account = accountMapper.toEntity(request);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // 3. Gán role cho account nếu có
        if (request.getRoleName() != null) {
            Role role = roleRepository.findByRoleName(request.getRoleName())
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            account.setRole(role);
        }
        if (request.getDepartmentCode() != null) {
            Department department = departmentRepository.findByDepartmentCode(request.getDepartmentCode())
                    .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
            account.setDepartment(department);
        }

        // 4. Lưu account
        account = accountRepository.save(account);
        log.info("Created new account with ID: {}, {}",
                account.getAccountId(), account.getDepartment().getDepartmentCode());

        // Send notification email after successful account creation.
        emailService.sendAccountCreatedEmailsBatch(List.of(
                new EmailService.AccountCreatedEmail(
                        account.getEmail(),
                        account.getFullName())));

        return accountMapper.toResponse(account);
    }

    // Cập nhật thông tin tài khoản
    @Transactional
    public AccountResponse updateAccount(String accountId,
            boolean status,
            AccountUpdateRequest request) {
        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountMapper.updateEntity(account, request);
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

    @Transactional
    public boolean changeDepartment(String accountId,
            String departmentCode, String adminId) {

        var checkRole = getAccountById(adminId);
        String roleName = checkRole.getRole().getRoleName();
        if (!(RoleName.ADMIN.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        var convert = UUID.fromString(accountId);
        Account account = accountRepository.findById(convert)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        Department department = departmentRepository.findByDepartmentCode(departmentCode)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        account.setDepartment(department);
        accountRepository.save(account);

        return true;
    }

    // Import tài khoản từ file Excel
    public ImportResult importAccounts(MultipartFile file, String roleName) {

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        List<AccountImportDTO> rows = null;
        try {
            rows = ExcelImporter.importFromExcel(file, AccountImportDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Set<String> emailSet = new HashSet<>();
        List<Account> accounts = new ArrayList<>();
        List<ImportAccountResult> results = new ArrayList<>();

        for (AccountImportDTO dto : rows) {

            try {

                String email = dto.getEmail() != null ? dto.getEmail().trim() : "";
                String fullName = dto.getFullName() != null ? dto.getFullName().trim() : "";
                String phoneNumber = dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : "";
                String departmentCode = dto.getDepartmentCode() != null ? dto.getDepartmentCode().trim() : "";

                // duplicate trong file
                if (!emailSet.add(email)) {

                    results.add(new ImportAccountResult(
                            email,
                            "FAILED",
                            "Duplicate email in Excel"));

                    continue;
                }

                Department department = departmentRepository.findByDepartmentCode(departmentCode)
                        .orElse(null);
                if (department == null) {
                    results.add(new ImportAccountResult(
                            email,
                            "FAILED",
                            "Department code not found"));
                    continue;
                }

                // Validate phoneNumber
                if (phoneNumber.isEmpty()) {
                    results.add(new ImportAccountResult(
                            email,
                            "FAILED",
                            "Phone number is required"));
                    continue;
                }

                // Validate phoneNumber format (10-11 digits)
                if (!phoneNumber.matches("^[0-9]{10,11}$")) {
                    results.add(new ImportAccountResult(
                            email,
                            "FAILED",
                            "Phone number must be 10-11 digits"));
                    continue;
                }

                Account account = new Account();
                String randomPassword = generateRandomPassword();
                account.setEmail(email);
                account.setFullName(fullName);
                account.setPhoneNumber(phoneNumber);
                account.setRole(role);
                account.setIsActive(true);
                account.setPasswordHash(passwordEncoder.encode(randomPassword));
                account.setDepartment(department);
                accounts.add(account);

            } catch (Exception e) {
                results.add(new ImportAccountResult(
                        null,
                        "FAILED",
                        "Invalid data format"));
            }
        }

        // check duplicate DB
        Set<String> emails = accounts.stream()
                .map(Account::getEmail)
                .collect(Collectors.toSet());

        List<Account> existingAccounts = accountRepository.findByEmailIn(emails);

        Set<String> existingEmails = existingAccounts.stream()
                .map(Account::getEmail)
                .collect(Collectors.toSet());

        List<Account> accountsToSave = new ArrayList<>();

        for (Account account : accounts) {

            if (existingEmails.contains(account.getEmail())) {

                results.add(new ImportAccountResult(
                        account.getEmail(),
                        "FAILED",
                        "Email already exists"));

            } else {

                accountsToSave.add(account);

                results.add(new ImportAccountResult(
                        account.getEmail(),
                        "SUCCESS",
                        "Created successfully"));
            }
        }

        List<Account> savedAccounts = accountRepository.saveAll(accountsToSave);

        if (!savedAccounts.isEmpty()) {
            List<EmailService.AccountCreatedEmail> emailPayloads = savedAccounts.stream()
                    .map(account -> new EmailService.AccountCreatedEmail(
                            account.getEmail(),
                            account.getFullName()))
                    .toList();

            emailService.sendAccountCreatedEmailsBatch(emailPayloads);
        }

        long success = results.stream()
                .filter(r -> r.getStatus().equals("SUCCESS"))
                .count();

        long failed = results.stream()
                .filter(r -> r.getStatus().equals("FAILED"))
                .count();

        return new ImportResult(
                results.size(),
                (int) success,
                (int) failed,
                results);
    }

    public ByteArrayInputStream exportAccounts() throws Exception {

        List<AccountExportDTO> accounts = accountRepository.exportAccounts();

        // List<AccountExportDTO> dtoList = accounts.stream()
        // .map(a -> {
        //
        // AccountExportDTO dto = new AccountExportDTO();
        //
        // dto.setEmail(a.getEmail());
        // dto.setFullName(a.getFullName());
        // dto.setPhoneNumber(a.getPhoneNumber());
        // dto.setRole(a.getRole().getRoleName());
        // dto.setDepartmentCode(a.getDepartment() != null ?
        // a.getDepartment().getDepartmentCode() : "");
        // dto.setDepartmentName(a.getDepartment() != null ?
        // a.getDepartment().getDepartmentName() : "");
        // return dto;
        //
        // }).toList();

        return ExcelExporter.export(accounts, AccountExportDTO.class);
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
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

    public List<AccountResponse> getAccountsByDepartment(UUID departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new AppException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        // 2. Nếu tồn tại, tiến hành lấy danh sách account
        List<Account> accounts = accountRepository.findAllByDepartmentId(departmentId);

        return accounts.stream()
                .map(accountMapper::toResponse)
                .toList();
    }

//    @Transactional(readOnly = true)
//    public List<AvailableAccountResponse> getAvailableAccountIdsInMyDepartmentBySyllabus(UUID syllabusId,
//            String currentAccountId) {
//        if (!syllabusRepository.existsById(syllabusId)) {
//            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
//        }
//
//        UUID currentId = UUID.fromString(currentAccountId);
//        Account currentAccount = accountRepository.findById(currentId)
//                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
//
//        if (currentAccount.getDepartment() == null) {
//            throw new AppException(ErrorCode.DEPARTMENT_NOT_FOUND);
//        }
//
//        UUID departmentId = currentAccount.getDepartment().getDepartmentId();
//
//        Set<UUID> assignedAccountIds = taskRepository
//                .findDistinctAccountIdsBySyllabusIdAndDepartmentId(syllabusId, departmentId);
//
//        return accountRepository.findAllByDepartmentId(departmentId).stream()
//                .filter(account -> !account.getAccountId().equals(currentId))
//                .filter(account -> !assignedAccountIds.contains(account.getAccountId()))
//                .map(account -> AvailableAccountResponse.builder()
//                        .accountId(account.getAccountId())
//                        .email(account.getEmail())
//                        .fullName(account.getFullName())
//                        .avatarUrl(account.getAvatarUrl())
//                        .build())
//                .toList();
//    }
}
