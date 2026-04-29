package com.example.smd.services;

import com.example.smd.dto.excel.CurriculumGroupSubjectImportDTO;
import com.example.smd.dto.excel.GroupImportDTO;
import com.example.smd.dto.excel.SubjectImportDTO;
import com.example.smd.dto.response.curriculum.ImportFullCurriculumResponse;
import com.example.smd.dto.response.curriculum.ImportCurriculumResponse;
import com.example.smd.dto.response.curriculum.ImportCurriculumResult;
import com.example.smd.dto.response.curriculumgroupsubject.ImportCurriculumGroupSubjectResponse;
import com.example.smd.dto.response.curriculumgroupsubject.ImportCurriculumGroupSubjectResult;
import com.example.smd.dto.response.group.ImportGroupResponse;
import com.example.smd.dto.response.group.ImportGroupResult;
import com.example.smd.dto.response.major.ImportMajorResponse;
import com.example.smd.dto.response.major.ImportMajorResult;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResult;
import com.example.smd.entities.*;
import com.example.smd.enums.CurriculumStatus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.*;
import com.example.smd.services.excelService.ExcelImporter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FullImportService {

    MajorRepository majorRepository;
    POsRepository poRepository;
    CurriculumRepository curriculumRepository;
    PLOsRepository plOsRepository;
    PoPloMappingRepository poPloMappingRepository;
    SubjectRepository subjectRepository;
    DepartmentRepository departmentRepository;
    GroupRepository groupRepository;
    RegulationRepository regulationRepository;
    CurriculumGroupSubjectRepository curriculumGroupSubjectRepository;

    @Transactional
    public ImportFullCurriculumResponse importFullCurriculum(MultipartFile file) {
        ImportFullCurriculumResponse response = ImportFullCurriculumResponse.builder().build();
        boolean hasErrors = false;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            
            // Contexts to store parsed data
            MajorImportContext majorContext = new MajorImportContext();
            CurriculumImportContext curContext = new CurriculumImportContext();
            SubjectImportContext subContext = new SubjectImportContext();
            GroupImportContext groupContext = new GroupImportContext();
            SemesterImportContext semContext = new SemesterImportContext();

            // 1. Parse and Validate Major
            Sheet majorSheet = workbook.getSheet("Major");
            if (majorSheet != null) {
                hasErrors |= parseAndValidateMajor(majorSheet, majorContext);
                response.setMajorResult(buildMajorResponse(majorContext));
            }

            // 2. Parse and Validate Curriculum
            Sheet curriculumSheet = workbook.getSheet("Curriculum");
            if (curriculumSheet != null) {
                hasErrors |= parseAndValidateCurriculum(curriculumSheet, curContext, majorContext);
                response.setCurriculumResult(buildCurriculumResponse(curContext));
            }

            // Initialize Zero-Layer Validation Map
            Map<String, SubjectRegulationDTO> regulationMap = new HashMap<>();
            if (majorContext.parsedMajorCode != null) {
                regulationMap = initZeroLayerValidation(majorContext.parsedMajorCode);
            }

            // 👉 THÊM 3 DÒNG NÀY ĐỂ DEBUG:
            log.info("=== KIỂM TRA DỮ LIỆU REGULATION MAP ===");
            log.info("Số lượng môn học trong Map: " + regulationMap.size());
            log.info("Danh sách các Mã môn (Keys) trong Map: " + regulationMap.keySet());
// ===========================================

            // 3. Parse and Validate Subject
            Sheet subjectSheet = workbook.getSheet("Subject");
            if (subjectSheet != null) {
                hasErrors |= parseAndValidateSubject(subjectSheet, subContext, regulationMap);
                response.setSubjectResult(buildSubjectResponse(subContext));
            }

            // 4. Parse and Validate Group
            Sheet groupSheet = workbook.getSheet("Group");
            if (groupSheet != null) {
                hasErrors |= parseAndValidateGroup(groupSheet, groupContext);
                response.setGroupResult(buildGroupResponse(groupContext));
            }

            // 5. Parse and Validate Semester Mapping
            Sheet semesterSheet = workbook.getSheet("Semester Mapping");
            if (semesterSheet != null) {
                hasErrors |= parseAndValidateSemesterMapping(semesterSheet, semContext, subContext, groupContext, curContext, regulationMap);
                response.setSemesterMappingResult(buildSemesterResponse(semContext));
            }

            // Check if any errors occurred across all sheets
            if (hasErrors) {
                response.setSuccess(false);
                response.setMessage("Validation failed. No data was saved to the database. Please check the results for details.");
                return response; // Transaction will rollback or nothing is saved yet because we only saved to DB at the end
            }

            // -------------------------------------------------------------
            // PHASE 3: INSERT INTO DATABASE
            // -------------------------------------------------------------
            
            // Insert Major & POs
            if (majorContext.parsedMajorCode != null) {
                Major majorToUse;
                if (!majorContext.isExistingMajor) {
                    Major newMajor = Major.builder()
                            .majorCode(majorContext.parsedMajorCode)
                            .majorName(majorContext.parsedMajorName)
                            .description(majorContext.parsedMajorDesc)
                            .status(PloStatus.DRAFT.toString())
                            .build();
                    majorToUse = majorRepository.save(newMajor);
                } else {
                    majorToUse = majorRepository.findByMajorCode(majorContext.parsedMajorCode).orElseThrow();
                }

                for (PO po : majorContext.poListToSave) {
                    po.setMajor(majorToUse);
                }
                if (!majorContext.poListToSave.isEmpty()) {
                    poRepository.saveAll(majorContext.poListToSave);
                }
            }

            // Insert Curriculum, PLOs & PO Mappings
            UUID newCurriculumId = null;
            if (curContext.parsedCurCode != null) {
                Curriculum curToUse;
                if (!curContext.isExistingCurriculum) {
                    Major curMajor = majorRepository.findByMajorCode(curContext.parsedMajorCode).orElseThrow();
                    Curriculum newCur = Curriculum.builder()
                            .curriculumCode(curContext.parsedCurCode)
                            .curriculumName(curContext.parsedCurName)
                            .description(curContext.parsedCurDesc)
                            .startYear(curContext.parsedStartYear)
                            .major(curMajor)
                            .status(CurriculumStatus.DRAFT.toString())
                            .build();
                    curToUse = curriculumRepository.save(newCur);
                } else {
                    curToUse = curriculumRepository.findByCurriculumCode(curContext.parsedCurCode).orElseThrow();
                }
                newCurriculumId = curToUse.getCurriculumId();
                if (response.getCurriculumResult() != null) {
                    response.getCurriculumResult().setCurriculumId(newCurriculumId);
                }

                for (PLORowData pData : curContext.ploListToSave) {
                    PLOs plo = PLOs.builder()
                            .ploCode(pData.ploCode)
                            .description(pData.ploDesc)
                            .curriculum(curToUse)
                            .status(PloStatus.DRAFT.toString())
                            .build();
                    PLOs savedPlo = plOsRepository.save(plo);
                    
                    for (PO po : pData.mappedPOs) {
                        PO_PLO_Mapping mapping = new PO_PLO_Mapping();
                        mapping.setPo(po);
                        mapping.setPlo(savedPlo);
                        poPloMappingRepository.save(mapping);
                    }
                }
            }

            // Insert Subjects
            if (!subContext.subjectsToSave.isEmpty()) {
                subjectRepository.saveAll(subContext.subjectsToSave);
            }

            // Insert Groups
            if (!groupContext.groupsToSave.isEmpty()) {
                groupRepository.saveAll(groupContext.groupsToSave);
            }

            // Insert Semester Mappings
            if (!semContext.mappingsToSave.isEmpty() && newCurriculumId != null) {
                Curriculum c = curriculumRepository.findById(newCurriculumId).orElseThrow();
                for (Curriculum_Group_Subject entity : semContext.mappingsToSave) {
                    entity.setCurriculum(c);
                    
                    // The subject might have been just saved, let's fetch it from DB to ensure valid reference
                    Subject s = subjectRepository.findBySubjectCode(entity.getSubject().getSubjectCode()).orElseThrow();
                    entity.setSubject(s);
                    
                    if (entity.getGroup() != null && entity.getGroup().getGroupCode() != null) {
                        Group g = groupRepository.findByGroupCode(entity.getGroup().getGroupCode()).orElseThrow();
                        entity.setGroup(g);
                    } else {
                        entity.setGroup(null);
                    }
                }
                curriculumGroupSubjectRepository.saveAll(semContext.mappingsToSave);
            }

            response.setSuccess(true);
            response.setMessage("Full import completed and data saved successfully.");

        } catch (Exception e) {
            log.error("Full import error", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Full import failed: " + e.getMessage());
        }

        return response;
    }

    // ==========================================
    // PARSE & VALIDATE METHODS
    // ==========================================

    private boolean parseAndValidateMajor(Sheet sheet, MajorImportContext ctx) {
        boolean hasErrors = false;
        DataFormatter formatter = new DataFormatter();
        int majorCodeCol = -1, majorNameCol = -1, majorDescCol = -1, poCodeCol = -1, poDescCol = -1;
        int state = 0; // 0 = find major header, 1 = read major data, 2 = find po header, 3 = read po data

        Set<String> poCodesInFile = new HashSet<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            if (state == 0) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row, c, formatter);
                    if (cellVal.equalsIgnoreCase("Major Code")) majorCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Name") || cellVal.equalsIgnoreCase("Major Name")) majorNameCol = c;
                    else if (cellVal.equalsIgnoreCase("Description")) majorDescCol = c;
                }
                if (majorCodeCol != -1 && majorNameCol != -1) state = 1;
            } else if (state == 1) {
                String code = getCellValue(row, majorCodeCol, formatter);
                if (code != null && !code.isEmpty()) {
                    ctx.parsedMajorCode = code;
                    ctx.parsedMajorName = getCellValue(row, majorNameCol, formatter);
                    ctx.parsedMajorDesc = majorDescCol != -1 ? getCellValue(row, majorDescCol, formatter) : null;
                    state = 2;
                }
            } else if (state == 2) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row, c, formatter);
                    if (cellVal.equalsIgnoreCase("PO Code")) poCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Description") || cellVal.equalsIgnoreCase("PO Description")) poDescCol = c;
                }
                if (poCodeCol != -1) state = 3;
            } else if (state == 3) {
                String poCode = getCellValue(row, poCodeCol, formatter);
                if (poCode != null && !poCode.isEmpty()) {
                    String poDesc = poDescCol != -1 ? getCellValue(row, poDescCol, formatter) : null;
                    
                    if (poDesc == null || poDesc.trim().isEmpty()) {
                        ctx.details.add(ImportMajorResult.builder()
                                .majorCode(ctx.parsedMajorCode)
                                .poCode(poCode)
                                .status("FAILED")
                                .message("PO Description cannot be empty")
                                .build());
                        hasErrors = true;
                        continue;
                    }

                    if (!poCodesInFile.add(poCode.toUpperCase())) {
                        ctx.details.add(ImportMajorResult.builder()
                                .majorCode(ctx.parsedMajorCode)
                                .poCode(poCode)
                                .status("FAILED")
                                .message("Duplicate PO code in file: " + poCode)
                                .build());
                        hasErrors = true;
                        continue;
                    }

                    PO po = PO.builder()
                            .poCode(poCode)
                            .description(poDesc)
                            .status(PloStatus.DRAFT.toString())
                            .build();
                    ctx.poListToSave.add(po);
                    
                    ctx.details.add(ImportMajorResult.builder()
                            .majorCode(ctx.parsedMajorCode)
                            .poCode(poCode)
                            .status("SUCCESS")
                            .message("Validated")
                            .build());
                }
            }
        }

        if (ctx.parsedMajorCode == null) {
            ctx.details.add(ImportMajorResult.builder().status("FAILED").message("Major Data not found in sheet").build());
            return true;
        }

        if (majorRepository.existsByMajorCode(ctx.parsedMajorCode)) {
            ctx.isExistingMajor = true;
            // Existing major: DO NOT throw error. Just use the existing one, but validate/import POs from the sheet as requested.
            // If the major exists but we still want to add POs from the sheet to it, it is valid based on user requirements.
        } else if (ctx.parsedMajorName == null || ctx.parsedMajorName.isEmpty()) {
            ctx.details.add(ImportMajorResult.builder().majorCode(ctx.parsedMajorCode).status("FAILED").message("Missing required field: Name").build());
            hasErrors = true;
        }

        return hasErrors;
    }

    private boolean parseAndValidateCurriculum(Sheet sheet, CurriculumImportContext ctx, MajorImportContext majorContext) {
        boolean hasErrors = false;
        DataFormatter formatter = new DataFormatter();
        int curCodeCol = -1, curNameCol = -1, curYearCol = -1, curDescCol = -1, majorCodeCol = -1;
        int ploCodeCol = -1, ploDescCol = -1, poMappingCol = -1;
        int state = 0; 
        Set<String> ploCodesInFile = new HashSet<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            // STATE 0: Tìm Header của Curriculum
            if (state == 0) { 
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row, c, formatter);
                    if (cellVal.equalsIgnoreCase("Curriculum Code")) curCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Name") || cellVal.equalsIgnoreCase("Curriculum Name")) curNameCol = c;
                    else if (cellVal.equalsIgnoreCase("Start Year")) curYearCol = c;
                    else if (cellVal.equalsIgnoreCase("Description")) curDescCol = c;
                    else if (cellVal.equalsIgnoreCase("Major Code")) majorCodeCol = c;
                }
                if (curCodeCol != -1 && majorCodeCol != -1) state = 1;
            }
            // STATE 1: Đọc thông tin Curriculum và Validate Major Code
            else if (state == 1) {
                String code = getCellValue(row, curCodeCol, formatter);
                if (code != null && !code.isEmpty()) {
                    ctx.parsedCurCode = code;
                    ctx.parsedMajorCode = majorCodeCol != -1 ? getCellValue(row, majorCodeCol, formatter) : null;
                    if (majorContext.parsedMajorCode == null) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .status("FAILED")
                                .message("Lỗi hệ thống: Không tìm thấy dữ liệu từ sheet Major để đối chiếu")
                                .build());
                        return true; // Dừng toàn bộ sheet này
                    }

                    // So sánh mã ngành giữa 2 sheet
                    if (!majorContext.parsedMajorCode.equalsIgnoreCase(ctx.parsedMajorCode)) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode)
                                .status("FAILED")
                                .message("Major Code bên sheet Curriculum [" + ctx.parsedMajorCode +
                                        "] không khớp với Major Code bên sheet Major [" + majorContext.parsedMajorCode + "]")
                                .build());

                        // Theo yêu cầu: Không khớp thì không cần check PO mapping
                        return true; // Thoát hàm validate sheet Curriculum ngay lập tức
                    }

                    ctx.parsedCurName = curNameCol != -1 ? getCellValue(row, curNameCol, formatter) : null;
                    ctx.parsedCurDesc = curDescCol != -1 ? getCellValue(row, curDescCol, formatter) : null;
                    if (curYearCol != -1) {
                        String yearRaw = getCellValue(row, curYearCol, formatter);
                        try { ctx.parsedStartYear = Integer.parseInt(yearRaw); } catch (Exception ignored) {}
                    }
                    state = 2;
                }
            } else if (state == 2) { 
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row, c, formatter);
                    if (cellVal.equalsIgnoreCase("PLO Code")) ploCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Description") || cellVal.equalsIgnoreCase("PLO Description")) ploDescCol = c;
                    else if (cellVal.equalsIgnoreCase("PO Code Mapping")) poMappingCol = c;
                }
                if (ploCodeCol != -1) state = 3;
            } else if (state == 3) { 
                String ploCode = getCellValue(row, ploCodeCol, formatter);
                if (ploCode != null && !ploCode.isEmpty()) {
                    String ploDesc = ploDescCol != -1 ? getCellValue(row, ploDescCol, formatter) : null;
                    String poMappingRaw = poMappingCol != -1 ? getCellValue(row, poMappingCol, formatter) : "";
                    
                    if (ploDesc == null || ploDesc.trim().isEmpty()) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode).ploCode(ploCode).status("FAILED")
                                .message("PLO Description cannot be empty").build());
                        hasErrors = true;
                        continue;
                    }

                    if (!ploCodesInFile.add(ploCode.toUpperCase())) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode).ploCode(ploCode).status("FAILED")
                                .message("Duplicate PLO code in file: " + ploCode).build());
                        hasErrors = true;
                        continue;
                    }
                    
                    PLORowData ploData = new PLORowData();
                    ploData.ploCode = ploCode;
                    ploData.ploDesc = ploDesc;
                    
                    boolean poMappingOk = true;
                    List<String> mappingErrors = new ArrayList<>();

                    if (!poMappingRaw.isEmpty()) {
                        String[] poCodesArray = poMappingRaw.split(",");
                        for (String pc : poCodesArray) {
                            String cleanPoCode = pc.trim();
                            if (cleanPoCode.isEmpty()) continue;
                            
                            // Check if this PO is being imported right now in Major sheet
                            boolean foundInSheet = majorContext.poListToSave.stream()
                                    .anyMatch(po -> po.getPoCode().equalsIgnoreCase(cleanPoCode) 
                                              && majorContext.parsedMajorCode.equalsIgnoreCase(ctx.parsedMajorCode));
                            
                            // If not in sheet, check in DB
                            Optional<PO> foundPOInDB = poRepository.findByPoCodeAndMajor_MajorCode(cleanPoCode, ctx.parsedMajorCode);

                            if (!foundInSheet && foundPOInDB.isEmpty()) {
                                poMappingOk = false;
                                mappingErrors.add(cleanPoCode + " not found in Major DB nor in Major import sheet");
                            } else {
                                if (foundPOInDB.isPresent()) {
                                    ploData.mappedPOs.add(foundPOInDB.get());
                                } else {
                                    // PO is in sheet, but since it's not saved yet, we create a temporary reference.
                                    // We will link it properly during insertion phase or assume it's created.
                                    // We just need to store it so it can be mapped later.
                                    PO sheetPo = majorContext.poListToSave.stream()
                                            .filter(po -> po.getPoCode().equalsIgnoreCase(cleanPoCode))
                                            .findFirst().orElse(null);
                                    if (sheetPo != null) ploData.mappedPOs.add(sheetPo);
                                }
                            }
                        }
                    }
                    
                    if (!poMappingOk) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode).ploCode(ploCode).status("FAILED")
                                .message("PLO " + ploCode + " mapping errors: " + String.join(", ", mappingErrors)).build());
                        hasErrors = true;
                    } else {
                        ctx.ploListToSave.add(ploData);
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode).ploCode(ploCode).status("SUCCESS")
                                .message("Validated").build());
                    }
                }
            }
        }

        if (ctx.parsedCurCode == null) {
            ctx.details.add(ImportCurriculumResult.builder().status("FAILED").message("Curriculum Data not found in sheet").build());
            return true;
        }

        if (curriculumRepository.existsByCurriculumCode(ctx.parsedCurCode)) {
            ctx.isExistingCurriculum = true;
            // Existing Curriculum: We do not error out. We append PLOs based on user requirements.
        }

        if (ctx.parsedMajorCode == null || (!majorContext.parsedMajorCode.equalsIgnoreCase(ctx.parsedMajorCode) && !majorRepository.existsByMajorCode(ctx.parsedMajorCode))) {
            ctx.details.add(ImportCurriculumResult.builder()
                    .curriculumCode(ctx.parsedCurCode).status("FAILED")
                    .message("Major code not found in DB or import sheet: " + ctx.parsedMajorCode).build());
            hasErrors = true;
        }

        return hasErrors;
    }

    private boolean parseAndValidateSubject(Sheet sheet, SubjectImportContext ctx, Map<String, SubjectRegulationDTO> regulationMap) {

        boolean hasErrors = false;
        Set<String> subjectCodesInFile = new HashSet<>();
        
        try {
            List<SubjectImportDTO> rows = ExcelImporter.importFromSheet(sheet, SubjectImportDTO.class);
            for (SubjectImportDTO row : rows) {
                String subjectCode = trim(row.getSubjectCode());
                String subjectName = trim(row.getSubjectName());
                String departmentCode = trim(row.getDepartmentCode());

                List<String> missingCols = new ArrayList<>();
                if (subjectCode == null) {
                    missingCols.add("Subject Code");
                } else{
                    if (subjectCode.equalsIgnoreCase("N/A")) {
                        subjectCode = generateNASubjectCode(subjectName);
                    }
                    ctx.fileSubjectCodes.add(subjectCode.toUpperCase());
                }
                if (subjectName == null) missingCols.add("Subject Name");
                if (departmentCode == null) missingCols.add("Department Code");
                if (trim(row.getCredits()) == null) missingCols.add("Credits");
                if (trim(row.getTimeAllocation()) == null) missingCols.add("Time Allocation");
                if (trim(row.getMinToPass()) == null) missingCols.add("Min to pass");
                if (trim(row.getStudentLimit()) == null) missingCols.add("Student Limit");
                if (trim(row.getStudentTasks()) == null) missingCols.add("Student Tasks");
                if (trim(row.getScoringScale()) == null) missingCols.add("Scoring Scale");
                if (trim(row.getMinBloomLevel()) == null) missingCols.add("Min Bloom Level");

                if (!missingCols.isEmpty()) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("FAILED")
                            .message("Subject " + (subjectCode != null ? subjectCode : "Unknown") + " missing columns: " + String.join(", ", missingCols))
                            .build());
                    hasErrors = true;
                    continue;
                }

                if (subjectCode.equalsIgnoreCase("N/A")) {
                    subjectCode = generateNASubjectCode(subjectName);
                }

                if (!regulationMap.containsKey(subjectCode.toUpperCase())) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("FAILED")
                            .message("Mã môn học [" + subjectCode + "] không nằm trong chương trình khung quy định")
                            .build());
                    hasErrors = true;
                    continue;
                }

                SubjectRegulationDTO regDto = regulationMap.get(subjectCode.toUpperCase());
                Integer excelCredits = parseIntegerSafe(row.getCredits());
                Integer theory = parseIntegerSafe(row.getTheoryPeriods());
                Integer practical = parseIntegerSafe(row.getPracticalPeriods());
                Integer selfStudy = parseIntegerSafe(row.getSelfStudyPeriods());

                List<String> mismatchErrors = new ArrayList<>();
                if (regDto.credit != null && !regDto.credit.equals(excelCredits)) {
                    mismatchErrors.add("Credit (Chuẩn: " + regDto.credit + ", Excel: " + excelCredits + ")");
                }
                if (regDto.theoryPeriod != null && !regDto.theoryPeriod.equals(theory)) {
                    mismatchErrors.add("Theory (Chuẩn: " + regDto.theoryPeriod + ", Excel: " + theory + ")");
                }
                if (regDto.practicalPeriod != null && !regDto.practicalPeriod.equals(practical)) {
                    mismatchErrors.add("Practical (Chuẩn: " + regDto.practicalPeriod + ", Excel: " + practical + ")");
                }
                if (regDto.selfStudyPeriod != null && !regDto.selfStudyPeriod.equals(selfStudy)) {
                    mismatchErrors.add("SelfStudy (Chuẩn: " + regDto.selfStudyPeriod + ", Excel: " + selfStudy + ")");
                }

                if (!mismatchErrors.isEmpty()) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("FAILED")
                            .message("Sai thông số quy định: " + String.join(", ", mismatchErrors))
                            .build());
                    hasErrors = true;
                    continue;
                }

               //comment do thử để chỗ khác
//                ctx.fileSubjectCodes.add(subjectCode.toUpperCase());
                if (!subjectCodesInFile.add(subjectCode.toUpperCase())) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("FAILED").message("Duplicate subjectCode in file").build());
                    hasErrors = true;
                    continue;
                }

                if (subjectRepository.existsBySubjectCode(subjectCode)) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("SUCCESS").message("Skipped: Already exists in DB").build());
                    continue; // SKIP Insert
                }

                Department department = departmentRepository.findByDepartmentCode(departmentCode).orElse(null);
                if (department == null) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("FAILED").message("Department code not found in DB: " + departmentCode).build());
                    hasErrors = true;
                    continue;
                }

                Integer credits;
                try {
                    credits = Integer.parseInt(row.getCredits().trim());
                } catch (NumberFormatException e) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("FAILED").message("Credits must be a valid number").build());
                    hasErrors = true;
                    continue;
                }

                Subject subject = Subject.builder()
                        .subjectCode(subjectCode)
                        .subjectName(subjectName)
                        .credits(credits)
                        .degreeLevel(trim(row.getDegreeLevel()))
                        .timeAllocation(trim(row.getTimeAllocation()))
                        .description(trim(row.getDescription()))
                        .department(department)
                        .minToPass(parseIntegerSafe(row.getMinToPass()))
                        .studentLimit(parseIntegerSafe(row.getStudentLimit()))
                        .studentTasks(trim(row.getStudentTasks()))
                        .scoringScale(parseIntegerSafe(row.getScoringScale()))
                        .theoryPeriods(parseIntegerSafe(row.getTheoryPeriods()))
                        .practicalPeriods(parseIntegerSafe(row.getPracticalPeriods()))
                        .selfStudyPeriods(parseIntegerSafe(row.getSelfStudyPeriods()))
                        .status(SubjectStatus.DRAFT.toString())
                        .createdAt(Instant.now())
                        .build();

                ctx.subjectsToSave.add(subject);
                ctx.details.add(ImportSubjectResult.builder()
                        .subjectCode(subjectCode).status("SUCCESS").message("Validated").build());
            }
        } catch (Exception e) {
            log.error("Subject parse error", e);
            hasErrors = true;
        }
        return hasErrors;
    }

    private boolean parseAndValidateGroup(Sheet sheet, GroupImportContext ctx) {
        boolean hasErrors = false;
        Set<String> groupCodesInFile = new HashSet<>();
        try {
            List<GroupImportDTO> rows = ExcelImporter.importFromSheet(sheet, GroupImportDTO.class);
            for (GroupImportDTO row : rows) {
                String groupCode = trim(row.getGroupCode());
                String groupName = trim(row.getGroupName());
                
                List<String> missingCols = new ArrayList<>();
                if (groupCode == null) missingCols.add("Group Code");
                if (groupName == null) missingCols.add("Group Name");

                if (!missingCols.isEmpty()) {
                    ctx.details.add(ImportGroupResult.builder()
                            .groupCode(groupCode).status("FAILED")
                            .message("Group " + (groupCode != null ? groupCode : "Unknown") + " missing columns: " + String.join(", ", missingCols))
                            .build());
                    hasErrors = true;
                    continue;
                }

                if (!groupCodesInFile.add(groupCode.toUpperCase())) {
                    ctx.details.add(ImportGroupResult.builder()
                            .groupCode(groupCode).status("FAILED").message("Duplicate group code in file").build());
                    hasErrors = true;
                    continue;
                }
                ctx.fileGroupCodes.add(groupCode.toUpperCase());

                if (groupRepository.existsByGroupCode(groupCode)) {
                    ctx.details.add(ImportGroupResult.builder()
                            .groupCode(groupCode).status("SUCCESS").message("Skipped: Already exists in DB").build());
                    continue; // SKIP Insert
                }

                Group group = Group.builder()
                        .groupCode(groupCode)
                        .groupName(groupName)
                        .description(trim(row.getDescription()))
                        .type(trim(row.getType()))
                        .createdAt(Instant.now())
                        .build();

                ctx.groupsToSave.add(group);
                ctx.details.add(ImportGroupResult.builder()
                        .groupCode(groupCode).status("SUCCESS").message("Validated").build());
            }
        } catch (Exception e) {
            log.error("Group parse error", e);
            hasErrors = true;
        }
        return hasErrors;
    }

    private boolean parseAndValidateSemesterMapping(Sheet sheet, SemesterImportContext ctx, SubjectImportContext subCtx, GroupImportContext grpCtx, CurriculumImportContext curCtx, Map<String, SubjectRegulationDTO> regulationMap) {
        boolean hasErrors = false;
        Set<String> subjectCodesInFile = new HashSet<>();
        
        // Find existing curriculum subjects to prevent duplicates if modifying an existing curriculum
        // Removed DB checks as requested.

        try {
            List<CurriculumGroupSubjectImportDTO> rows = ExcelImporter.importFromSheet(sheet, CurriculumGroupSubjectImportDTO.class);
            for (int i = 0; i < rows.size(); i++) {
                int rowNumber = i + 2;
                CurriculumGroupSubjectImportDTO row = rows.get(i);
                
                String groupCode = trim(row.getGroupCode());
                String subjectCode = trim(row.getSubjectCode());
                String semesterRaw = trim(row.getSemester());

                if (subjectCode == null || semesterRaw == null) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Missing required fields: Subject Code, Semester"));
                    hasErrors = true;
                    continue;
                }


                if (!subjectCodesInFile.add(subjectCode.toUpperCase())) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Duplicate subject in mapping file"));
                    hasErrors = true;
                    continue;
                }

                int semesterNo;
                try {
                    semesterNo = Integer.parseInt(semesterRaw);
                    if (semesterNo <= 0) throw new Exception();
                } catch (Exception e) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Invalid semester value"));
                    hasErrors = true;
                    continue;
                }

                // Verify Subject exists in Subject sheet
                if (!subCtx.fileSubjectCodes.contains(subjectCode.toUpperCase())) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Subject Code này không có bên Sheet Subject"));
                    hasErrors = true;
                    continue;
                }

                // Verify Group exists in Group sheet (if provided)
                if (groupCode != null && !groupCode.isEmpty()) {
                    if (!grpCtx.fileGroupCodes.contains(groupCode.toUpperCase())) {
                        ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Group Code này không có bên Sheet Group"));
                        hasErrors = true;
                        continue;
                    }
                }

                // ZERO-LAYER VALIDATION for Semester
                if (regulationMap.containsKey(subjectCode.toUpperCase())) {
                    SubjectRegulationDTO regDto = regulationMap.get(subjectCode.toUpperCase());
                    if (regDto.semester != null && !regDto.semester.equals(semesterNo)) {
                        ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Sai học kỳ quy định (Chuẩn: " + regDto.semester + ", Excel: " + semesterNo + ")"));
                        hasErrors = true;
                        continue;
                    }
                }

                Curriculum_Group_Subject mapping = Curriculum_Group_Subject.builder()
                        .semester(semesterNo)
                        // Temporary dummy subjects/groups to hold the code, replaced during insertion phase
                        .subject(Subject.builder().subjectCode(subjectCode).build()) 
                        .group(groupCode != null ? Group.builder().groupCode(groupCode).build() : null)
                        .build();

                ctx.mappingsToSave.add(mapping);
                ctx.details.add(ImportCurriculumGroupSubjectResult.builder()
                        .rowNumber(rowNumber).groupCode(groupCode).subjectCode(subjectCode).semester(semesterRaw)
                        .status("SUCCESS").message("Validated").build());
            }
        } catch (Exception e) {
            log.error("Semester mapping parse error", e);
            hasErrors = true;
        }
        return hasErrors;
    }

    // ==========================================
    // UTILS & RESPONSE BUILDERS
    // ==========================================

    private Map<String, SubjectRegulationDTO> initZeroLayerValidation(String majorCode) {
        Map<String, SubjectRegulationDTO> map = new HashMap<>();
        if (majorCode == null || majorCode.isEmpty()) return map;

        Major major = majorRepository.findByMajorCode(majorCode).orElse(null);
        if (major == null) return map;

        Regulation regulation = regulationRepository.findByCodeAndMajor_MajorId("COURSE_MAPPING", major.getMajorId()).orElse(null);
        if (regulation == null || regulation.getValue() == null) return map;

        String value = regulation.getValue();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(.*?)\\s*\\(([^)]+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            String namePart = matcher.group(1).trim();
            if (namePart.startsWith(",")) {
                namePart = namePart.substring(1).trim();
            }
            String dataPart = matcher.group(2).trim();
            String[] dataFields = dataPart.split("\\|");
            if (dataFields.length >= 5) {
                String code = dataFields[0].trim();
                Integer credit = parseIntegerSafe(dataFields[1]);
                Integer theory = parseIntegerSafe(dataFields[2]);
                Integer practical = parseIntegerSafe(dataFields[3]);
                Integer selfStudy = parseIntegerSafe(dataFields[4]);
                Integer semester = null;
                if (dataFields.length >= 6) {
                    semester = parseIntegerSafe(dataFields[5]);
                }
                
                if (code.equalsIgnoreCase("N/A")) {
                    code = generateNASubjectCode(namePart);
                }
                
                SubjectRegulationDTO dto = new SubjectRegulationDTO(code, namePart, semester, credit, theory, practical, selfStudy);
                map.put(code.toUpperCase(), dto);
            }
        }
        return map;
    }

    private String generateNASubjectCode(String subjectName) {
        if (subjectName == null || subjectName.trim().isEmpty()) return "N/A_UNKNOWN";
        String normalized = java.text.Normalizer.normalize(subjectName.trim(), java.text.Normalizer.Form.NFD);
        String noDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String noSpaces = noDiacritics.replaceAll("[^a-zA-Z0-9]", "");
        return "N/A_" + noSpaces;
    }

    private ImportCurriculumGroupSubjectResult buildSemFail(int rowNumber, String groupCode, String subjectCode, String semester, String message) {
        return ImportCurriculumGroupSubjectResult.builder()
                .rowNumber(rowNumber).groupCode(groupCode).subjectCode(subjectCode).semester(semester)
                .status("FAILED").message(message).build();
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null || cellIndex == -1) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseIntegerSafe(String raw) {
        String value = trim(raw);
        if (value == null) return null;
        try { return Integer.parseInt(value); } catch (Exception e) { return null; }
    }

    private ImportMajorResponse buildMajorResponse(MajorImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportMajorResponse.builder().total(total).success(success).failed(total - success).details(ctx.details).build();
    }

    private ImportCurriculumResponse buildCurriculumResponse(CurriculumImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportCurriculumResponse.builder().total(total).success(success).failed(total - success).details(ctx.details).build();
    }

    private ImportSubjectResponse buildSubjectResponse(SubjectImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportSubjectResponse.builder().total(total).success(success).failed(total - success).details(ctx.details).build();
    }

    private ImportGroupResponse buildGroupResponse(GroupImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportGroupResponse.builder().total(total).success(success).failed(total - success).details(ctx.details).build();
    }

    private ImportCurriculumGroupSubjectResponse buildSemesterResponse(SemesterImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportCurriculumGroupSubjectResponse.builder().total(total).success(success).failed(total - success).details(ctx.details).build();
    }

    // ==========================================
    // INTERNAL CONTEXT CLASSES
    // ==========================================

    private static class SubjectRegulationDTO {
        String subjectCode;
        String subjectName;
        Integer semester;
        Integer credit;
        Integer theoryPeriod;
        Integer practicalPeriod;
        Integer selfStudyPeriod;

        public SubjectRegulationDTO(String subjectCode, String subjectName, Integer semester, Integer credit, Integer theoryPeriod, Integer practicalPeriod, Integer selfStudyPeriod) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.semester = semester;
            this.credit = credit;
            this.theoryPeriod = theoryPeriod;
            this.practicalPeriod = practicalPeriod;
            this.selfStudyPeriod = selfStudyPeriod;
        }
    }

    private static class MajorImportContext {
        String parsedMajorCode;
        String parsedMajorName;
        String parsedMajorDesc;
        boolean isExistingMajor = false;
        List<PO> poListToSave = new ArrayList<>();
        List<ImportMajorResult> details = new ArrayList<>();
    }

    private static class CurriculumImportContext {
        String parsedCurCode;
        String parsedCurName;
        String parsedMajorCode;
        String parsedCurDesc;
        Integer parsedStartYear;
        boolean isExistingCurriculum = false;
        List<PLORowData> ploListToSave = new ArrayList<>();
        List<ImportCurriculumResult> details = new ArrayList<>();
    }

    private static class PLORowData {
        String ploCode;
        String ploDesc;
        List<PO> mappedPOs = new ArrayList<>();
    }

    private static class SubjectImportContext {
        Set<String> fileSubjectCodes = new HashSet<>();
        List<Subject> subjectsToSave = new ArrayList<>();
        List<ImportSubjectResult> details = new ArrayList<>();
    }

    private static class GroupImportContext {
        Set<String> fileGroupCodes = new HashSet<>();
        List<Group> groupsToSave = new ArrayList<>();
        List<ImportGroupResult> details = new ArrayList<>();
    }

    private static class SemesterImportContext {
        List<Curriculum_Group_Subject> mappingsToSave = new ArrayList<>();
        List<ImportCurriculumGroupSubjectResult> details = new ArrayList<>();
    }
}
