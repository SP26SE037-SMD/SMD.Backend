package com.example.smd.services;

import com.example.smd.dto.request.SubjectRequest;
import com.example.smd.dto.response.SubjectResponse;
import com.example.smd.entities.Subject;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SubjectMapper;
import com.example.smd.repositories.SubjectRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubjectService {
    SubjectRepository subjectRepository;
    SubjectMapper subjectMapper;

    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsBySubjectCode(request.getSubjectCode()))
            throw new AppException(ErrorCode.SUBJECT_CODE_EXISTS);

        Subject subject = subjectMapper.toSubject(request);
        return subjectMapper.toSubjectResponse(subjectRepository.save(subject));
    }

//     UUID deptId,
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
//            if (deptId != null) {
//                predicates.add(cb.equal(root.get("department").get("id"), deptId));
//            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return subjectRepository.findAll(spec, pageable).map(subjectMapper::toSubjectResponse);
    }

    public SubjectResponse update(UUID id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        subjectMapper.updateSubject(subject, request);
        return subjectMapper.toSubjectResponse(subjectRepository.save(subject));
    }

    public void delete(UUID id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
        subject.setStatus(false); // Soft delete
        subjectRepository.save(subject);
    }

    public SubjectResponse getDetail(UUID id) {
        // Sử dụng method findDetailById đã có JOIN FETCH
        Subject subject = subjectRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

        return subjectMapper.toSubjectResponse(subject);
    }
}
