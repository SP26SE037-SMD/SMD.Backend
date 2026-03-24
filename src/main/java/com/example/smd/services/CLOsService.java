package com.example.smd.services;

import com.example.smd.dto.request.clo.CLOsCreateRequest;
import com.example.smd.dto.request.clo.CLOsRequest;
import com.example.smd.dto.response.clo.CLOsResponse;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Subject;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SubjectStatus;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.CLOsMapper;
import com.example.smd.repositories.CLOsRepository;
import com.example.smd.repositories.SubjectRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CLOsService {

    CLOsRepository closRepository;
    SubjectRepository subjectRepository;
    AccountService accountService;
    CLOsMapper closMapper;

    @Transactional
    public List<CLOsResponse> createBulkClos(String subjectId, List<CLOsCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Kiểm tra Môn học (Subject) tồn tại
        UUID uuidSubjectId = UUID.fromString(subjectId);
        Subject subject = subjectRepository.findById(uuidSubjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        if (!subject.getStatus().equals(SubjectStatus.WAITING_SYLLABUS.toString())){
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
            if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
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
                List<String> managerRoles = List.of("VP", "ADMIN", "HOCFDC", "HOPDC");
                if (!managerRoles.contains(roleName)) {
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
    public CLOsResponse updateClo(String id, CLOsRequest request) {
        CLOs clo = closRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

        if(!clo.getStatus().equals(PloStatus.DRAFT.toString())){
            throw new AppException(ErrorCode.CLO_NOT_EDITABLE);
        }

        // Cập nhật các trường thông tin
        clo.setCloCode(request.getCloCode());
        clo.setDescription(request.getDescription());
        clo.setBloomLevel(request.getBloomLevel());

        return closMapper.toCloResponse(closRepository.save(clo));
    }

    @Transactional
    public void deleteClo(String id) {
        try {
            UUID cloId = UUID.fromString(id);

            // Kiểm tra xem CLO có tồn tại không
            CLOs clo = closRepository.findById(cloId)
                    .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));
            if(clo.getStatus().equals("DRAFT")) {
                closRepository.delete(clo);
            } else{
                clo.setStatus("ARCHIVED");
                closRepository.save(clo);
            }

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public CLOsResponse getCloDetail(String id, String accountId) {
        try {
            UUID cloId = UUID.fromString(id);

            // 1. Tìm CLO (Nên dùng EntityGraph hoặc Fetch Join trong Repo để lấy luôn Subject nếu cần)
            CLOs clo = closRepository.findById(cloId)
                    .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

            // 2. Lấy thông tin Account để phân quyền
            var account = accountService.getAccountById(accountId);
            String roleName = account.getRole().getRoleName();

            // 3. Phân quyền: STUDENT + LECTURER chỉ xem được bản PUBLISHED
            if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
                if (!"PUBLISHED".equalsIgnoreCase(clo.getStatus())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
                }
            }

            // 4. Phân quyền: Bản DRAFT chỉ dành cho HOCFDC (Người soạn thảo chính)
            if ("DRAFT".equalsIgnoreCase(clo.getStatus())) {
                if (!roleName.equals("HOCFDC")) {
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
        SyllabusStatus status;
        try {
            status = SyllabusStatus.valueOf(newStatus.toUpperCase());
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
}
