package com.example.smd.services;

import com.example.smd.dto.request.ElectiveRequest;
import com.example.smd.dto.response.ElectiveResponse;
import com.example.smd.entities.Elective;
import com.example.smd.entities.Elective_Subject;
import com.example.smd.entities.Subject;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.ElectiveMapper;
import com.example.smd.mapper.SubjectMapper;
import com.example.smd.repositories.ElectiveRepository;
import com.example.smd.repositories.ElectiveSubjectRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElectiveService {
    ElectiveRepository electiveRepository;
    ElectiveSubjectRepository electiveSubjectRepository;
    SubjectRepository subjectRepository;

    ElectiveMapper electiveMapper;
    SubjectMapper subjectMapper;

    // Create - Kiểm tra mã trùng
    public ElectiveResponse create(ElectiveRequest request) {
        // Kiểm tra xem electiveCode đã tồn tại trong DB chưa
        if (electiveRepository.existsByElectiveCode(request.getElectiveCode())) {
            throw new AppException(ErrorCode.ELECTIVE_CODE_EXISTED);
        }

        Elective elective = electiveMapper.toElective(request);
        return electiveMapper.toElectiveResponse(electiveRepository.save(elective));
    }

    // Update - Kiểm tra NOT_FOUND và mã trùng (trừ chính nó)
    public ElectiveResponse update(UUID id, ElectiveRequest request) {
        Elective elective = electiveRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ELECTIVE_NOT_FOUND));

        // Nếu người dùng thay đổi electiveCode, phải kiểm tra xem mã mới có trùng với nhóm khác không
        if (request.getElectiveCode() != null &&
                !request.getElectiveCode().equals(elective.getElectiveCode()) &&
                electiveRepository.existsByElectiveCode(request.getElectiveCode())) {
            throw new AppException(ErrorCode.ELECTIVE_CODE_EXISTED);
        }

        electiveMapper.updateElective(elective, request);
        return electiveMapper.toElectiveResponse(electiveRepository.save(elective));
    }

    // Delete - Kiểm tra NOT_FOUND
    public void delete(UUID id) {
        if (!electiveRepository.existsById(id)) {
            throw new AppException(ErrorCode.ELECTIVE_NOT_FOUND);
        }

        // Gợi ý: Kiểm tra thêm nếu nhóm đang chứa Subjects thì không cho xóa
         if (electiveSubjectRepository.existsByElective_ElectiveId(id)) throw new AppException(ErrorCode.ELECTIVE_HAS_SUBJECTS);

        electiveRepository.deleteById(id);
    }

    // Get Details - Đã có bắt NOT_FOUND
    @Transactional
    public ElectiveResponse getDetails(UUID id) {
        Elective elective = electiveRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ELECTIVE_NOT_FOUND));

        // Lấy danh sách Subject từ bảng trung gian
        List<Subject> subjects = elective.getElectiveSubjects().stream()
                .map(Elective_Subject::getSubject)
                .toList();

        // Map Elective sang Response
        ElectiveResponse response = electiveMapper.toElectiveResponse(elective);

        // Set danh sách môn học đã được convert sang DTO
        response.setSubjects(subjects.stream()
                .map(subjectMapper::toSubjectResponse)
                .toList());

        return response;
    }

    public Page<ElectiveResponse> getAll(String search, int page, int size) {
        // 1. Thiết lập phân trang và mặc định sắp xếp theo electiveCode tăng dần
        Pageable pageable = PageRequest.of(page, size, Sort.by("electiveCode").ascending());

        // 2. Logic tìm kiếm: Nếu không có từ khóa thì lấy tất cả
        if (search == null || search.trim().isEmpty()) {
            return electiveRepository.findAll(pageable)
                    .map(electiveMapper::toElectiveResponse);
        }

        // 3. Tìm kiếm theo Tên hoặc Mã (không phân biệt hoa thường)
        return electiveRepository.findAllByElectiveNameContainingIgnoreCaseOrElectiveCodeContainingIgnoreCase(
                        search, search, pageable)
                .map(electiveMapper::toElectiveResponse);
    }

    @Transactional
    public void addSubjectToElective(UUID electiveId, UUID subjectId) {
        // 1. Kiểm tra tồn tại
        Elective elective = electiveRepository.findById(electiveId)
                .orElseThrow(() -> new AppException(ErrorCode.ELECTIVE_NOT_FOUND));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 2. Kiểm tra xem đã tồn tại liên kết chưa để tránh lỗi Duplicate Key
        boolean exists = electiveSubjectRepository.existsByElective_ElectiveIdAndSubject_SubjectId(electiveId, subjectId);
        if (exists) {
            throw new AppException(ErrorCode.ELECTIVE_SUBJECT_ALREADY_EXISTS);
        }

        // 3. Lưu liên kết
        Elective_Subject link = Elective_Subject.builder()
                .elective(elective)
                .subject(subject)
                .build();
        electiveSubjectRepository.save(link);
    }

    @Transactional
    public void removeSubjectFromElective(UUID electiveId, UUID subjectId) {
        // Tìm bản ghi cụ thể để xóa
        Elective_Subject link = electiveSubjectRepository.findByElective_ElectiveIdAndSubject_SubjectId(electiveId, subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.ELECTIVE_SUBJECT_NOT_FOUND));

        electiveSubjectRepository.delete(link);
    }
}
