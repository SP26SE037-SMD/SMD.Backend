package com.example.smd.services;

import com.example.smd.dto.response.curriculum.ImportFullCurriculumResponse;
import com.example.smd.dto.response.curriculum.ImportCurriculumResponse;
import com.example.smd.dto.response.curriculumgroupsubject.ImportCurriculumGroupSubjectResponse;
import com.example.smd.dto.response.group.ImportGroupResponse;
import com.example.smd.dto.response.major.ImportMajorResponse;
import com.example.smd.dto.response.subject.ImportSubjectResponse;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FullImportService {

    MajorService majorService;
    CurriculumService curriculumService;
    SubjectService subjectService;
    GroupService groupService;
    CurriculumGroupSubjectService curriculumGroupSubjectService;

    @Transactional
    public ImportFullCurriculumResponse importFullCurriculum(MultipartFile file) {
        ImportFullCurriculumResponse response = ImportFullCurriculumResponse.builder().build();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            
            // 1. Import Major
            Sheet majorSheet = workbook.getSheet("Major");
            if (majorSheet != null) {
                ImportMajorResponse majorRes = majorService.importMajorFromSheet(majorSheet);
                response.setMajorResult(majorRes);
            }

            // 2. Import Curriculum & PLOs
            Sheet curriculumSheet = workbook.getSheet("Curriculum");
            UUID newCurriculumId = null;
            if (curriculumSheet != null) {
                ImportCurriculumResponse curRes = curriculumService.importCurriculumFromSheet(curriculumSheet);
                response.setCurriculumResult(curRes);
                newCurriculumId = curRes.getCurriculumId();
            }

            // 3. Import Subjects
            Sheet subjectSheet = workbook.getSheet("New Subject");
            if (subjectSheet != null) {
                ImportSubjectResponse subRes = subjectService.importSubjectFromSheet(subjectSheet);
                response.setSubjectResult(subRes);
            }

            // 4. Import Groups
            Sheet groupSheet = workbook.getSheet("Group");
            if (groupSheet != null) {
                ImportGroupResponse groupRes = groupService.importGroupFromSheet(groupSheet);
                response.setGroupResult(groupRes);
            }

            // 5. Import Semester Mapping (CurriculumGroupSubject)
            Sheet semesterSheet = workbook.getSheet("Semester Mapping");
            if (semesterSheet != null && newCurriculumId != null) {
                ImportCurriculumGroupSubjectResponse semRes = curriculumGroupSubjectService
                        .importCurriculumGroupSubjectFromSheet(semesterSheet, newCurriculumId);
                response.setSemesterMappingResult(semRes);
            }

        } catch (Exception e) {
            log.error("Full import error", e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Full import failed: " + e.getMessage());
        }

        response.setSuccess(true);
        response.setMessage("Full import completed successfully");
        return response;
    }
}
