package com.example.smd.services;

import com.example.smd.dto.request.session.SessionRequest;
import com.example.smd.dto.request.session.SessionNumberListRequest;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.entities.Session;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.*;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SessionMapper;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String SOFT_DELETE_STATUS = "ARCHIVED";

    private final AccountService accountService;
    private final SessionRepository sessionRepository;
    private final SyllabusRepository syllabusRepository;
    private final SessionMapper sessionMapper;
    private final SessionRegulationValidationService sessionRegulationValidationService;

    @Transactional(readOnly = true)
    public Page<SessionResponse> getAllSessions(UUID syllabusId,
                                                String status,
                                                String search,
                                                int page,
                                                int size,
                                                String[] sort,
                                                String accountId) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] parsedSort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(parsedSort[1]), parsedSort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!status.equals(MaterialStatus.PUBLISHED.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(status)) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        Specification<Session> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (syllabusId != null) {
                predicates.add(cb.equal(root.get("syllabus").get("syllabusId"), syllabusId));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("sessionTitle"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("content"), "")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("teachingMethods"), "")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sessionRepository.findAll(specification, pagingSort)
                .map(sessionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSessionById(UUID sessionId, String accountId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // 3. Logic Phân quyền:
        // Nếu là STUDENT hoặc LECTURER, chỉ cho phép xem nếu status là PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!MaterialStatus.PUBLISHED.toString().equalsIgnoreCase(session.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if ("DRAFT".equals(session.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(session.getStatus())) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return sessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessionsBySyllabus(UUID syllabusId) {
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        return sessionRepository.findBySyllabus_SyllabusIdOrderBySessionNumberAsc(syllabusId)
                .stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(syllabus.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(syllabus.getStatus()))) {
            throw new AppException(ErrorCode.SESSION_CANNOT_CREATE);
        }

        if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumber(
                request.getSyllabusId(), request.getSessionNumber())) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

        String newType = "";
        if (SessionType.THEORY.toString().equals(request.getSessionType())) {
            newType = SessionType.THEORY.toString();
        } else if (SessionType.PRACTICE.toString().equals(request.getSessionType())) {
            newType = SessionType.PRACTICE.toString();
        } else if (SessionType.SELF_STUDY.toString().equals(request.getSessionType())) {
            newType = SessionType.SELF_STUDY.toString();
        }

        Session session = sessionMapper.toEntity(request);
        session.setSessionType(newType);
        session.setSyllabus(syllabus);
        session.setStatus(DEFAULT_STATUS);

        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, SessionRequest request, String accountId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!("DRAFT".equals(session.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(session.getStatus()))) {
            throw new AppException(ErrorCode.SESSION_NOT_EDITABLE);
        }

        String newType = "";
        if (SessionType.THEORY.toString().equals(request.getSessionType())) {
            newType = SessionType.THEORY.toString();
        } else if (SessionType.PRACTICE.toString().equals(request.getSessionType())) {
            newType = SessionType.PRACTICE.toString();
        } else if (SessionType.SELF_STUDY.toString().equals(request.getSessionType())) {
            newType = SessionType.SELF_STUDY.toString();
        }

        if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumberAndSessionIdNot(
                request.getSyllabusId(), request.getSessionNumber(), sessionId)) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

//        sessionRegulationValidationService.validateDurationByRegulation(
//            request.getSyllabusId(),
//            request.getDuration(),
//            sessionId
//        );

        session.setSyllabus(syllabus);
        session.setSessionType(newType);
        sessionMapper.updateEntity(session, request);

        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public SessionResponse updateSessionStatus(UUID sessionId, String status) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (status == null || status.trim().isEmpty()) {
            throw new AppException(ErrorCode.SESSION_STATUS_REQUIRED);
        }

        session.setStatus(status.trim());
        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public boolean deleteSession(UUID sessionId, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.PDCM.toString().equals(roleName) || RoleName.COLLABORATOR.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(session.getSyllabus().getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(session.getSyllabus().getStatus()))) {
            throw new AppException(ErrorCode.SESSION_NOT_EDITABLE);
        }

        if (DEFAULT_STATUS.equalsIgnoreCase(session.getStatus())) {
            sessionRepository.delete(session);
            return true;
        }

        session.setStatus(SOFT_DELETE_STATUS);
        sessionRepository.save(session);
        return true;
    }

    @Transactional
    public boolean deleteSessionListBySyllabusAndSessionNumbers(UUID syllabusId, SessionNumberListRequest request) {
        if (request == null || request.getSessionNumbers() == null || request.getSessionNumbers().isEmpty()) {
            throw new AppException(ErrorCode.SESSION_NUMBER_LIST_REQUIRED);
        }

        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        List<Integer> distinctNumbers = request.getSessionNumbers().stream().distinct().toList();
        List<Session> sessions = sessionRepository.findBySyllabus_SyllabusIdAndSessionNumberIn(syllabusId, distinctNumbers);

        if (sessions.size() != distinctNumbers.size()) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND);
        }

        boolean hasNonDraft = sessions.stream()
                .anyMatch(session -> !DEFAULT_STATUS.equalsIgnoreCase(session.getStatus()));
        if (hasNonDraft) {
            throw new AppException(ErrorCode.SESSION_NOT_DRAFT);
        }

        sessionRepository.deleteAll(sessions);
        return true;
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        }
        if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }

    @jakarta.transaction.Transactional
    public void updateSessionStatusBySyllabus(String syllabusId, String newStatus) {
        // 1. Kiểm tra trạng thái hợp lệ từ Enum SyllabusStatus (hoặc MaterialStatus nếu bạn có riêng)
        SyllabusStatus status;
        try {
            status = SyllabusStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_MATERIAL_STATUS);
        }

        UUID uuidSyllabusId = UUID.fromString(syllabusId);

        // 2. Kiểm tra Syllabus có tồn tại không trước khi update Material
        if (!syllabusRepository.existsById(uuidSyllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        // 3. Cập nhật hàng loạt trạng thái các Materials thuộc Syllabus này
        // Lưu ý: Material đi theo Syllabus nên ta dùng updateStatusBySyllabusId
        int affectedRows = sessionRepository.updateStatusBySyllabusId(status.toString(), uuidSyllabusId);
    }
}
