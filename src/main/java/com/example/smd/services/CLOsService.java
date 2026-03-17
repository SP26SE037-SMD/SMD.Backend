package com.example.smd.services;

import com.example.smd.dto.request.clo.CLOsCreateRequest;
import com.example.smd.dto.request.clo.CLOsRequest;
import com.example.smd.dto.response.clo.CLOsResponse;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Subject;
import com.example.smd.enums.PloStatus;
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


    public Page<CLOsResponse> getClosBySubject(String subjectId, int page, int size) {
        try {
            // 1. Kiểm tra định dạng UUID và sự tồn tại của Subject
            UUID id = UUID.fromString(subjectId);
            if (!subjectRepository.existsById(id)) {
                throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
            }

            // 2. Thiết lập phân trang và sắp xếp theo cloCode
            Pageable pageable = PageRequest.of(page, size, Sort.by("cloCode").ascending());

            // 3. Truy vấn dữ liệu và chuyển đổi sang Response DTO
            return closRepository.findBySubject_SubjectId(id, pageable)
                    .map(closMapper::toCloResponse);

        } catch (IllegalArgumentException e) {
            // Ném lỗi nếu định dạng ID không hợp lệ
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional
    public CLOsResponse updateClo(String id, CLOsRequest request) {
        CLOs clo = closRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

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

    public CLOsResponse getCloDetail(String id) {
        try {
            UUID cloId = UUID.fromString(id);

            // Sử dụng hàm có Join Fetch/EntityGraph để lấy luôn thông tin Subject
            return closRepository.findById(cloId)
                    .map(closMapper::toCloResponse)
                    .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

        } catch (IllegalArgumentException e) {
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
