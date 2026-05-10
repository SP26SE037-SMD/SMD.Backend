package com.example.smd.services;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.major.ImportMajorResponse;
import com.example.smd.dto.response.major.ImportMajorResult;
import com.example.smd.entities.Major;
import com.example.smd.entities.PO;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.RoleName;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.POsRepository;
import com.example.smd.repositories.RegulationRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MajorService {
    AccountService accountService;
    RegulationRepository regulationRepository;
    MajorRepository majorRepository;
    MajorMapper majorMapper;
    POsRepository poRepository;

    @Transactional
    public Page<MajorResponse> getAllMajors(String accountId, String search, String searchBy, String status, int page,
            int size, String[] sort) {
        // 1. Khởi tạo Pageable
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        // 2. Chuẩn hóa Status: null, rỗng hoặc "all" đều được coi là "Không lọc" (null)
        String finalStatus = (status == null || status.isBlank() || "all".equalsIgnoreCase(status))
                ? null
                : status.trim();

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // 3. Phân quyền (Sửa lỗi NullPointerException và logic GetAll)

        // Nếu là STUDENT/LECTURER: Họ chỉ được phép xem PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            // Nếu họ muốn getAll (finalStatus == null) hoặc chọn status khác PUBLISHED
            if (finalStatus == null || !PloStatus.PUBLISHED.toString().equals(finalStatus)) {
                // Ép về PUBLISHED để bảo mật dữ liệu nháp, thay vì quăng lỗi gây crash
                finalStatus = PloStatus.PUBLISHED.toString();
            }
        }

        // Nếu muốn xem DRAFT: Chỉ VP mới được xem
        // Dùng Yoda conditions (đẩy Enum lên trước) để tránh lỗi null.equals
        if (PloStatus.DRAFT.toString().equals(finalStatus)) {
            if (!RoleName.VP.toString().equals(roleName)) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        // 4. Logic Rẽ Nhánh (Đảm bảo getAll hoạt động khi finalStatus là null)
        Page<Major> majorPage;
        boolean hasStatus = (finalStatus != null);
        boolean hasSearch = (search != null && !search.isBlank());
        String type = (searchBy != null && !searchBy.isBlank()) ? searchBy.toLowerCase() : "all";

        if (!hasSearch) {
            // TRƯỜNG HỢP 1: Nếu finalStatus là null (getAll cho Admin/VP) -> Chạy
            // findAll(pageable)
            majorPage = hasStatus ? majorRepository.findByStatus(finalStatus, pageable)
                    : majorRepository.findAll(pageable);
        } else {
            String searchLower = search.trim();
            if (hasStatus) {
                majorPage = switch (type) {
                    case "code" -> majorRepository.findByMajorCodeContainingIgnoreCaseAndStatus(searchLower,
                            finalStatus, pageable);
                    case "name" -> majorRepository.findByMajorNameContainingIgnoreCaseAndStatus(searchLower,
                            finalStatus, pageable);
                    default -> majorRepository.searchAllFieldsWithStatus(searchLower, finalStatus, pageable);
                };
            } else {
                // Nếu không có status, search trên toàn bộ dữ liệu (getAll + search)
                majorPage = switch (type) {
                    case "code" -> majorRepository.findByMajorCodeContainingIgnoreCase(searchLower, pageable);
                    case "name" -> majorRepository.findByMajorNameContainingIgnoreCase(searchLower, pageable);
                    default -> majorRepository.findByMajorNameContainingIgnoreCaseOrMajorCodeContainingIgnoreCase(
                            searchLower, searchLower, pageable);
                };
            }
        }

        return majorPage.map(majorMapper::toMajorResponse);
    }

    // Create Major
    @Transactional
    public MajorResponse createMajor(MajorRequest request, String accountId) {

        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.VP.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (majorRepository.existsByMajorCode(request.getMajorCode())) {
            throw new AppException(ErrorCode.MAJOR_CODE_EXISTS);
        }

        Major major = majorMapper.toMajor(request);
        major.setStatus(PloStatus.DRAFT.toString());
        var response = majorRepository.save(major);
        return majorMapper.toMajorResponse(response);
    }

    // Update Major
    @Transactional
    public MajorResponse updateMajor(UUID id, MajorRequest request, String accountId) {

        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        if (!PloStatus.DRAFT.toString().equals(major.getStatus())) {
            throw new AppException(ErrorCode.MAJOR_NOT_DRAFT);
        }

        major.setMajorName(request.getMajorName());
        major.setDescription(request.getDescription());
        major.setUpdatedAt(Instant.now());

        var response = majorRepository.save(major);
        return majorMapper.toMajorResponse(response);
    }

    // Delete Major (Xóa mềm)
    @Transactional
    public void deleteMajor(UUID id, String accountId) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        if ("DRAFT".equals(major.getStatus())) {
            regulationRepository.deleteByMajorId(id);
            majorRepository.delete(major);
        } else {
            major.setStatus(PloStatus.ARCHIVED.toString());
            majorRepository.save(major);
        }
    }

    @Transactional
    public MajorResponse getMajorDetail(String majorCode, String accountId) {
        Major major = majorRepository.findByMajorCode(majorCode)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        // Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (RoleName.STUDENT.toString().equals(account.getRole().getRoleName())
                || RoleName.LECTURER.toString().equals(account.getRole().getRoleName())) {
            if (!PloStatus.PUBLISHED.toString().equals(major.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(major.getStatus())) {
            if (!RoleName.VP.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return majorMapper.toMajorResponse(major);
    }

    @Transactional
    public MajorResponse getMajorById(UUID id, String accountId) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        // Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (RoleName.STUDENT.toString().equals(account.getRole().getRoleName())
                || RoleName.LECTURER.toString().equals(account.getRole().getRoleName())) {
            if (!PloStatus.PUBLISHED.toString().equals(major.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        return majorMapper.toMajorResponse(major);
    }

    @Transactional
    public MajorResponse updateStatus(String id, String newStatus) {
        // 1. Kiểm tra trạng thái có hợp lệ không
        PloStatus status;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            status = PloStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_MAJOR_STATUS);
        }

        // 2. Tìm CLO theo ID
        Major major = majorRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        // 3. Cập nhật trạng thái
        major.setStatus(status.toString());
        major.setUpdatedAt(Instant.now());
        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    public Page<MajorResponse> getMajorsUpdatedInLast24Hours(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        // Mốc bắt đầu: 24 tiếng trước
        Instant startTime = Instant.now().minus(24, ChronoUnit.HOURS);
        // Mốc kết thúc: Bây giờ
        Instant endTime = Instant.now();

        // Truyền đủ 2 tham số vào hàm Repo đã viết
        return majorRepository.findByStatusAndUpdatedBetween(status, startTime, endTime, pageable)
                .map(majorMapper::toMajorResponse);
    }

    /**
     * Import Major + POs từ Excel.
     * Mỗi dòng gồm: Major Code, Name, Description, PO Code, PO Description.
     * Một Major có thể có nhiều dòng (nhiều PO). Major Code dùng để nhóm các PO.
     * Validate:
     * - Major Code đã tồn tại trong DB → báo lỗi, skip toàn bộ dòng có Major Code
     * đó.
     * - PO Code phải duy nhất (global) → validate trong file và trong DB.
     */
    @Transactional
    public ImportMajorResponse importMajors(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Major");
            if (sheet == null)
                sheet = workbook.getSheetAt(0);
            return importMajorFromSheet(sheet);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import major failed: " + e.getMessage());
        }
    }

    @Transactional
    public ImportMajorResponse importMajorFromSheet(Sheet sheet) {
        List<ImportMajorResult> details = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try {
            int majorCodeCol = -1, majorNameCol = -1, majorDescCol = -1;
            int poCodeCol = -1, poDescCol = -1;

            String parsedMajorCode = null;
            String parsedMajorName = null;
            String parsedMajorDesc = null;

            List<PO> poList = new ArrayList<>();
            Set<String> poCodesInFile = new HashSet<>();

            int state = 0; // 0 = find major header, 1 = read major data, 2 = find po header, 3 = read po
                           // data

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                if (state == 0) { // find major header
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = getCellValue(row, c, formatter);
                        if (cellVal.equalsIgnoreCase("Major Code"))
                            majorCodeCol = c;
                        else if (cellVal.equalsIgnoreCase("Name") || cellVal.equalsIgnoreCase("Major Name"))
                            majorNameCol = c;
                        else if (cellVal.equalsIgnoreCase("Description"))
                            majorDescCol = c;
                    }
                    if (majorCodeCol != -1 && majorNameCol != -1) {
                        state = 1;
                    }
                } else if (state == 1) { // read major data
                    String code = getCellValue(row, majorCodeCol, formatter);
                    if (code != null && !code.isEmpty()) {
                        parsedMajorCode = code;
                        parsedMajorName = getCellValue(row, majorNameCol, formatter);
                        parsedMajorDesc = majorDescCol != -1 ? getCellValue(row, majorDescCol, formatter) : null;
                        state = 2;
                    }
                } else if (state == 2) { // find po header
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = getCellValue(row, c, formatter);
                        if (cellVal.equalsIgnoreCase("PO Code"))
                            poCodeCol = c;
                        else if (cellVal.equalsIgnoreCase("Description") || cellVal.equalsIgnoreCase("PO Description"))
                            poDescCol = c;
                    }
                    if (poCodeCol != -1) {
                        state = 3;
                    }
                } else if (state == 3) { // read po data
                    String poCode = getCellValue(row, poCodeCol, formatter);
                    if (poCode != null && !poCode.isEmpty()) {
                        String poDesc = poDescCol != -1 ? getCellValue(row, poDescCol, formatter) : null;

                        if (!poCodesInFile.add(poCode.toUpperCase())) {
                            details.add(ImportMajorResult.builder()
                                    .majorCode(parsedMajorCode)
                                    .poCode(poCode)
                                    .status("FAILED")
                                    .message("Duplicate PO code in file: " + poCode)
                                    .build());
                            continue;
                        }

                        // PO Code no longer needs to be globally unique. It only needs to be unique
                        // within the new major.
                        // Since we track poCodesInFile, we guarantee uniqueness within the file.
                        // And since the Major is brand new (rejected if exists), there are no existing
                        // POs for this Major to collide with.

                        PO po = PO.builder()
                                .poCode(poCode)
                                .description(poDesc)
                                .status(PloStatus.DRAFT.toString())
                                .build();
                        poList.add(po);

                        details.add(ImportMajorResult.builder()
                                .majorCode(parsedMajorCode)
                                .poCode(poCode)
                                .status("SUCCESS")
                                .message("Will be created")
                                .build());
                    }
                }
            }

            if (parsedMajorCode == null) {
                details.add(ImportMajorResult.builder()
                        .status("FAILED")
                        .message("Major Data not found in sheet")
                        .build());
            } else if (majorRepository.existsByMajorCode(parsedMajorCode)) {
                details.add(ImportMajorResult.builder()
                        .majorCode(parsedMajorCode)
                        .status("FAILED")
                        .message("Major code already exists: " + parsedMajorCode)
                        .build());
            } else if (parsedMajorName == null || parsedMajorName.isEmpty()) {
                details.add(ImportMajorResult.builder()
                        .majorCode(parsedMajorCode)
                        .status("FAILED")
                        .message("Missing required field: Name")
                        .build());
            } else {
                // Determine if all POs are valid (no FAILED in details for this major)
                boolean hasErrors = details.stream().anyMatch(d -> "FAILED".equals(d.getStatus()));
                if (!hasErrors) {
                    Major major = Major.builder()
                            .majorCode(parsedMajorCode)
                            .majorName(parsedMajorName)
                            .description(parsedMajorDesc)
                            .status(PloStatus.DRAFT.toString())
                            .build();
                    Major savedMajor = majorRepository.save(major);

                    for (PO po : poList) {
                        po.setMajor(savedMajor);
                    }
                    poRepository.saveAll(poList);
                } else {
                    details.add(ImportMajorResult.builder()
                            .majorCode(parsedMajorCode)
                            .status("FAILED")
                            .message("Major not saved due to PO errors")
                            .build());
                }
            }

        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "Import major from sheet failed: " + e.getMessage());
        }

        int total = details.size();
        int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = total - success;

        return ImportMajorResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .details(details)
                .build();
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null)
            return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null)
            return "";

        if (cell.getCellType() == CellType.FORMULA) {
            FormulaEvaluator evaluator = row.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
            return formatter.formatCellValue(cell, evaluator).trim();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private String trim(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
