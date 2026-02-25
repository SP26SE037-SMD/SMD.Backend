package com.example.smd.services;

import com.example.smd.dto.request.CLOsRequest;
import com.example.smd.dto.response.CLOsResponse;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Subject;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CLOsService {

    CLOsRepository closRepository;
    SubjectRepository subjectRepository;

    CLOsMapper closMapper;

    @Transactional
    public CLOsResponse createClo(CLOsRequest request) {
        UUID subjectId = UUID.fromString(request.getSubjectId()); // Bạn gửi lên là syllabusId nhưng hiểu là subjectId

        // 1. Kiểm tra môn học (Syllabus) có tồn tại không
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 2. Check trùng Code trong cùng 1 môn
        if (closRepository.existsByCloCodeAndSubject_SubjectId(request.getCloCode(), subjectId)) {
            throw new AppException(ErrorCode.CLO_CODE_EXISTS);
        }

        CLOs clo = closMapper.toClo(request);
        clo.setSubject(subject); // Liên kết CLO với Môn học

        return closMapper.toCloResponse(closRepository.save(clo));
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
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    @Transactional
    public CLOsResponse updateClo(String id, CLOsRequest request) {
        CLOs clo = closRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.CLO_NOT_FOUND));

        // Cập nhật các trường thông tin
        clo.setCloCode(request.getCloCode());
        clo.setCloName(request.getCloName()); // Trường cloName bạn yêu cầu thêm
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

            // Thực hiện xóa trực tiếp
            closRepository.delete(clo);

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
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
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
