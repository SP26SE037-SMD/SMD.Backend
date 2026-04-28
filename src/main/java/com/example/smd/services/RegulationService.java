package com.example.smd.services;

import com.example.smd.dto.request.RegulationRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.validate.ProgramRegulationResponse;
import com.example.smd.dto.response.RegulationResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.Regulation;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.mapper.RegulationMapper;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.RegulationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegulationService {

    private final RegulationRepository regulationRepository;
    private final RegulationMapper regulationMapper;
    private final MajorRepository majorRepository;
    private final MajorMapper majorMapper;
    private final GeminiService geminiService;

    @Transactional(readOnly = true)
    public Page<RegulationResponse> getAll(String search, int page, int size, String[] sort, UUID majorId) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] split = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(split[1]), split[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        Specification<Regulation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc bắt buộc theo majorId
            if (majorId != null) {
                predicates.add(cb.equal(root.get("major").get("majorId"), majorId));
            }

            // 2. Lọc theo từ khóa search (nếu có)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
                );
                predicates.add(searchPredicate);
            }

            // Kết hợp các điều kiện bằng phép AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return regulationRepository.findAll(spec, pageable)
                .map(regulationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RegulationResponse getById(UUID id) {
        Regulation regulation = regulationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation not found"));
        return regulationMapper.toResponse(regulation);
    }

    @Transactional
    public RegulationResponse create(RegulationRequest request) {
        if (regulationRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.INVALID_KEY, "Regulation code already exists");
        }

        Major major = majorRepository.findById(request.getMajorId())
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        Regulation regulation = regulationMapper.toEntity(request);
        regulation.setMajor(major);
        regulation = regulationRepository.save(regulation);
        return regulationMapper.toResponse(regulation);
    }

    @Transactional
    public RegulationResponse update(UUID id, RegulationRequest request) {
        Regulation regulation = regulationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation not found"));

        if (request.getCode() != null &&
                regulationRepository.existsByCodeAndRegulationIdNot(request.getCode(), id)) {
            throw new AppException(ErrorCode.INVALID_KEY, "Regulation code already exists");
        }

        regulationMapper.updateEntity(regulation, request);
        regulation = regulationRepository.save(regulation);
        return regulationMapper.toResponse(regulation);
    }

    @Transactional
    public boolean delete(UUID id) {
        Regulation regulation = regulationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation not found"));
        regulationRepository.delete(regulation);
        return true;
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }

    @Transactional
    public MajorResponse importMajorAndAddRegulation(MultipartFile file, String accountId) throws InterruptedException {
        var programRegulationResponse = geminiService.extractMasterDataFromPdf(file, accountId);

        var major = new Major();
        major.setMajorCode(programRegulationResponse.getMajorCode());
        major.setMajorName(programRegulationResponse.getMajorName());
        major.setDescription(programRegulationResponse.getMajorDescription());
        major.setStatus(PloStatus.DRAFT.toString());
        var saveMajor = majorRepository.save(major);

        List<RegulationResponse> saveRegulations = createRegulationBluk(programRegulationResponse, saveMajor);
        var majorResponse = majorMapper.toMajorResponse(saveMajor);
        majorResponse.setRegulations(saveRegulations);

        return majorResponse;
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
        regulations.add(createRegulation("THEORY_LIMIT", "Định mức tiết lý thuyết", response.getTheoryTotalLimit(), major));
        regulations.add(createRegulation("DISCUSSION_LIMIT", "Định mức tiết thảo luận/thực hành", response.getDiscussionTotalLimit(), major));
        regulations.add(createRegulation("MAX_WEEKLY_PERIODS", "Số tiết tối đa/tuần", response.getMaxPeriodsPerWeek(), major));
        regulations.add(createRegulation("SELF_STUDY_FORMULA", "Công thức tự học", response.getSelfStudyAutoCalc(), major));
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
