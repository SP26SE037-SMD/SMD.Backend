package com.example.smd.services;

import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.dto.response.validate.ProgramRegulationResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.Regulation;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.RoleName;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.mapper.RegulationMapper;
import com.example.smd.realtime.RealtimePayload;
import com.example.smd.realtime.RealtimePublisher;
import com.example.smd.repositories.AccountRepository;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.RegulationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulationAsyncService {

    private final RegulationRepository regulationRepository;
    private final RegulationMapper regulationMapper;
    private final MajorRepository majorRepository;
    private final AccountService accountService;
    private final GeminiService geminiService;
    private final RealtimePublisher realtimePublisher;

    @Async
    @Transactional
    public void importMajorAndAddRegulation(byte[] fileData, String contentType, String accountId) {
        var programRegulationResponse = geminiService.extractMasterDataFromPdf(fileData, contentType, accountId);
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (RoleName.VP.toString().equals(roleName)) {
            realtimePublisher.publishToAccount(accountId,
                    RealtimePayload.status("VALIDATE_SUCCESS", programRegulationResponse));
            log.info("VALIDATE_SUCCESS: {}", "Data verification successful");
            log.info("Data: {}", programRegulationResponse);
        } else {
            var major = new Major();
            major.setMajorCode(programRegulationResponse.getMajorCode());
            major.setMajorName(programRegulationResponse.getMajorName());
            major.setDescription(programRegulationResponse.getMajorDescription());
            major.setStatus(PloStatus.DRAFT.toString());
            var saveMajor = majorRepository.save(major);

            List<RegulationResponse> saveRegulations = createRegulationBluk(programRegulationResponse, saveMajor);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (RoleName.HOCFDC.toString().equals(roleName)) {
                        realtimePublisher.publishToAccount(accountId,
                                RealtimePayload.status("IMPORT_SUCCESS", saveMajor.getMajorId()));
                    }
                }
            });
        }
    }

    @Transactional
    public List<RegulationResponse> createRegulationBluk(ProgramRegulationResponse response, Major major) {
        List<Regulation> regulations = new ArrayList<>();

        regulations.add(createRegulation("TRAINING_LEVEL", "Training Level", response.getTrainingLevel(), major));
        regulations.add(createRegulation("PO_PLO_RULE", "PO/PLO Regulations", response.getPoPloRule(), major));
        regulations.add(createRegulation("TOTAL_CREDITS", "Total Program Credits", response.getTotalCreditsRule(), major));
        regulations.add(createRegulation("EXCLUDED_CREDITS", "Excluded Credits (GDQP/GDTC)", response.getExcludedCreditsRule(), major));
        regulations.add(createRegulation("GENERAL_EDU_CREDITS", "General Education Credits", response.getGeneralEducationCredits(), major));
        regulations.add(createRegulation("PROFESSIONAL_EDU_CREDITS", "Professional Education Credits", response.getProfessionalEducationCredits(), major));
        regulations.add(createRegulation("ASSESSMENT_RATIO", "In-class/Final Exam Grading Ratio", response.getAssessmentRule(), major));
        regulations.add(createRegulation("COURSE_CATALOG", "Course Catalog / Specializations", response.getCourseCatalogValidation(), major));
        regulations.add(createRegulation("COURSE_MAPPING", "Detailed Course Metrics (N|a|b|c)", response.getCourseDetailMapping(), major));
        regulations.add(createRegulation("SOURCE_DOCUMENTS", "Main Textbooks and Reference List", response.getSourceValidation(), major));

        List<Regulation> savedRegulations = regulationRepository.saveAll(regulations);

        return savedRegulations.stream()
                .map(regulationMapper::toResponse)
                .toList();
    }

    private Regulation createRegulation(String code, String name, String value, Major major) {
        Regulation reg = new Regulation();
        reg.setCode(code);
        reg.setName(name);
        reg.setValue(value != null ? value : "N/A"); // Tránh Null cho trường @NotNull
        reg.setMajor(major);
        return reg;
    }
}
