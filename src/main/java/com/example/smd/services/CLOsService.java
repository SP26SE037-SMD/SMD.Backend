package com.example.smd.services;

import com.example.smd.dto.request.clo.CLOsCreateRequest;
import com.example.smd.dto.request.clo.CLOsRequest;
import com.example.smd.dto.response.clo.CLOsResponse;
import com.example.smd.dto.response.cloplo.ImportCloPloMappingResponse;
import com.example.smd.dto.response.cloplo.ImportCloPloMappingResult;
import com.example.smd.entities.*;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.RoleName;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CLOsMapper;
import com.example.smd.repositories.*;
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

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CLOsService {

    CLOsRepository closRepository;
    SubjectRepository subjectRepository;
    CurriculumRepository curriculumRepository;
    CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;
    PLOsRepository plOsRepository;
    CloPloMappingRepository cloPloMappingRepository;
    AccountService accountService;
    CLOsMapper closMapper;

    @Transactional
    public List<CLOsResponse> createBulkClos(String subjectId, List<CLOsCreateRequest> requests, String accountId) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Kiểm tra Môn học (Subject) tồn tại
        UUID uuidSubjectId = UUID.fromString(subjectId);
        Subject subject = subjectRepository.findById(uuidSubjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!SubjectStatus.WAITING_SYLLABUS.toString().equals(subject.getStatus())) {
            throw new AppException(ErrorCode.CLO_SUBJECT_NOT_EDITABLE);
        }

        // 2. Check trùng mã CLO ngay trong danh sách gửi lên (Local Check)
        Set<String> uniqueCodes = new HashSet<>();
        for (CLOsCreateRequest req : requests) {
            if (!uniqueCodes.add(req.getCloCode())) {
                throw new AppException(ErrorCode.CLO_CODE_EXISTS);
            }
        }

        // 3. Check trùng mã CLO với Database cho riêng môn học này (Global Check)
        List<String> incomingCodes = requests.stream().map(CLOsCreateRequest::getCloCode).toList();
        if (closRepository.existsByCloCodeInAndSubject_SubjectId(incomingCodes, uuidSubjectId)) {
            throw new AppException(ErrorCode.CLO_CODE_EXISTS);
        }

        // 4. Map và Set các giá trị mặc định
        List<CLOs> closToSave = requests.stream().map(request -> {
            CLOs clo = closMapper.toCloCreate(request); // Đảm bảo Mapper nhận CLOsCreateRequest
            clo.setSubject(subject);
            clo.setStatus("DRAFT");
            return clo;
        }).toList();

        // 5. Lưu hàng loạt và trả về Response
        return closRepository.saveAll(closToSave).stream()
                .map(closMapper::toCloResponse)
                .toList();
    }

    public Page<CLOsResponse> getClosBySubject(String subjectId, int page, int size, String accountId) {
        try {
            // 1. Kiểm tra định dạng UUID và sự tồn tại của Subject
            UUID id = UUID.fromString(subjectId);
            if (!subjectRepository.existsById(id)) {
                throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
            }

            // 3. Lấy thông tin Account và xác định Filter Status
            var account = accountService.getAccountById(accountId);
            String roleName = account.getRole().getRoleName();

            // Mặc định null để Admin/VP/HOCFDC xem được tất cả các trạng thái
            String finalStatus = null;

            // Phân quyền: Học sinh và Giảng viên chỉ được xem CLO đã PUBLISHED
            if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
                finalStatus = "PUBLISHED"; // Hoặc CloStatus.PUBLISHED.toString() nếu bạn có Enum
            }

            // 4. Thiết lập phân trang
            Pageable pageable = PageRequest.of(page, size, Sort.by("cloCode").ascending());

            Page<CLOs> cloPage;
            if (finalStatus != null) {
                // Nhánh lọc theo PUBLISHED cho Student/Lecturer
                cloPage = closRepository.findBySubject_SubjectIdAndStatus(id, finalStatus, pageable);
            } else {
                // Nhánh lấy tất cả cho Role quản lý (Security Check)
                List<String> managerRoles = List.of(RoleName.STUDENT.toString(), RoleName.LECTURER.toString());
                if (managerRoles.contains(roleName)) {
                    throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
                }
                cloPage = closRepository.findBySubject_SubjectId(id, pageable);
            }

            return cloPage.map(closMapper::toCloResponse);

        } catch (IllegalArgumentException e) {
            // Ném lỗi nếu định dạng ID không hợp lệ
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public CLOsResponse updateClo(String id, CLOsRequest request, String accountId) {

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        CLOs clo = closRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

        if (!PloStatus.DRAFT.toString().equals(clo.getStatus())) {
            throw new AppException(ErrorCode.CLO_NOT_EDITABLE);
        }

        // Cập nhật các trường thông tin
        clo.setCloCode(request.getCloCode());
        clo.setDescription(request.getDescription());
        clo.setBloomLevel(request.getBloomLevel());

        return closMapper.toCloResponse(closRepository.save(clo));
    }

    @Transactional
    public void deleteClo(String id, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOPDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }
        UUID cloId = UUID.fromString(id);

        // Kiểm tra xem CLO có tồn tại không
        CLOs clo = closRepository.findById(cloId)
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));
        if ("DRAFT".equals(clo.getStatus())) {
            closRepository.delete(clo);
        } else {
            clo.setStatus("ARCHIVED");
            closRepository.save(clo);
        }

    }

    @Transactional
    public CLOsResponse getCloDetail(String id, String accountId) {
        try {
            UUID cloId = UUID.fromString(id);

            // 1. Tìm CLO (Nên dùng EntityGraph hoặc Fetch Join trong Repo để lấy luôn
            // Subject nếu cần)
            CLOs clo = closRepository.findById(cloId)
                    .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

            // 2. Lấy thông tin Account để phân quyền
            var account = accountService.getAccountById(accountId);
            String roleName = account.getRole().getRoleName();

            // 3. Phân quyền: STUDENT + LECTURER chỉ xem được bản PUBLISHED
            if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
                if (!PloStatus.PUBLISHED.toString().equalsIgnoreCase(clo.getStatus())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
                }
            }

            // 4. Phân quyền: Bản DRAFT chỉ dành cho HOCFDC (Người soạn thảo chính)
            if ("DRAFT".equalsIgnoreCase(clo.getStatus())) {
                if (!RoleName.HOPDC.toString().equals(roleName)) {
                    throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
                }
            }

            // 5. Trả về kết quả sau khi đã qua các bước check
            return closMapper.toCloResponse(clo);

        } catch (IllegalArgumentException e) {
            // Trả về lỗi định dạng ID thay vì lỗi chung chung
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public void updateStatusBySubject(String subjectId, String newStatus) {
        // 1. Kiểm tra trạng thái hợp lệ (Sử dụng SubjectStatus cho đồng bộ)
        PloStatus status;
        try {
            status = PloStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_CLO_STATUS);
        }

        UUID uuidSubjectId = UUID.fromString(subjectId);

        // 2. Kiểm tra môn học có tồn tại không
        if (!subjectRepository.existsById(uuidSubjectId)) {
            throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
        }

        // 3. Cập nhật hàng loạt trạng thái các CLOs thuộc môn học này
        int affectedRows = closRepository.updateStatusBySubjectId(status.toString(), uuidSubjectId);
    }

    @Transactional
    public ImportCloPloMappingResponse importCloPloMapping(MultipartFile file) {
        boolean hasErrors = false;
        DataFormatter formatter = new DataFormatter();
        
        List<ImportCloPloMappingResult> details = new ArrayList<>();
        List<CLOs> newClosToSave = new ArrayList<>();
        List<CLO_PLO_Mapping> mappingsToSave = new ArrayList<>();
        Set<String> processedCloCodes = new HashSet<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheet("CLOs");
            if (sheet == null) {
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Sheet CLOs not found");
            }

            int state = 0; // 0 = find Header 1, 1 = read Header 1, 2 = find Header 2, 3 = read Header 2
            
            int subjCodeCol = -1, minBloomCol = -1, curCodeCol = -1;
            int cloCodeCol = -1, descCol = -1, bloomCol = -1, ploMapCol = -1;
            
            String currentSubjectCode = null;
            Integer currentMinBloom = null;
            String currentCurriculumCode = null;
            Subject currentSubject = null;
            Curriculum currentCurriculum = null;
            boolean header1Valid = false;

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                if (state == 0) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = getCellValue(row, c, formatter);
                        if (cellVal.equalsIgnoreCase("Subject Code")) subjCodeCol = c;
                        else if (cellVal.equalsIgnoreCase("Min Bloom Level")) minBloomCol = c;
                        else if (cellVal.equalsIgnoreCase("Curriculum Code")) curCodeCol = c;
                    }
                    if (subjCodeCol != -1 && minBloomCol != -1 && curCodeCol != -1) state = 1;
                } else if (state == 1) {
                    String sCode = getCellValue(row, subjCodeCol, formatter);
                    if (sCode != null && !sCode.isEmpty()) {
                        currentSubjectCode = sCode;
                        currentMinBloom = parseIntegerSafe(getCellValue(row, minBloomCol, formatter));
                        currentCurriculumCode = getCellValue(row, curCodeCol, formatter);
                        
                        header1Valid = true;
                        // Validate Curriculum
                        currentCurriculum = curriculumRepository.findByCurriculumCode(currentCurriculumCode).orElse(null);
                        if (currentCurriculum == null) {
                            details.add(ImportCloPloMappingResult.builder()
                                    .subjectCode(currentSubjectCode).status("FAILED").message("Curriculum Code not found in DB: " + currentCurriculumCode).build());
                            hasErrors = true;
                            header1Valid = false;
                        }
                        
                        // Validate Subject
                        currentSubject = subjectRepository.findBySubjectCode(currentSubjectCode).orElse(null);
                        if (currentSubject == null) {
                            details.add(ImportCloPloMappingResult.builder()
                                    .subjectCode(currentSubjectCode).status("FAILED").message("Subject Code not found in DB: " + currentSubjectCode).build());
                            hasErrors = true;
                            header1Valid = false;
                        }
                        
                        // Validate association
                        if (header1Valid) {
                            boolean existsAssoc = curriculumGroupSubjectRepository.existsByCurriculumAndSubject(currentCurriculum.getCurriculumId(), currentSubject.getSubjectId());
                            if (!existsAssoc) {
                                details.add(ImportCloPloMappingResult.builder()
                                        .subjectCode(currentSubjectCode).status("FAILED").message("Subject does not belong to Curriculum").build());
                                hasErrors = true;
                                header1Valid = false;
                            }
                        }
                        
                        state = 2;
                    }
                } else if (state == 2) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String cellVal = getCellValue(row, c, formatter);
                        if (cellVal.equalsIgnoreCase("CLO Code")) cloCodeCol = c;
                        else if (cellVal.equalsIgnoreCase("Description")) descCol = c;
                        else if (cellVal.equalsIgnoreCase("Bloom Level")) bloomCol = c;
                        else if (cellVal.equalsIgnoreCase("PLO Code Mapping")) ploMapCol = c;
                    }
                    if (cloCodeCol != -1) state = 3;
                } else if (state == 3) {
                    String cloCode = getCellValue(row, cloCodeCol, formatter);
                    if (cloCode == null || cloCode.isEmpty()) {
                        String firstCell = getCellValue(row, 0, formatter);
                        if (firstCell.equalsIgnoreCase("Subject Code")) {
                            i--; 
                            state = 0;
                            subjCodeCol = -1; minBloomCol = -1; curCodeCol = -1;
                            cloCodeCol = -1; descCol = -1; bloomCol = -1; ploMapCol = -1;
                            continue;
                        }
                        continue; // Skip empty rows
                    }
                    
                    if (!header1Valid) {
                        continue; // Skip processing CLOs if header 1 is invalid
                    }

                    String desc = descCol != -1 ? getCellValue(row, descCol, formatter) : null;
                    Integer bloomLvl = bloomCol != -1 ? parseIntegerSafe(getCellValue(row, bloomCol, formatter)) : null;
                    String ploMappingRaw = ploMapCol != -1 ? getCellValue(row, ploMapCol, formatter) : "";
                    
                    List<String> rowErrors = new ArrayList<>();

                    if (desc == null || desc.trim().isEmpty()) {
                        rowErrors.add("Description cannot be empty");
                    }

                    if (bloomLvl == null || currentMinBloom == null || bloomLvl < currentMinBloom) {
                        rowErrors.add("Bloom Level (" + bloomLvl + ") must be >= Min Bloom Level (" + currentMinBloom + ")");
                    }

                    String uniqueKey = currentSubjectCode.toUpperCase() + "_" + cloCode.toUpperCase();
                    if (!processedCloCodes.add(uniqueKey)) {
                        rowErrors.add("Duplicate CLO Code in file for this subject");
                    }

                    CLOs cloObj = closRepository.findByCloCodeAndSubject_SubjectId(cloCode, currentSubject.getSubjectId()).orElse(null);
                    if (cloObj == null) {
                        cloObj = CLOs.builder()
                                .cloCode(cloCode)
                                .cloName(null)
                                .description(desc)
                                .bloomLevel(String.valueOf(bloomLvl))
                                .subject(currentSubject)
                                .status(com.example.smd.enums.PloStatus.DRAFT.toString())
                                .build();
                        newClosToSave.add(cloObj);
                    }

                    if (!ploMappingRaw.isEmpty()) {
                        String[] ploCodes = ploMappingRaw.split(",");
                        for (String pc : ploCodes) {
                            String cleanPlo = pc.trim();
                            if (cleanPlo.isEmpty()) continue;
                            
                           PLOs ploObj = plOsRepository.findByPloCodeAndCurriculum_CurriculumId(cleanPlo, currentCurriculum.getCurriculumId()).orElse(null);
                            if (ploObj == null) {
                                rowErrors.add("PLO Code '" + cleanPlo + "' not found in Curriculum");
                            } else {
                                boolean mappingExists = false;
                                if (cloObj.getCloId() != null) {
                                    mappingExists = cloPloMappingRepository.existsByClo_CloIdAndPlo_PloId(cloObj.getCloId(), ploObj.getPloId());
                                }
                                if (mappingExists) {
                                    rowErrors.add("Mapping " + cloCode + " -> " + cleanPlo + " already exists in DB");
                                } else {
                                   CLO_PLO_Mapping mapping = CLO_PLO_Mapping.builder()
                                            .clo(cloObj)
                                            .plo(ploObj)
                                            .contributionLevel(null)
                                            .build();
                                    mappingsToSave.add(mapping);
                                }
                            }
                        }
                    }

                    if (!rowErrors.isEmpty()) {
                        details.add(com.example.smd.dto.response.cloplo.ImportCloPloMappingResult.builder()
                                .subjectCode(currentSubjectCode).cloCode(cloCode).status("FAILED")
                                .message(String.join("; ", rowErrors)).build());
                        hasErrors = true;
                    } else {
                        details.add(com.example.smd.dto.response.cloplo.ImportCloPloMappingResult.builder()
                                .subjectCode(currentSubjectCode).cloCode(cloCode).status("SUCCESS").message("Validated").build());
                    }
                }
            }

            if (!hasErrors && !mappingsToSave.isEmpty()) {
                // Save any new CLOs first
                for (CLOs clo : newClosToSave) {
                    Subject subject = subjectRepository.findBySubjectCode(clo.getSubject().getSubjectCode()).orElseThrow();
                    clo.setSubject(subject);
                    closRepository.save(clo);
                }
                
                // Save CLO_PLO mappings
                for (CLO_PLO_Mapping mapping : mappingsToSave) {
                    CLOs clo = closRepository.findByCloCodeAndSubject_SubjectId(
                            mapping.getClo().getCloCode(),
                            subjectRepository.findBySubjectCode(mapping.getClo().getSubject().getSubjectCode()).get().getSubjectId()
                    ).orElseThrow();
                    mapping.setClo(clo);

                    PLOs plo = plOsRepository.findByPloCodeAndCurriculum_CurriculumId(
                            mapping.getPlo().getPloCode(),
                            curriculumRepository.findByCurriculumCode(mapping.getPlo().getCurriculum().getCurriculumCode()).get().getCurriculumId()
                    ).orElseThrow();
                    mapping.setPlo(plo);

                    cloPloMappingRepository.save(mapping);
                }
            }
            
            int total = details.size();
            int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
            return ImportCloPloMappingResponse.builder()
                    .total(total).success(success).failed(total - success).details(details).build();

        } catch (Exception e) {
            log.error("CLO_PLO_Mapping import error", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import failed: " + e.getMessage());
        }
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null || cellIndex == -1) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private Integer parseIntegerSafe(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        try { return Integer.parseInt(value); } catch (Exception e) { return null; }
    }
}
