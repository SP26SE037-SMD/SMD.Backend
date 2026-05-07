package com.example.smd.services;

import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.dto.response.validate.ProgramRegulationResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.Regulation;
import com.example.smd.enums.PloStatus;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.mapper.RegulationMapper;
import com.example.smd.realtime.RealtimePayload;
import com.example.smd.realtime.RealtimePublisher;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.RegulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegulationAsyncService {

    private final RegulationRepository regulationRepository;
    private final RegulationMapper regulationMapper;
    private final MajorRepository majorRepository;
    private final GeminiService geminiService;
    private final RealtimePublisher realtimePublisher;

    @Async
    @Transactional
    public void importMajorAndAddRegulation(byte[] fileData, String contentType, String accountId) {
        var programRegulationResponse = geminiService.extractMasterDataFromPdf(fileData, contentType, accountId);
        List<String> missingFields = new ArrayList<>();
        for (java.lang.reflect.Field field : programRegulationResponse.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(programRegulationResponse);
                if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                    // Lấy tên field hoặc lấy giá trị từ @JsonProperty để thông báo cho thân thiện
                    var jsonProperty = field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
                    missingFields.add(jsonProperty != null ? jsonProperty.value() : field.getName());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("The system encountered an error while checking all the data.");
            }
        }

        if (!missingFields.isEmpty()) {
            String errorMsg = String.join(", ", missingFields);
            realtimePublisher.publishToAccount(accountId,
                    RealtimePayload.status("VALIDATE_FAIL", errorMsg));
            throw new RuntimeException(errorMsg);
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
                    realtimePublisher.publishToAccount(accountId,
                            RealtimePayload.status("IMPORT_SUCCESS", saveMajor.getMajorId()));
                }
            });
        }


    }

    @Transactional
    public List<RegulationResponse> createRegulationBluk(ProgramRegulationResponse response, Major major) {
        // Danh sách các quy tắc cần trích xuất (Trừ major_code, major_name, major_description)
        // Cấu trúc: {Code, Name, Value}
        List<Regulation> regulations = new ArrayList<>();

        regulations.add(createRegulation("TRAINING_LEVEL", "Trình độ đào tạo", response.getTrainingLevel(), major));
        regulations.add(createRegulation("PO_PLO_RULE", "Quy định PO/PLO", response.getPoPloRule(), major));
        regulations.add(createRegulation("TOTAL_CREDITS", "Tổng tín chỉ chương trình", response.getTotalCreditsRule(), major));
        regulations.add(createRegulation("EXCLUDED_CREDITS", "Tín chỉ ngoại lệ (GDQP/GDTC)", response.getExcludedCreditsRule(), major));
        regulations.add(createRegulation("GENERAL_EDU_CREDITS", "Tín chỉ giáo dục đại cương", response.getGeneralEducationCredits(), major));
        regulations.add(createRegulation("PROFESSIONAL_EDU_CREDITS", "Tín chỉ giáo dục chuyên nghiệp", response.getProfessionalEducationCredits(), major));
        regulations.add(createRegulation("ASSESSMENT_RATIO", "Tỉ lệ điểm quá trình/cuối kỳ", response.getAssessmentRule(), major));
        regulations.add(createRegulation("COURSE_CATALOG", "Danh mục học phần", response.getCourseCatalogValidation(), major));
        regulations.add(createRegulation("COURSE_MAPPING", "Chi tiết định biên học phần (N|a|b|c)", response.getCourseDetailMapping(), major));
        regulations.add(createRegulation("SOURCE_DOCUMENTS", "Danh mục tài liệu tham khảo", response.getSourceValidation(), major));

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
