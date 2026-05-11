package com.example.smd.services;

import com.example.smd.dto.excel.CurriculumGroupSubjectImportDTO;
import com.example.smd.dto.excel.GroupImportDTO;
import com.example.smd.dto.excel.SourceImportDTO;
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
import com.example.smd.dto.response.source.ImportSourceResponse;
import com.example.smd.dto.response.source.ImportSourceResult;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import com.example.smd.dto.response.subject.ImportSubjectResult;
import com.example.smd.entities.*;
import com.example.smd.enums.CurriculumStatus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SourceType;
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

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;

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
    SourceRepository sourceRepository;
    ProposedSourceRepository proposedSourceRepository;

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
            SourceImportContext srcContext = new SourceImportContext();

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
                hasErrors |= parseAndValidateSemesterMapping(semesterSheet, semContext, subContext, groupContext,
                        curContext, regulationMap);
                response.setSemesterMappingResult(buildSemesterResponse(semContext));
            }

            // Initialize Source Zero-Layer Validation Map
            Map<String, SourceRegulationDTO> sourceRegulationMap = new HashMap<>();
            if (majorContext.parsedMajorCode != null) {
                sourceRegulationMap = initSourceZeroLayerValidation(majorContext.parsedMajorCode);
            }
            // 👉 THÊM 3 DÒNG NÀY ĐỂ DEBUG:
            log.info("=== KIỂM TRA DỮ LIỆU REGULATION SOURCE ===");
            log.info("Số lượng môn học trong Map: " + sourceRegulationMap.size());
            log.info("Danh sách các Mã môn (Keys) trong Map: " + sourceRegulationMap.keySet());

            // 6. Parse and Validate Source
            Sheet sourceSheet = workbook.getSheet("Source");
            if (sourceSheet != null) {
                hasErrors |= parseAndValidateSource(sourceSheet, srcContext, subContext, sourceRegulationMap);
                response.setSourceResult(buildSourceResponse(srcContext));
            }

            // Check if any errors occurred across all sheets
            if (hasErrors) {
                response.setSuccess(false);
                response.setMessage(
                        "Validation failed. No data was saved to the database. Please check the results for details.");
                return response; // Transaction will rollback or nothing is saved yet because we only saved to DB
                                 // at the end
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

                List<PO> validPosToSave = new ArrayList<>();
                for (PO po : majorContext.poListToSave) {
                    // Kiểm tra xem PO Code này đã thuộc về Major này trong DB chưa
                    Optional<PO> existingPo = poRepository.findByPoCodeAndMajor_MajorCode(po.getPoCode(), majorContext.parsedMajorCode);

                    if (existingPo.isEmpty()) {
                        po.setMajor(majorToUse); // Gán Major gốc
                        validPosToSave.add(po);  // Đưa vào danh sách cần lưu
                    }
                }

                // Chỉ lưu những PO mới
                if (!validPosToSave.isEmpty()) {
                    poRepository.saveAll(validPosToSave);
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

                    // The subject might have been just saved, let's fetch it from DB to ensure
                    // valid reference
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

            // Insert Sources & ProposedSources
            if (!srcContext.rowsToSave.isEmpty()) {
                for (SourceImportDTO row : srcContext.rowsToSave) {
                    String srcCode = trim(row.getSourceCode());

                    Source source;
                    if (!sourceRepository.existsBySourceCode(srcCode)) {
                        source = Source.builder()
                                .sourceCode(srcCode)
                                .sourceName(trim(row.getSourceName()))
                                .type(SourceType.REFERENCE_BOOK.name())
                                .author(trim(row.getAuthor()))
                                .publisher(trim(row.getPublisher()))
                                .publishedYear(parseIntegerSafe(row.getPublicationYear()) != null
                                        ? parseIntegerSafe(row.getPublicationYear())
                                        : 0)
                                .url(trim(row.getUrl()))
                                .build();
                        source = sourceRepository.save(source);
                    } else {
                        source = sourceRepository.findBySourceCode(srcCode).orElseThrow();
                    }

                    // Map to Subjects
                    String subjectCodeRaw = trim(row.getSubjectCode());
                    if (subjectCodeRaw != null) {
                        String[] subjectCodes = subjectCodeRaw.split("[,\\-;\n]+");
                        for (String sCode : subjectCodes) {
                            sCode = sCode.trim();
                            if (sCode.isEmpty())
                                continue;

                            Subject subject = subjectRepository.findBySubjectCode(sCode).orElse(null);
                            if (subject != null) {
                                boolean exists = proposedSourceRepository.existsBySource_SourceIdAndSubject_SubjectId(
                                        source.getSourceId(), subject.getSubjectId());
                                if (!exists) {
                                    ProposedSource ps = ProposedSource.builder()
                                            .source(source)
                                            .subject(subject)
                                            .build();
                                    proposedSourceRepository.save(ps);
                                }
                            }
                        }
                    }
                }
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
        int state = 0; // 0 = find major header, 1 = read major data, 2 = find po header, 3 = read po
                       // data

        Set<String> poCodesInFile = new HashSet<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            if (state == 0) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row, c, formatter);
                    if (cellVal.equalsIgnoreCase("Major Code"))
                        majorCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Name") || cellVal.equalsIgnoreCase("Major Name"))
                        majorNameCol = c;
                    else if (cellVal.equalsIgnoreCase("Description"))
                        majorDescCol = c;
                }
                if (majorCodeCol != -1 && majorNameCol != -1)
                    state = 1;
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
                    if (cellVal.equalsIgnoreCase("PO Code"))
                        poCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Description") || cellVal.equalsIgnoreCase("PO Description"))
                        poDescCol = c;
                }
                if (poCodeCol != -1)
                    state = 3;
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
            ctx.details
                    .add(ImportMajorResult.builder().status("FAILED").message("Major Data not found in sheet").build());
            return true;
        }

        if (majorRepository.existsByMajorCode(ctx.parsedMajorCode)) {
            ctx.isExistingMajor = true;
            // Existing major: DO NOT throw error. Just use the existing one, but
            // validate/import POs from the sheet as requested.
            // If the major exists but we still want to add POs from the sheet to it, it is
            // valid based on user requirements.
        } else if (ctx.parsedMajorName == null || ctx.parsedMajorName.isEmpty()) {
            ctx.details.add(ImportMajorResult.builder().majorCode(ctx.parsedMajorCode).status("FAILED")
                    .message("Missing required field: Name").build());
            hasErrors = true;
        }

        return hasErrors;
    }

    private boolean parseAndValidateCurriculum(Sheet sheet, CurriculumImportContext ctx,
            MajorImportContext majorContext) {
        boolean hasErrors = false;
        DataFormatter formatter = new DataFormatter();
        int curCodeCol = -1, curNameCol = -1, curYearCol = -1, curDescCol = -1, majorCodeCol = -1;
        int ploCodeCol = -1, ploDescCol = -1, poMappingCol = -1;
        int state = 0;
        Set<String> ploCodesInFile = new HashSet<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;
            // STATE 0: Tìm Header của Curriculum
            if (state == 0) {
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
                if (curCodeCol != -1 && majorCodeCol != -1)
                    state = 1;
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
                                .message("System error: Data from the Major sheet could not be found for comparison")
                                .build());
                        return true; // Dừng toàn bộ sheet này
                    }

                    // So sánh mã ngành giữa 2 sheet
                    if (!majorContext.parsedMajorCode.equalsIgnoreCase(ctx.parsedMajorCode)) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode)
                                .status("FAILED")
                                .message("Major Code in sheet " +
                                        "Curriculum [" + ctx.parsedMajorCode +
                                        "] does not match the Major Code in the Major sheet ["
                                        + majorContext.parsedMajorCode + "]")
                                .build());

                        // Theo yêu cầu: Không khớp thì không cần check PO mapping
                        return true; // Thoát hàm validate sheet Curriculum ngay lập tức
                    }

                    ctx.parsedCurName = curNameCol != -1 ? getCellValue(row, curNameCol, formatter) : null;
                    ctx.parsedCurDesc = curDescCol != -1 ? getCellValue(row, curDescCol, formatter) : null;
                    if (curYearCol != -1) {
                        String yearRaw = getCellValue(row, curYearCol, formatter);
                        try {
                            ctx.parsedStartYear = Integer.parseInt(yearRaw);
                        } catch (Exception ignored) {
                        }
                    }
                    state = 2;
                }
            } else if (state == 2) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row, c, formatter);
                    if (cellVal.equalsIgnoreCase("PLO Code"))
                        ploCodeCol = c;
                    else if (cellVal.equalsIgnoreCase("Description") || cellVal.equalsIgnoreCase("PLO Description"))
                        ploDescCol = c;
                    else if (cellVal.equalsIgnoreCase("PO Code Mapping"))
                        poMappingCol = c;
                }
                if (ploCodeCol != -1)
                    state = 3;
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
                            if (cleanPoCode.isEmpty())
                                continue;

                            // Check if this PO is being imported right now in Major sheet
                            boolean foundInSheet = majorContext.poListToSave.stream()
                                    .anyMatch(po -> po.getPoCode().equalsIgnoreCase(cleanPoCode)
                                            && majorContext.parsedMajorCode.equalsIgnoreCase(ctx.parsedMajorCode));

                            // If not in sheet, check in DB
                            Optional<PO> foundPOInDB = poRepository.findByPoCodeAndMajor_MajorCode(cleanPoCode,
                                    ctx.parsedMajorCode);

                            if (!foundInSheet && foundPOInDB.isEmpty()) {
                                poMappingOk = false;
                                mappingErrors.add(cleanPoCode + " not found in Major DB nor in Major import sheet");
                            } else {
                                if (foundPOInDB.isPresent()) {
                                    ploData.mappedPOs.add(foundPOInDB.get());
                                } else {
                                    // PO is in sheet, but since it's not saved yet, we create a temporary
                                    // reference.
                                    // We will link it properly during insertion phase or assume it's created.
                                    // We just need to store it so it can be mapped later.
                                    PO sheetPo = majorContext.poListToSave.stream()
                                            .filter(po -> po.getPoCode().equalsIgnoreCase(cleanPoCode))
                                            .findFirst().orElse(null);
                                    if (sheetPo != null)
                                        ploData.mappedPOs.add(sheetPo);
                                }
                            }
                        }
                    }

                    if (!poMappingOk) {
                        ctx.details.add(ImportCurriculumResult.builder()
                                .curriculumCode(ctx.parsedCurCode).ploCode(ploCode).status("FAILED")
                                .message("PLO " + ploCode + " mapping errors: " + String.join(", ", mappingErrors))
                                .build());
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
            ctx.details.add(ImportCurriculumResult.builder().status("FAILED")
                    .message("Curriculum Data not found in sheet").build());
            return true;
        }

        if (curriculumRepository.existsByCurriculumCode(ctx.parsedCurCode)) {
            ctx.isExistingCurriculum = true;
            // Existing Curriculum: We do not error out. We append PLOs based on user
            // requirements.
        }

        if (ctx.parsedMajorCode == null || (!majorContext.parsedMajorCode.equalsIgnoreCase(ctx.parsedMajorCode)
                && !majorRepository.existsByMajorCode(ctx.parsedMajorCode))) {
            ctx.details.add(ImportCurriculumResult.builder()
                    .curriculumCode(ctx.parsedCurCode).status("FAILED")
                    .message("Major code not found in DB or import sheet: " + ctx.parsedMajorCode).build());
            hasErrors = true;
        }

        return hasErrors;
    }

    private boolean parseAndValidateSubject(Sheet sheet, SubjectImportContext ctx,
            Map<String, SubjectRegulationDTO> regulationMap) {

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
                } else {
                    if (subjectCode.equalsIgnoreCase("N/A")) {
                        subjectCode = generateNASubjectCode(subjectName);
                    }
                    ctx.fileSubjectCodes.add(subjectCode.toUpperCase());
                }
                if (subjectName == null)
                    missingCols.add("Subject Name");
                if (departmentCode == null)
                    missingCols.add("Department Code");
                if (trim(row.getCredits()) == null)
                    missingCols.add("Credits");
                if (trim(row.getTimeAllocation()) == null)
                    missingCols.add("Time Allocation");
                if (trim(row.getMinToPass()) == null)
                    missingCols.add("Min to pass");
                if (trim(row.getStudentLimit()) == null)
                    missingCols.add("Student Limit");
                if (trim(row.getStudentTasks()) == null)
                    missingCols.add("Student Tasks");
                if (trim(row.getScoringScale()) == null)
                    missingCols.add("Scoring Scale");
                if (trim(row.getMinBloomLevel()) == null)
                    missingCols.add("Min Bloom Level");

                if (!missingCols.isEmpty()) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("FAILED")
                            .message("Subject " + (subjectCode != null ? subjectCode : "Unknown") + " missing columns: "
                                    + String.join(", ", missingCols))
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
                            .message(subjectCode + " not included " +
                                    "in the prescribed framework program")
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
                    mismatchErrors.add("Credit (Standard: " + regDto.credit + ", Excel: " + excelCredits + ")");
                }
                if (regDto.theoryPeriod != null && !regDto.theoryPeriod.equals(theory)) {
                    mismatchErrors.add("Theory (Standard: " + regDto.theoryPeriod + ", Excel: " + theory + ")");
                }
                if (regDto.practicalPeriod != null && !regDto.practicalPeriod.equals(practical)) {
                    mismatchErrors
                            .add("Practical (Standard: " + regDto.practicalPeriod + ", Excel: " + practical + ")");
                }
                if (regDto.selfStudyPeriod != null && !regDto.selfStudyPeriod.equals(selfStudy)) {
                    mismatchErrors
                            .add("SelfStudy (Standard: " + regDto.selfStudyPeriod + ", Excel: " + selfStudy + ")");
                }

                if (!mismatchErrors.isEmpty()) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode)
                            .status("FAILED")
                            .message("Incorrect specified parameters: " + String.join(", ", mismatchErrors))
                            .build());
                    hasErrors = true;
                    continue;
                }

                // comment do thử để chỗ khác
                // ctx.fileSubjectCodes.add(subjectCode.toUpperCase());
                if (!subjectCodesInFile.add(subjectCode.toUpperCase())) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("FAILED").message("Duplicate subjectCode in file")
                            .build());
                    hasErrors = true;
                    continue;
                }

                if (subjectRepository.existsBySubjectCode(subjectCode)) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status(
                                    "SUCCESS")
                            .message("Validated").build());
                    continue; // SKIP Insert
                }

                Department department = departmentRepository.findByDepartmentCode(departmentCode).orElse(null);
                if (department == null) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("FAILED")
                            .message("Department code not found in DB: " + departmentCode).build());
                    hasErrors = true;
                    continue;
                }

                Integer credits;
                try {
                    credits = Integer.parseInt(row.getCredits().trim());
                } catch (NumberFormatException e) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(subjectCode).status("FAILED").message("Credits must be a valid number")
                            .build());
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
                        .minBloomLevel(parseIntegerSafe(row.getMinBloomLevel()))
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

            // === VALIDATE COMPLETENESS: Đảm bảo sheet Subject có đủ TẤT CẢ môn trong
            // COURSE_MAPPING ===
            if (!regulationMap.isEmpty()) {
                List<String> missingInSheet = new ArrayList<>();
                for (String regCode : regulationMap.keySet()) {
                    // subjectCodesInFile chứa các code đã pass duplicate check (uppercase)
                    if (!subjectCodesInFile.contains(regCode.toUpperCase())) {
                        // Cũng cần check trong DB — nếu môn đã tồn tại trong DB thì được tính là "đã
                        // có"
                        if (!subjectRepository.existsBySubjectCode(regulationMap.get(regCode).subjectCode)) {
                            missingInSheet.add(regulationMap.get(regCode).subjectCode);
                        }
                    }
                }
                if (!missingInSheet.isEmpty()) {
                    ctx.details.add(ImportSubjectResult.builder()
                            .subjectCode(null)
                            .status("FAILED")
                            .message(
                                    "The Sheet Subject lacks the mandatory courses as required by regulations COURSE_MAPPING: "
                                            + String.join(", ", missingInSheet))
                            .build());
                    hasErrors = true;
                }
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
                if (groupCode == null)
                    missingCols.add("Group Code");
                if (groupName == null)
                    missingCols.add("Group Name");

                if (!missingCols.isEmpty()) {
                    ctx.details.add(ImportGroupResult.builder()
                            .groupCode(groupCode).status("FAILED")
                            .message("Group " + (groupCode != null ? groupCode : "Unknown") + " missing columns: "
                                    + String.join(", ", missingCols))
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

    private boolean parseAndValidateSemesterMapping(
            Sheet sheet,
            SemesterImportContext ctx,
            SubjectImportContext subCtx,
            GroupImportContext grpCtx,
            CurriculumImportContext curCtx,
            Map<String, SubjectRegulationDTO> regulationMap) {
        boolean hasErrors = false;
        Set<String> subjectCodesInFile = new HashSet<>();

        // Find existing curriculum subjects to prevent duplicates if modifying an
        // existing curriculum
        // Removed DB checks as requested.

        try {
            List<CurriculumGroupSubjectImportDTO> rows = ExcelImporter.importFromSheet(sheet,
                    CurriculumGroupSubjectImportDTO.class);
            for (int i = 0; i < rows.size(); i++) {
                int rowNumber = i + 2;
                CurriculumGroupSubjectImportDTO row = rows.get(i);

                String groupCode = trim(row.getGroupCode());
                String subjectCode = trim(row.getSubjectCode());
                String semesterRaw = trim(row.getSemester());

                if (subjectCode == null || semesterRaw == null) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw,
                            "Missing required fields: Subject Code, Semester"));
                    hasErrors = true;
                    continue;
                }

                if (!subjectCodesInFile.add(subjectCode.toUpperCase())) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw,
                            "Duplicate subject in mapping file"));
                    hasErrors = true;
                    continue;
                }

                int semesterNo;
                try {
                    semesterNo = Integer.parseInt(semesterRaw);
                    if (semesterNo <= 0)
                        throw new Exception();
                } catch (Exception e) {
                    ctx.details.add(
                            buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw, "Invalid semester value"));
                    hasErrors = true;
                    continue;
                }

                // Verify Subject exists in Subject sheet
                if (!subCtx.fileSubjectCodes.contains(subjectCode.toUpperCase())) {
                    ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw,
                            "This Subject Code is not in the Subject Sheet"));
                    hasErrors = true;
                    continue;
                }

                // Verify Group exists in Group sheet (if provided)
                if (groupCode != null && !groupCode.isEmpty()) {
                    if (!grpCtx.fileGroupCodes.contains(groupCode.toUpperCase())) {
                        ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw,
                                "This Group Code is not in the Sheet Group"));
                        hasErrors = true;
                        continue;
                    }
                }

                // ZERO-LAYER VALIDATION for Semester
                if (regulationMap.containsKey(subjectCode.toUpperCase())) {
                    SubjectRegulationDTO regDto = regulationMap.get(subjectCode.toUpperCase());
                    if (regDto.semester != null && !regDto.semester.equals(semesterNo)) {
                        ctx.details.add(buildSemFail(rowNumber, groupCode, subjectCode, semesterRaw,
                                "Wrong semester specified (Standard: " + regDto.semester + ", Excel: " + semesterNo
                                        + ")"));
                        hasErrors = true;
                        continue;
                    }
                }

                Curriculum_Group_Subject mapping = Curriculum_Group_Subject.builder()
                        .semester(semesterNo)
                        // Temporary dummy subjects/groups to hold the code, replaced during insertion
                        // phase
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

    private boolean parseAndValidateSource(
            Sheet sheet,
            SourceImportContext ctx,
            SubjectImportContext subCtx,
            Map<String, SourceRegulationDTO> regulationMap) {
        boolean hasErrors = false;
        try {
            List<SourceImportDTO> rows = ExcelImporter.importFromSheet(sheet,
                    SourceImportDTO.class);
            for (SourceImportDTO row : rows) {
                String sourceCode = trim(row.getSourceCode());
                String sourceName = trim(row.getSourceName());
                String subjectCodeRaw = trim(row.getSubjectCode());

                List<String> missingCols = new ArrayList<>();
                if (sourceCode == null)
                    missingCols.add("Source Code");
                if (sourceName == null)
                    missingCols.add("Source Name");
                if (subjectCodeRaw == null)
                    missingCols.add("Subject Code");

                if (!missingCols.isEmpty()) {
                    ctx.details.add(ImportSourceResult.builder()
                            .sourceCode(sourceCode)
                            .status("FAILED")
                            .message("Missing columns: " + String.join(", ", missingCols))
                            .build());
                    hasErrors = true;
                    continue;
                }

                if (!ctx.fileSourceCodes.add(sourceCode.toUpperCase())) {
                    ctx.details.add(ImportSourceResult.builder()
                            .sourceCode(sourceCode).status("FAILED").message("Duplicate Source Code in file").build());
                    hasErrors = true;
                    continue;
                }

                // Zero-Layer Validation
                if (!regulationMap.containsKey(sourceCode.toUpperCase())) {
                    ctx.details.add(ImportSourceResult.builder()
                            .sourceCode(sourceCode)
                            .status("FAILED")
                            .message("Source Code [" + sourceCode +
                                    "] not included in the regulations SOURCE_DOCUMENTS")
                            .build());
                    hasErrors = true;
                    continue;
                }

                SourceRegulationDTO regDto = regulationMap.get(sourceCode.toUpperCase());
                List<String> mismatchErrors = new ArrayList<>();

                if (regDto.sourceName != null && !regDto.sourceName.equalsIgnoreCase(sourceName)) {
                    mismatchErrors.add("Source Name (Standard: " + regDto.sourceName + ")");
                }
                String author = trim(row.getAuthor());
                if (regDto.author != null && !regDto.author.equalsIgnoreCase(author)) {
                    mismatchErrors.add("Author (Standard: " + regDto.author + ")");
                }
                String publisher = trim(row.getPublisher());
                if (regDto.publisher != null && !regDto.publisher.equalsIgnoreCase(publisher)) {
                    mismatchErrors.add("Publisher (Standard: " + regDto.publisher + ")");
                }
                Integer pubYear = parseIntegerSafe(row.getPublicationYear());
                if (regDto.publicationYear != null && !regDto.publicationYear.equals(pubYear)) {
                    mismatchErrors.add("Publication Year (Standard: " + regDto.publicationYear + ")");
                }

                if (!mismatchErrors.isEmpty()) {
                    ctx.details.add(ImportSourceResult.builder()
                            .sourceCode(sourceCode)
                            .status("FAILED")
                            .message("Incorrect specified parameters: " + String.join(", ", mismatchErrors))
                            .build());
                    hasErrors = true;
                    continue;
                }

                // Cross-sheet Validation for Subject Code
                String[] subjectCodes = subjectCodeRaw.split("[,\\-;\n]+");
                boolean subjectMissing = false;
                List<String> invalidSubjects = new ArrayList<>();
                for (String sCode : subjectCodes) {
                    sCode = sCode.trim();
                    if (sCode.isEmpty())
                        continue;

                    if (!subCtx.fileSubjectCodes.contains(sCode.toUpperCase())) {
                        subjectMissing = true;
                        invalidSubjects.add(sCode);
                    }
                }

                if (subjectMissing) {
                    ctx.details.add(ImportSourceResult.builder()
                            .sourceCode(sourceCode)
                            .status("FAILED")
                            .message("The following Subject Codes are not in the Subject sheet: "
                                    + String.join(", ", invalidSubjects))
                            .build());
                    hasErrors = true;
                    continue;
                }

                // Passed
                ctx.rowsToSave.add(row);
                ctx.details.add(ImportSourceResult.builder()
                        .sourceCode(sourceCode).status("SUCCESS").message("Validated").build());
            }

            // === VALIDATE COMPLETENESS: Đảm bảo sheet Source có đủ TẤT CẢ source trong
            // SOURCE_DOCUMENTS ===
            if (!regulationMap.isEmpty()) {
                List<String> missingSourcesInSheet = new ArrayList<>();
                for (String regCode : regulationMap.keySet()) {
                    if (!ctx.fileSourceCodes.contains(regCode.toUpperCase())) {
                        // Cũng check DB — nếu source đã tồn tại thì được tính là "đã có"
//                        if (!sourceRepository.existsBySourceCode(regulationMap.get(regCode).sourceCode)) {
//                        }
                        missingSourcesInSheet.add(regulationMap.get(regCode).sourceCode);
                    }
                }
                if (!missingSourcesInSheet.isEmpty()) {
                    ctx.details.add(ImportSourceResult.builder()
                            .sourceCode(null)
                            .status("FAILED")
                            .message(
                                    "The Source Sheet lacks the mandatory documents as required by regulations SOURCE_DOCUMENTS: "
                                            + String.join(", ", missingSourcesInSheet))
                            .build());
                    hasErrors = true;
                }
            }

        } catch (Exception e) {
            log.error("Source parse error", e);
            hasErrors = true;
        }
        return hasErrors;
    }

    // ==========================================
    // UTILS & RESPONSE BUILDERS
    // ==========================================

    private Map<String, SubjectRegulationDTO> initZeroLayerValidation(String majorCode) {
        Map<String, SubjectRegulationDTO> map = new HashMap<>();
        if (majorCode == null || majorCode.isEmpty())
            return map;

        Major major = majorRepository.findByMajorCode(majorCode).orElse(null);
        if (major == null)
            return map;

        Regulation regulation = regulationRepository.findByCodeAndMajor_MajorId("COURSE_MAPPING", major.getMajorId())
                .orElse(null);
        if (regulation == null || regulation.getValue() == null)
            return map;

        String value = regulation.getValue();
        Pattern pattern = Pattern.compile("(.*?)\\s*\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            String namePart = matcher.group(1).trim();
            if (namePart.startsWith(",")) {
                namePart = namePart.substring(1).trim();
            }
            String dataPart = matcher.group(2).trim();
            String[] dataFields = dataPart.split("\\|");
            if (dataFields.length >= 6) {
                String code = dataFields[0].trim();
                Integer semester = parseIntegerSafe(dataFields[1]);
                Integer credit = parseIntegerSafe(dataFields[2]);
                Integer theory = parseIntegerSafe(dataFields[3]);
                Integer practical = parseIntegerSafe(dataFields[4]);
                Integer selfStudy = parseIntegerSafe(dataFields[5]);

                if (code.equalsIgnoreCase("N/A")) {
                    code = generateNASubjectCode(namePart);
                }

                SubjectRegulationDTO dto = new SubjectRegulationDTO(code, namePart, semester, credit, theory, practical,
                        selfStudy);
                map.put(code.toUpperCase(), dto);
            }
        }
        return map;
    }

    private String generateNASubjectCode(String subjectName) {
        if (subjectName == null || subjectName.trim().isEmpty())
            return "N/A_UNKNOWN";
        String normalized = Normalizer.normalize(subjectName.trim(), Normalizer.Form.NFD);
        String noDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String noSpaces = noDiacritics.replaceAll("[^a-zA-Z0-9]", "");
        return "N/A_" + noSpaces;
    }

    // private Map<String, SourceRegulationDTO> initSourceZeroLayerValidation(String
    // majorCode) {
    // Map<String, SourceRegulationDTO> map = new HashMap<>();
    // if (majorCode == null || majorCode.isEmpty()) return map;
    //
    // Major major = majorRepository.findByMajorCode(majorCode).orElse(null);
    // if (major == null) return map;
    //
    // Regulation regulation =
    // regulationRepository.findByCodeAndMajor_MajorId("SOURCE_DOCUMENTS",
    // major.getMajorId()).orElse(null);
    // if (regulation == null || regulation.getValue() == null) return map;
    //
    // String value = regulation.getValue();
    // // Format: SourceCode/SubjectCode/SourceName/Author/Publisher/PublisherYear
    // // separated by comma. Notice that some names might have commas!
    // // But the example format is: "000100/001535-001264/Giáo trình.../Bộ GD
    // ĐT/NXB.../2016, 000101/..."
    // // If there's a comma in the title, it could break. We split by ", " followed
    // by a number.
    // // Actually, let's split by ", " and check if the part starts with numbers.
    // String[] parts = value.split(",\\s*(?=\\d{5,6}/)"); //
    // // Split by comma followed by 5 or 6 digits and a slash
    // if (parts.length == 1 && !value.contains(", ")) {
    // // maybe no comma at all
    // parts = new String[]{value};
    // }
    //
    // for (String part : parts) {
    // String[] fields = part.split("/");
    // if (fields.length >= 6) {
    // String sourceCode = fields[0].trim();
    // String subjectCode = fields[1].trim();
    // String sourceName = fields[2].trim();
    // String author = fields[3].trim();
    // String publisher = fields[4].trim();
    // String yearStr = fields[5].trim();
    //
    // Integer year = parseIntegerSafe(yearStr);
    //
    // SourceRegulationDTO dto = new SourceRegulationDTO(
    // sourceCode, subjectCode, sourceName, author, publisher, year);
    // map.put(sourceCode.toUpperCase(), dto);
    // }
    // }
    // return map;
    // }

    private Map<String, SourceRegulationDTO> initSourceZeroLayerValidation(String majorCode) {
        Map<String, SourceRegulationDTO> map = new HashMap<>();
        if (majorCode == null || majorCode.isEmpty())
            return map;

        Major major = majorRepository.findByMajorCode(majorCode).orElse(null);
        if (major == null)
            return map;

        Regulation regulation = regulationRepository.findByCodeAndMajor_MajorId("SOURCE_DOCUMENTS", major.getMajorId())
                .orElse(null);
        if (regulation == null || regulation.getValue() == null)
            return map;

        String value = regulation.getValue();

        // Sử dụng Regex để tìm chuỗi theo mẫu: Tên sách(Mã|Môn|Tác giả|NXB|Năm)
        Pattern pattern = Pattern.compile("(.*?)\\s*\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(value);

        while (matcher.find()) {
            String sourceName = matcher.group(1).trim();
            if (sourceName.startsWith(",")) {
                sourceName = sourceName.substring(1).trim();
            }

            String dataPart = matcher.group(2).trim();
            String[] dataFields = dataPart.split("\\|");

            if (dataFields.length >= 5) {
                String sourceCode = dataFields[0].trim();
                String subjectsRaw = dataFields[1].trim();
                String author = dataFields[2].trim();
                String publisher = dataFields[3].trim();
                Integer year = parseIntegerSafe(dataFields[4]);

                // Xử lý đa mã môn (cách nhau bằng phẩy, chấm phẩy hoặc gạch nối)
                String[] subjectArray = subjectsRaw.split("[,;\\-]");
                List<String> allowedSubjects = new ArrayList<>();
                for (String s : subjectArray) {
                    String cleanCode = s.trim();
                    if (!cleanCode.isEmpty()) {
                        // Chỉ dùng trim() và in hoa để đối chiếu cho an toàn
                        allowedSubjects.add(cleanCode.toUpperCase());
                    }
                }

                SourceRegulationDTO dto = new SourceRegulationDTO(
                        sourceCode, allowedSubjects, sourceName, author, publisher, year);
                map.put(sourceCode.toUpperCase(), dto);
            }
        }
        return map;
    }

    private ImportCurriculumGroupSubjectResult buildSemFail(
            int rowNumber,
            String groupCode,
            String subjectCode,
            String semester,
            String message) {
        return ImportCurriculumGroupSubjectResult.builder()
                .rowNumber(rowNumber).groupCode(groupCode).subjectCode(subjectCode).semester(semester)
                .status("FAILED").message(message).build();
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null || cellIndex == -1)
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

    private Integer parseIntegerSafe(String raw) {
        String value = trim(raw);
        if (value == null)
            return null;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private ImportMajorResponse buildMajorResponse(MajorImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportMajorResponse.builder().total(total).success(success).failed(total - success).details(ctx.details)
                .build();
    }

    private ImportCurriculumResponse buildCurriculumResponse(CurriculumImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportCurriculumResponse.builder().total(total).success(success).failed(total - success)
                .details(ctx.details).build();
    }

    private ImportSubjectResponse buildSubjectResponse(SubjectImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportSubjectResponse.builder().total(total).success(success).failed(total - success)
                .details(ctx.details).build();
    }

    private ImportGroupResponse buildGroupResponse(GroupImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportGroupResponse.builder().total(total).success(success).failed(total - success).details(ctx.details)
                .build();
    }

    private ImportCurriculumGroupSubjectResponse buildSemesterResponse(SemesterImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportCurriculumGroupSubjectResponse.builder().total(total).success(success).failed(total - success)
                .details(ctx.details).build();
    }

    private ImportSourceResponse buildSourceResponse(SourceImportContext ctx) {
        int total = ctx.details.size();
        int success = (int) ctx.details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        return ImportSourceResponse.builder().total(total).success(success).failed(total - success).details(ctx.details)
                .build();
    }

    // ==========================================
    // INTERNAL CONTEXT CLASSES
    // ==========================================

    private static class SourceRegulationDTO {
        String sourceCode;
        List<String> subjectCode;
        String sourceName;
        String author;
        String publisher;
        Integer publicationYear;

        public SourceRegulationDTO(String sourceCode, List<String> subjectCode, String sourceName, String author,
                String publisher, Integer publicationYear) {
            this.sourceCode = sourceCode;
            this.subjectCode = subjectCode;
            this.sourceName = sourceName;
            this.author = author;
            this.publisher = publisher;
            this.publicationYear = publicationYear;
        }
    }

    private static class SourceImportContext {
        Set<String> fileSourceCodes = new HashSet<>();
        List<SourceImportDTO> rowsToSave = new ArrayList<>();
        List<ImportSourceResult> details = new ArrayList<>();
    }

    private static class SubjectRegulationDTO {
        String subjectCode;
        String subjectName;
        Integer semester;
        Integer credit;
        Integer theoryPeriod;
        Integer practicalPeriod;
        Integer selfStudyPeriod;

        public SubjectRegulationDTO(String subjectCode, String subjectName, Integer semester, Integer credit,
                Integer theoryPeriod, Integer practicalPeriod, Integer selfStudyPeriod) {
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
