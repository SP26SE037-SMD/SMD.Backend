package com.example.smd.services;

import com.example.smd.dto.request.account.AccountRequest;
import com.example.smd.dto.response.account.AccountResponse;
import com.example.smd.dto.response.account.ImportAccountResult;
import com.example.smd.dto.response.account.ImportResult;
import com.example.smd.entities.Account;
import com.example.smd.entities.Role;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.AccountMapper;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        return accountPage.map(accountMapper::toResponse);
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

    //Import tài khoản từ file Excel
    public ImportResult importAccounts(MultipartFile file, String roleName) {

        Role role = roleRepository.findByRoleName(roleName.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Set<String> emailSet = new HashSet<>();
        List<Account> accounts = new ArrayList<>();
        List<ImportAccountResult> results = new ArrayList<>();
        String randomPassword = generateRandomPassword();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                int rowNumber = i + 1;

                if (row == null) continue;

                try {

                    String email = row.getCell(0).getStringCellValue().trim();
                    String fullName = row.getCell(1).getStringCellValue().trim();

                    // duplicate trong file
                    if (!emailSet.add(email)) {
                        results.add(new ImportAccountResult(
                                email,
                                "FAILED",
                                "Duplicate email in Excel"
                        ));
                        continue;
                    }

                    Account account = new Account();
                    account.setEmail(email);
                    account.setFullName(fullName);
                    account.setRole(role);   // 👈 gán role từ API
                    account.setIsActive(true);
                    account.setPasswordHash(passwordEncoder.encode(randomPassword));

                    accounts.add(account);

                } catch (Exception e) {

                    results.add(new ImportAccountResult(
                            null,
                            "FAILED",
                            "Invalid data format"
                    ));
                }
            }

            // check duplicate trong DB
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
                            "Email already exists"
                    ));

                } else {

                    accountsToSave.add(account);

                    results.add(new ImportAccountResult(
                            account.getEmail(),
                            "SUCCESS",
                            "Created successfully"
                    ));
                }
            }

            accountRepository.saveAll(accountsToSave);

        } catch (IOException e) {
            throw new RuntimeException("Import failed", e);
        }

        long success = results.stream().filter(r -> r.getStatus().equals("SUCCESS")).count();
        long failed = results.stream().filter(r -> r.getStatus().equals("FAILED")).count();

        return new ImportResult(
                results.size(),
                (int) success,
                (int) failed,
                results
        );
    }

    public ByteArrayInputStream exportAccounts() throws IOException {
        List<Account> accounts = accountRepository.findAllWithRole();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Accounts");

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("Email");
        header.createCell(1).setCellValue("Full Name");
        header.createCell(2).setCellValue("Phone Number");
        header.createCell(3).setCellValue("Role");

        int rowIdx = 1;

        for (Account account : accounts) {

            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(account.getEmail());
            row.createCell(1).setCellValue(account.getFullName());
            row.createCell(2).setCellValue(account.getPhoneNumber());
            row.createCell(3).setCellValue(account.getRole().getRoleName());

        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return new ByteArrayInputStream(out.toByteArray());
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
}
