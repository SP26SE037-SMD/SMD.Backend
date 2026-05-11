package com.example.smd.services;

import com.example.smd.dto.excel.CurriculumImportDTO;
import com.example.smd.dto.request.NotificationRequest;
import com.example.smd.dto.request.curriculum.CurriculumCreateRequest;
import com.example.smd.dto.response.CurriculumResponse;
import com.example.smd.dto.response.CurriculumShortResponse;
import com.example.smd.dto.response.curriculum.ImportCurriculumResponse;
import com.example.smd.dto.response.curriculum.ImportCurriculumResult;
import com.example.smd.entities.*;
import com.example.smd.enums.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CurriculumMapper;
import com.example.smd.repositories.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
public class CurriculumService {
    AccountRepository accountRepository;
    CurriculumRepository curriculumRepository;
    MajorRepository majorRepository;
    CurriculumMapper curriculumMapper;
    AccountService accountService;
    NotificationService notificationService;
    PLOsRepository plOsRepository;
    POsRepository poRepository;
    PoPloMappingRepository poPloMappingRepository;
    SubjectRepository subjectRepository;
    CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;

    /**
     * Lấy danh sách curriculum với phân trang và bộ lọc
     *
     * @param search   - Từ khóa tìm kiếm (có thể null)
     * @param searchBy - Tìm theo trường nào: code, name, hoặc all
     * @param status   - Filter theo status (có thể null)
     * @param page     - Số trang (0-based)
     * @param size     - Số lượng item mỗi trang
     * @param sort     - Mảng sort [field, direction]
     * @return Page<CurriculumResponse>
     */
    @Transactional(readOnly = true)
    public Page<CurriculumResponse> getAllCurriculums(
            String search,
            String searchBy,
            String status,
            int page,
            int size,
            String[] sort,
            String accountId) {

        // 1. Khởi tạo Pageable
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        // 2. Chuẩn hóa Status & Phân quyền (Giống hệt Major)
        // Xử lý trường hợp chuỗi rỗng hoặc "all" từ Frontend
        String finalStatus = (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("all"))
                ? null
                : status.trim();

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!PloStatus.PUBLISHED.toString().equals(finalStatus)) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(finalStatus)) {
            if (!RoleName.HOCFDC.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        Page<Curriculum> curriculumPage;
        boolean hasStatus = finalStatus != null;
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String type = (searchBy != null && !searchBy.trim().isEmpty()) ? searchBy.toLowerCase() : "all";

        // 3. Logic QUYẾT ĐỊNH (Điểm mấu chốt để không bị GetAll)
        if (!hasSearch) {
            // TRƯỜNG HỢP 1: Không có search -> Kiểm tra status để gọi đúng hàm Repo
            if (hasStatus) {
                curriculumPage = curriculumRepository.findByStatus(finalStatus, pageable);
            } else {
                // Chỉ Admin/VP mới có thể vào đây nếu không truyền status
                curriculumPage = curriculumRepository.findAll(pageable);
            }
        } else {
            String searchLower = search.trim();

            if (hasStatus) {
                // TRƯỜNG HỢP 2: Có Search + Có Status (Dùng AND trong SQL)
                curriculumPage = switch (type) {
                    case "code" ->
                        curriculumRepository.findByCurriculumCodeContainingIgnoreCaseAndStatus(searchLower, finalStatus,
                                pageable);
                    case "name" ->
                        curriculumRepository.findByCurriculumNameContainingIgnoreCaseAndStatus(searchLower, finalStatus,
                                pageable);
                    default -> curriculumRepository.searchAllFieldsWithStatus(searchLower, finalStatus, pageable);
                };
            } else {
                // TRƯỜNG HỢP 3: Có Search nhưng không có Status (Chỉ dành cho Admin/VP)
                curriculumPage = switch (type) {
                    case "code" -> curriculumRepository.findByCurriculumCodeContainingIgnoreCase(searchLower, pageable);
                    case "name" -> curriculumRepository.findByCurriculumNameContainingIgnoreCase(searchLower, pageable);
                    default ->
                        curriculumRepository
                                .findByCurriculumNameContainingIgnoreCaseOrCurriculumCodeContainingIgnoreCase(
                                        searchLower, searchLower, pageable);
                };
            }
        }

        return curriculumPage.map(curriculumMapper::toCurriculumResponse);
    }

    /**
     * Lấy danh sách curriculum theo majorId
     *
     * @param majorId - ID của Major
     * @return List<CurriculumShortResponse>
     */
    @Transactional(readOnly = true)
    public List<CurriculumShortResponse> getCurriculumsByMajor(UUID majorId) {
        log.info("Fetching curriculums for major ID: {}", majorId);

        // Kiểm tra Major tồn tại
        if (!majorRepository.existsById(majorId)) {
            throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
        }

        List<Curriculum> curriculums = curriculumRepository.findByMajor_MajorId(majorId);

        return curriculums.stream()
                .map(curriculumMapper::toCurriculumShortResponse)
                .toList();
    }

    /**
     * Tạo curriculum mới
     */
    @Transactional
    public CurriculumResponse createCurriculum(CurriculumCreateRequest request, String accountId) {

        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOCFDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Validate curriculum code không trùng
        if (curriculumRepository.existsByCurriculumCode(request.getCurriculumCode())) {
            throw new AppException(ErrorCode.CURRICULUM_CODE_EXISTS);
        }

        // 3. Kiểm tra Major tồn tại
        Major major = majorRepository.findById(UUID.fromString(request.getMajorId()))
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        if (!(PloStatus.PUBLISHED.toString().equals(major.getStatus())
                || PloStatus.INTERNAL_REVIEW.toString().equals(major.getStatus()))) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_CREATE);
        }

        // 4. Map request sang entity
        Curriculum curriculum = curriculumMapper.toCreateCurriculum(request);
        curriculum.setEndYear(null); // Khi tạo mới, endYear có thể để null, sẽ được cập nhật sau khi có thông tin
        curriculum.setMajor(major);

        // 5. Set default status nếu chưa có
        if (curriculum.getStatus() == null || curriculum.getStatus().isEmpty()) {
            curriculum.setStatus("DRAFT");
        }

        // 6. Lưu vào database
        Curriculum savedCurriculum = curriculumRepository.save(curriculum);
        log.info("Curriculum created successfully with ID: {}", savedCurriculum.getCurriculumId());

        return curriculumMapper.toCurriculumResponse(savedCurriculum);
    }

    /**
     * Lấy chi tiết curriculum theo ID
     */
    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculumDetail(String id, String accountId) {
        log.info("Fetching curriculum detail for ID: {}", id);

        Curriculum curriculum = curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (RoleName.STUDENT.toString().equals(account.getRole().getRoleName())
                || RoleName.LECTURER.toString().equals(account.getRole().getRoleName())) {
            if (!CurriculumStatus.PUBLISHED.toString().equals(curriculum.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (CurriculumStatus.DRAFT.toString().equals(curriculum.getStatus())) {
            if (!RoleName.HOCFDC.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return curriculumMapper.toCurriculumResponse(curriculum);
    }

    /**
     * Lấy chi tiết curriculum theo Code
     */
    @Transactional(readOnly = true)
    public CurriculumResponse getCurriculumByCode(String code, String accountId) {
        log.info("Fetching curriculum by code: {}", code);

        Curriculum curriculum = curriculumRepository.findByCurriculumCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (RoleName.STUDENT.toString().equals(account.getRole().getRoleName())
                || RoleName.LECTURER.toString().equals(account.getRole().getRoleName())) {
            if (!CurriculumStatus.PUBLISHED.toString().equals(curriculum.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (CurriculumStatus.DRAFT.toString().equals(curriculum.getStatus())) {
            if (!RoleName.HOCFDC.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        return curriculumMapper.toCurriculumResponse(curriculum);
    }

    /**
     * Cập nhật curriculum
     */
    @Transactional
    public CurriculumResponse updateCurriculum(String id,
            CurriculumCreateRequest request, String accountId) {
        log.info("Updating curriculum with ID: {}", id);

        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOCFDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Tìm curriculum hiện tại
        Curriculum curriculum = curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // 2. Kiểm tra nếu đổi code thì không được trùng với code khác
        if (!curriculum.getCurriculumCode().equals(request.getCurriculumCode())) {
            if (curriculumRepository.existsByCurriculumCode(request.getCurriculumCode())) {
                throw new AppException(ErrorCode.CURRICULUM_CODE_EXISTS);
            }
        }

        if (!CurriculumStatus.DRAFT.toString().equals(curriculum.getStatus())) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_DRAFT);
        }

        // 5. Cập nhật các trường
        curriculum.setCurriculumCode(request.getCurriculumCode());
        curriculum.setCurriculumName(request.getCurriculumName());
        curriculum.setStartYear(request.getStartYear());

        // 6. Lưu lại
        Curriculum updatedCurriculum = curriculumRepository.save(curriculum);
        log.info("Curriculum updated successfully: {}", id);

        return curriculumMapper.toCurriculumResponse(updatedCurriculum);
    }

    /**
     * Cập nhật status của curriculum
     */
    @Transactional
    public CurriculumResponse updateCurriculumStatus(String id,
            String status) {
        log.info("Updating curriculum status for ID: {} to {}", id, status);

        Curriculum curriculum = curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
        CurriculumStatus curriculumStatus;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            curriculumStatus = CurriculumStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_CURRICULUM_STATUS);
        }
        curriculum.setStatus(curriculumStatus.toString());
        Curriculum updatedCurriculum = curriculumRepository.save(curriculum);

        return curriculumMapper.toCurriculumResponse(updatedCurriculum);
    }

    /**
     * Cập nhật status của curriculum
     */
    @Transactional
    public CurriculumResponse updateCurriculumEndYear(String id,
            int endYear, String accountId) {
        log.info("Updating curriculum status for ID: {} to {}", id, endYear);

        Curriculum curriculum = curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOCFDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        curriculum.setEndYear(endYear);
        Curriculum updatedCurriculum = curriculumRepository.save(curriculum);

        return curriculumMapper.toCurriculumResponse(updatedCurriculum);
    }

    /**
     * API đồng bộ status của Curriculum, PLOs, Major, POs, Subjects
     */
    @Transactional
    public void syncCurriculumStatus(String curriculumId, String accountId) {

        // Kiểm tra Role
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOCFDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Dựa vào curriculumId lấy lên đối tượng curriculum rồi chuyển sang
        // SYLLABUS_DEVELOP
        Curriculum curriculum = curriculumRepository.findById(UUID.fromString(curriculumId))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
        if(CurriculumStatus.DRAFT.toString().equals(curriculum.getStatus())) {
            curriculum.setStatus(CurriculumStatus.SYLLABUS_DEVELOP.toString());
        } else {
            throw new AppException(ErrorCode.CURRICULUM_NOT_DRAFT);
        }
        curriculumRepository.save(curriculum);

        // 2. Lấy lên list PLO thuộc curriculum đó rồi chuyển status sang
        // INTERNAL_REVIEW
        List<PLOs> plos = plOsRepository.findByCurriculum_CurriculumId(curriculum.getCurriculumId());
        for (PLOs plo : plos) {
            if (PloStatus.DRAFT.toString().equals(plo.getStatus())) {
                plo.setStatus(PloStatus.INTERNAL_REVIEW.toString());
            }
        }
        plOsRepository.saveAll(plos);

        // 3. Dựa vào đối tượng curriculum có majorId, lấy lên đối tượng major đó
        Major major = curriculum.getMajor();
        if (major != null && !PloStatus.PUBLISHED.toString().equals(major.getStatus())) {
            // Nếu là DRAFT thì chuyển sang INTERNAL_REVIEW
            if (PloStatus.DRAFT.toString().equals(major.getStatus())) {
                List<PO> pos = poRepository.findByMajor_MajorId(major.getMajorId());
                for (PO po : pos) {
                    if (PloStatus.DRAFT.toString().equals(po.getStatus())) {
                        po.setStatus(PloStatus.INTERNAL_REVIEW.toString());
                    }
                }
                poRepository.saveAll(pos);

                major.setStatus(PloStatus.INTERNAL_REVIEW.toString());
                majorRepository.save(major);
            }
        }

        // 4. Lấy lên list subject thuộc curriculum. Validate nếu là DRAFT thì chuyển
        // sang WAITING_SYLLABUS
        List<Curriculum_Group_Subject> mappings = curriculumGroupSubjectRepository
                .findAllByCurriculumIdOrderBySemester(curriculum.getCurriculumId());
        List<Subject> subjectsToUpdate = new ArrayList<>();

        for (Curriculum_Group_Subject mapping : mappings) {
            Subject subject = mapping.getSubject();
            if (SubjectStatus.DRAFT.toString().equals(subject.getStatus())) {
                subject.setStatus(SubjectStatus.WAITING_SYLLABUS.toString());
                subjectsToUpdate.add(subject);
            }
        }
        if (!subjectsToUpdate.isEmpty()) {
            subjectRepository.saveAll(subjectsToUpdate);
        }
        Account vpAccount = accountRepository
                .findFirstByRole_RoleName(RoleName.VP.name())
                .stream()
                .findFirst()
                .orElse(null);

        NotificationRequest notifReq = NotificationRequest.builder()
                .title("Major Created")
                .message( "Curriculum: "+ curriculum.getCurriculumCode()
                        + " of Major " + major.getMajorCode()+" been created")
                .type(NotificationType.SYSTEM)
                .accountId(vpAccount.getAccountId())
                .build();
        notificationService.createNotification(notifReq);
    }

    @Transactional
    public void delete(UUID id, String accountId) {
        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOCFDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Curriculum curriculum = curriculumRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        if (CurriculumStatus.DRAFT.toString().equals(curriculum.getStatus())) {
            curriculumRepository.delete(curriculum);
        } else {
            curriculum.setStatus(CurriculumStatus.ARCHIVED.toString());
            curriculumRepository.save(curriculum);
        }
    }

    /**
     * Import Curriculum + PLOs từ Excel.
     * Mỗi dòng gồm: Curriculum Code, Name, Start Year, Description, Major Code, PLO
     * Code, PLO Description.
     * Một Curriculum có thể có nhiều dòng (nhiều PLO). Curriculum Code dùng để nhóm
     * các PLO.
     * Validate:
     * - Curriculum Code đã tồn tại trong DB → báo lỗi, skip toàn bộ dòng có
     * Curriculum Code đó.
     * - PLO Code phải duy nhất (global) → validate trong file và trong DB.
     */
    @Transactional
    public ImportCurriculumResponse importCurriculums(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("Curriculum");
            if (sheet == null)
                sheet = workbook.getSheetAt(0); // fallback
            return importCurriculumFromSheet(sheet);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import curriculum failed: " + e.getMessage());
        }
    }

    @Transactional
    public ImportCurriculumResponse importCurriculumFromSheet(Sheet sheet) {
        List<ImportCurriculumResult> details = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        UUID parsedCurriculumId = null;

        try {
            int curCodeCol = -1, curNameCol = -1, curYearCol = -1, curDescCol = -1, majorCodeCol = -1;
            int ploCodeCol = -1, ploDescCol = -1, poMappingCol = -1;

            String parsedCurCode = null;
            String parsedCurName = null;
            Integer parsedStartYear = null;
            String parsedCurDesc = null;
            String parsedMajorCode = null;

            // Temporary class to hold PLO data
            class PLORowData {
                String ploCode;
                String ploDesc;
                List<PO> mappedPOs = new ArrayList<>();
            }

            List<PLORowData> ploList = new ArrayList<>();
            Set<String> ploCodesInFile = new HashSet<>();

            int state = 0; // 0 = find cur header, 1 = read cur data, 2 = find plo header, 3 = read plo
                           // data

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                if (state == 0) { // find curriculum header
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = getCellValue(row, c, formatter);
                        if (cellVal.equalsIgnoreCase("Curriculum Code"))
                            curCodeCol = c;
                        else if (cellVal.equalsIgnoreCase("Name") || cellVal.equalsIgnoreCase("Curriculum Name"))
                            curNameCol = c;
                        else if (cellVal.equalsIgnoreCase("Start Year"))
                            curYearCol = c;
                        else if (cellVal.equalsIgnoreCase("Description"))
                            curDescCol = c;
                        else if (cellVal.equalsIgnoreCase("Major Code"))
                            majorCodeCol = c;
                    }
                    if (curCodeCol != -1 && majorCodeCol != -1) {
                        state = 1;
                    }
                } else if (state == 1) { // read curriculum data
                    String code = getCellValue(row, curCodeCol, formatter);
                    if (code != null && !code.isEmpty()) {
                        parsedCurCode = code;
                        parsedCurName = curNameCol != -1 ? getCellValue(row, curNameCol, formatter) : null;
                        parsedMajorCode = majorCodeCol != -1 ? getCellValue(row, majorCodeCol, formatter) : null;
                        parsedCurDesc = curDescCol != -1 ? getCellValue(row, curDescCol, formatter) : null;

                        if (curYearCol != -1) {
                            String yearRaw = getCellValue(row, curYearCol, formatter);
                            try {
                                parsedStartYear = Integer.parseInt(yearRaw);
                            } catch (Exception ignored) {
                            }
                        }
                        state = 2;
                    }
                } else if (state == 2) { // find plo header
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = getCellValue(row, c, formatter);
                        if (cellVal.equalsIgnoreCase("PLO Code"))
                            ploCodeCol = c;
                        else if (cellVal.equalsIgnoreCase("Description") || cellVal.equalsIgnoreCase("PLO Description"))
                            ploDescCol = c;
                        else if (cellVal.equalsIgnoreCase("PO Code Mapping"))
                            poMappingCol = c;
                    }
                    if (ploCodeCol != -1) {
                        state = 3;
                    }
                } else if (state == 3) { // read plo data
                    String ploCode = getCellValue(row, ploCodeCol, formatter);
                    if (ploCode != null && !ploCode.isEmpty()) {
                        String ploDesc = ploDescCol != -1 ? getCellValue(row, ploDescCol, formatter) : null;
                        String poMappingRaw = poMappingCol != -1 ? getCellValue(row, poMappingCol, formatter) : "";

                        if (!ploCodesInFile.add(ploCode.toUpperCase())) {
                            details.add(ImportCurriculumResult.builder()
                                    .curriculumCode(parsedCurCode)
                                    .ploCode(ploCode)
                                    .status("FAILED")
                                    .message("Duplicate PLO code in file: " + ploCode)
                                    .build());
                            continue;
                        }

                        PLORowData ploData = new PLORowData();
                        ploData.ploCode = ploCode;
                        ploData.ploDesc = ploDesc;

                        boolean poMappingOk = true;
                        if (!poMappingRaw.isEmpty()) {
                            String[] poCodesArray = poMappingRaw.split(",");
                            for (String pc : poCodesArray) {
                                String cleanPoCode = pc.trim();
                                if (cleanPoCode.isEmpty())
                                    continue;
                                // validate PO Code exists within the current Major
                                java.util.Optional<PO> foundPO = poRepository
                                        .findByPoCodeAndMajor_MajorCode(cleanPoCode, parsedMajorCode);
                                if (foundPO.isEmpty()) {
                                    poMappingOk = false;
                                    details.add(ImportCurriculumResult.builder()
                                            .curriculumCode(parsedCurCode)
                                            .ploCode(ploCode)
                                            .status("FAILED")
                                            .message("PO Code for mapping not found in DB: " + cleanPoCode)
                                            .build());
                                    break;
                                } else {
                                    ploData.mappedPOs.add(foundPO.get());
                                }
                            }
                        }

                        if (poMappingOk) {
                            ploList.add(ploData);
                            details.add(ImportCurriculumResult.builder()
                                    .curriculumCode(parsedCurCode)
                                    .ploCode(ploCode)
                                    .status("SUCCESS")
                                    .message("Will be created")
                                    .build());
                        }
                    }
                }
            }

            if (parsedCurCode == null) {
                details.add(ImportCurriculumResult.builder()
                        .status("FAILED")
                        .message("Curriculum Data not found in sheet")
                        .build());
            } else if (curriculumRepository.existsByCurriculumCode(parsedCurCode)) {
                details.add(ImportCurriculumResult.builder()
                        .curriculumCode(parsedCurCode)
                        .status("FAILED")
                        .message("Curriculum code already exists: " + parsedCurCode)
                        .build());
            } else if (parsedMajorCode == null || !majorRepository.existsByMajorCode(parsedMajorCode)) {
                details.add(ImportCurriculumResult.builder()
                        .curriculumCode(parsedCurCode)
                        .status("FAILED")
                        .message("Major code not found or missing: " + parsedMajorCode)
                        .build());
            } else {
                // Determine if all PLOs are valid
                boolean hasErrors = details.stream().anyMatch(d -> "FAILED".equals(d.getStatus()));
                if (!hasErrors) {
                    Major major = majorRepository.findByMajorCode(parsedMajorCode).orElseThrow();

                    Curriculum curriculum = Curriculum.builder()
                            .curriculumCode(parsedCurCode)
                            .curriculumName(parsedCurName)
                            .description(parsedCurDesc)
                            .startYear(parsedStartYear)
                            .major(major)
                            .status(CurriculumStatus.DRAFT.toString())
                            .build();
                    Curriculum savedCurriculum = curriculumRepository.save(curriculum);
                    parsedCurriculumId = savedCurriculum.getCurriculumId();

                    for (PLORowData pData : ploList) {
                        PLOs plo = PLOs.builder()
                                .ploCode(pData.ploCode)
                                .description(pData.ploDesc)
                                .curriculum(savedCurriculum)
                                .status(PloStatus.DRAFT.toString())
                                .build();
                        PLOs savedPlo = plOsRepository.save(plo);

                        // Save mappings
                        for (PO po : pData.mappedPOs) {
                            PO_PLO_Mapping mapping = new PO_PLO_Mapping();
                            mapping.setPo(po);
                            mapping.setPlo(savedPlo);
                            poPloMappingRepository.save(mapping);
                        }
                    }
                } else {
                    details.add(ImportCurriculumResult.builder()
                            .curriculumCode(parsedCurCode)
                            .status("FAILED")
                            .message("Curriculum not saved due to PLO or PO Mapping errors")
                            .build());
                }
            }

        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION,
                    "Import curriculum from sheet failed: " + e.getMessage());
        }

        int total = details.size();
        int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = total - success;

        return ImportCurriculumResponse.builder()
                .curriculumId(parsedCurriculumId)
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
