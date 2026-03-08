package com.example.smd.services;

import com.example.smd.dto.request.subject.SubjectRequest;
import com.example.smd.dto.response.ElectiveResponse;
import com.example.smd.dto.response.PrerequisiteResponse;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.entities.Department;
import com.example.smd.entities.Elective;
import com.example.smd.entities.Elective_Subject;
import com.example.smd.entities.Subject;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.ElectiveMapper;
import com.example.smd.mapper.PrerequisiteMapper;
import com.example.smd.mapper.SubjectMapper;
import com.example.smd.repositories.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectService {

    SubjectRepository subjectRepository;
    DepartmentRepository departmentRepository;
    ElectiveRepository electiveRepository;
    ElectiveSubjectRepository electiveSubjectRepository;
    PrerequisiteRepository prerequisiteRepository;

    SubjectMapper subjectMapper;
    ElectiveMapper  electiveMapper;
    PrerequisiteMapper prerequisiteMapper;

    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsBySubjectCode(request.getSubjectCode()))
            throw new AppException(ErrorCode.SUBJECT_CODE_EXISTS);

        Subject subject = subjectMapper.toSubject(request);

        //Check Department có tồn tại hay không
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        subject.setDepartment(department);

        subject = subjectRepository.save(subject);

        //Tạo môn mang theo giá trị elective
        if (request.getElectiveId() != null) {
            Elective elective = electiveRepository.findById(request.getElectiveId())
                    .orElseThrow(() -> new AppException(ErrorCode.ELECTIVE_NOT_FOUND));

            Elective_Subject link = Elective_Subject.builder()
                    .subject(subject)
                    .elective(elective)
                    .build();
            electiveSubjectRepository.save(link);
        }

        SubjectResponse response = subjectMapper.toSubjectResponse(subject);
        List<ElectiveResponse> electives = electiveSubjectRepository.findBySubject_SubjectId(response.getSubjectId())
                .stream()
                .map(es -> electiveMapper.toElectiveResponse(es.getElective()))
                .toList();

        response.setElectives(electives);

        return response;
    }

    public Page<SubjectResponse> getAll(String search, String searchBy, Boolean status, Pageable pageable) {
        Specification<Subject> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tách biệt logic search để database đánh index hiệu quả
            if (search != null && !search.isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                if ("name".equalsIgnoreCase(searchBy)) {
                    predicates.add(cb.like(cb.lower(root.get("subjectName")), searchPattern));
                } else {
                    // Mặc định search theo code
                    predicates.add(cb.like(cb.lower(root.get("subjectCode")), searchPattern));
                }
            }

            // 2. Các filter cố định (Static Filters)
            if (status != null) {
                // Nếu truyền true hoặc false -> Lọc theo giá trị đó
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // Nếu KHÔNG truyền status (status == null) -> Chỉ quét các môn đang biên soạn
                predicates.add(cb.isNull(root.get("status")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return subjectRepository.findAll(spec, pageable).map(subject -> {
            SubjectResponse response = subjectMapper.toSubjectResponse(subject);

            // 1. Bổ sung Electives từ bảng trung gian
            List<ElectiveResponse> electives = electiveSubjectRepository.findBySubject_SubjectId(subject.getSubjectId())
                    .stream()
                    .map(es -> electiveMapper.toElectiveResponse(es.getElective()))
                    .toList();
            response.setElectives(electives);

            // 2. Bổ sung Prerequisites
            List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(subject.getSubjectId())
                    .stream()
                    .map(prerequisiteMapper::toResponse)
                    .toList();
            response.setPreRequisite(prerequisites);

            return response;
        });
    }

    @Transactional
    public SubjectResponse update(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        subjectMapper.updateSubject(subject, request);

        //Check Department có tồn tại hay không
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        subject.setDepartment(department);

        return subjectMapper.toSubjectResponse(subjectRepository.save(subject));
    }

    public void delete(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        subject.setStatus(false); // Soft delete
        subjectRepository.save(subject);
    }

    @Transactional
    public SubjectResponse getDetail(UUID id) {
        // Sử dụng method findDetailById đã có JOIN FETCH
        Subject subject = subjectRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        SubjectResponse response = subjectMapper.toSubjectResponse(subject);

        List<PrerequisiteResponse> prerequisites = prerequisiteRepository.findBySubject_SubjectId(id)
                .stream()
                .map(prerequisiteMapper::toResponse)
                .toList();
        response.setPreRequisite(prerequisites);

        // Lấy danh sách Elective từ bảng trung gian Elective_Subject
        List<ElectiveResponse> electives = electiveSubjectRepository.findBySubject_SubjectId(id)
                .stream()
                .map(es -> electiveMapper.toElectiveResponse(es.getElective()))
                .toList();
        response.setElectives(electives);

        return response;
    }

    @Transactional
    public SubjectResponse publishSubject(UUID subjectId, String decisionNo) {
        // 1. Tìm môn học, nếu không thấy thì ném lỗi
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        // 2. Cập nhật các trường liên quan đến việc ban hành
        subject.setDecisionNo(decisionNo);       // Gán số quyết định ban hành
        subject.setIsApproved(true);             // Đánh dấu đã phê duyệt
        subject.setStatus(true);                 // Chuyển từ Draft (null) sang Active (true)
        subject.setApprovedDate(Instant.now());  // Lưu ngày phê duyệt (nếu có field này)

        // 3. Lưu và trả về response
        return subjectMapper.toSubjectResponse(subjectRepository.save(subject));
    }
}
