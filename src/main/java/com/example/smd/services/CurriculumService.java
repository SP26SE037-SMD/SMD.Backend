package com.example.smd.services;

import com.example.smd.dto.request.curriculum.CurriculumCreateRequest;
import com.example.smd.dto.response.CurriculumResponse;
import com.example.smd.entities.Curriculum;
import com.example.smd.entities.Major;
import com.example.smd.entities.Subject;
import com.example.smd.enums.CurriculumStatus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CurriculumMapper;
import com.example.smd.repositories.CurriculumRepository;
import com.example.smd.repositories.MajorRepository;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurriculumService {
    
    CurriculumRepository curriculumRepository;
    MajorRepository majorRepository;
    CurriculumMapper curriculumMapper;
    AccountService accountService;
    
    /**
     * Lấy danh sách curriculum với phân trang và bộ lọc
     * @param search - Từ khóa tìm kiếm (có thể null)
     * @param searchBy - Tìm theo trường nào: code, name, hoặc all
     * @param status - Filter theo status (có thể null)
     * @param page - Số trang (0-based)
     * @param size - Số lượng item mỗi trang
     * @param sort - Mảng sort [field, direction]
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
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        // 2. Chuẩn hóa Status & Phân quyền (Giống hệt Major)
        // Xử lý trường hợp chuỗi rỗng hoặc "all" từ Frontend
        String finalStatus = (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("all"))
                ? null : status.trim();

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            finalStatus = "PUBLISHED"; // Role thấp luôn bị ép về PUBLISHED
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
                    case "code" -> curriculumRepository.findByCurriculumCodeContainingIgnoreCaseAndStatus(searchLower, finalStatus, pageable);
                    case "name" -> curriculumRepository.findByCurriculumNameContainingIgnoreCaseAndStatus(searchLower, finalStatus, pageable);
                    default -> curriculumRepository.searchAllFieldsWithStatus(searchLower, finalStatus, pageable);
                };
            } else {
                // TRƯỜNG HỢP 3: Có Search nhưng không có Status (Chỉ dành cho Admin/VP)
                curriculumPage = switch (type) {
                    case "code" -> curriculumRepository.findByCurriculumCodeContainingIgnoreCase(searchLower, pageable);
                    case "name" -> curriculumRepository.findByCurriculumNameContainingIgnoreCase(searchLower, pageable);
                    default -> curriculumRepository.findByCurriculumNameContainingIgnoreCaseOrCurriculumCodeContainingIgnoreCase(searchLower, searchLower, pageable);
                };
            }
        }

        return curriculumPage.map(curriculumMapper::toCurriculumResponse);
    }
    
    /**
     * Tạo curriculum mới
     */
    @Transactional
    public CurriculumResponse createCurriculum(CurriculumCreateRequest request) {
        log.info("Creating curriculum with code: {}", request.getCurriculumCode());
        
        // 1. Validate curriculum code không trùng
        if (curriculumRepository.existsByCurriculumCode(request.getCurriculumCode())) {
            throw new AppException(ErrorCode.CURRICULUM_CODE_EXISTS);
        }

        // 3. Kiểm tra Major tồn tại
        Major major = majorRepository.findById(UUID.fromString(request.getMajorId()))
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

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

        Curriculum curriculum =
                curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if(account.getRole().getRoleName().equals("STUDENT") ||  account.getRole().getRoleName().equals("LECTURER")) {
            if(!curriculum.getStatus().equals(CurriculumStatus.PUBLISHED.toString())) {
                new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if(curriculum.getStatus().equals(CurriculumStatus.DRAFT.toString())) {
            if(!account.getRole().getRoleName().equals("HOCFDC")){
                new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
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

        //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if(account.getRole().getRoleName().equals("STUDENT") ||  account.getRole().getRoleName().equals("LECTURER")) {
            if(!curriculum.getStatus().equals(CurriculumStatus.PUBLISHED.toString())) {
                new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if(curriculum.getStatus().equals(CurriculumStatus.DRAFT.toString())) {
            if(!account.getRole().getRoleName().equals("HOCFDC")){
                new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        
        return curriculumMapper.toCurriculumResponse(curriculum);
    }
    
    /**
     * Cập nhật curriculum
     */
    @Transactional
    public CurriculumResponse updateCurriculum(String id,
                                               CurriculumCreateRequest request) {
        log.info("Updating curriculum with ID: {}", id);
        
        // 1. Tìm curriculum hiện tại
        Curriculum curriculum =
                curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
        
        // 2. Kiểm tra nếu đổi code thì không được trùng với code khác
        if (!curriculum.getCurriculumCode().equals(request.getCurriculumCode())) {
            if (curriculumRepository.existsByCurriculumCode(request.getCurriculumCode())) {
                throw new AppException(ErrorCode.CURRICULUM_CODE_EXISTS);
            }
        }

        if(!curriculum.getStatus().equals(CurriculumStatus.DRAFT.toString())) {
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
                                                      int endYear) {
        log.info("Updating curriculum status for ID: {} to {}", id, endYear);

        Curriculum curriculum = curriculumRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

        curriculum.setEndYear(endYear);
        Curriculum updatedCurriculum = curriculumRepository.save(curriculum);

        return curriculumMapper.toCurriculumResponse(updatedCurriculum);
    }

    public void delete(UUID id) {
        try {
            Curriculum curriculum = curriculumRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

            if (curriculum.getStatus().equals(CurriculumStatus.DRAFT.toString())) {
                curriculumRepository.delete(curriculum);
            } else {
                curriculum.setStatus(CurriculumStatus.ARCHIVED.toString());
                curriculumRepository.save(curriculum);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
