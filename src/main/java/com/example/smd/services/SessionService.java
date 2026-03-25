package com.example.smd.services;

import com.example.smd.dto.request.SessionRequest;
import com.example.smd.dto.request.SessionItemRequest;
import com.example.smd.dto.request.SessionNumberListRequest;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.entities.Session;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.SyllabusStatus;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final String DEFAULT_STATUS = "DRAFT";
    private static final String SOFT_DELETE_STATUS = "ARCHIVED";

    private final AccountService accountService;
    private final SessionRepository sessionRepository;
    private final SyllabusRepository syllabusRepository;
    private final SessionMapper sessionMapper;

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
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            if (!status.equals(SyllabusStatus.PUBLISHED.toString())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (status.equals(PloStatus.DRAFT.toString())) {
            if (!(account.getRole().getRoleName().equals("PDCM") || account.getRole().getRoleName().equals("COLLABORATOR"))) {
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
        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            if (!"PUBLISHED".equalsIgnoreCase(session.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (session.getStatus().equals("DRAFT") || session.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString())) {
            if (!(account.getRole().getRoleName().equals("PDCM") || account.getRole().getRoleName().equals("COLLABORATOR"))) {
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
        if (!(roleName.equals("COLLABORATOR") || roleName.equals("PDCM"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(syllabus.getStatus().equals("DRAFT") || syllabus.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.SESSION_CANNOT_CREATE);
        }

        if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumber(
                request.getSyllabusId(), request.getSessionNumber())) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

        Session session = sessionMapper.toEntity(request);
        session.setSyllabus(syllabus);
        session.setStatus(DEFAULT_STATUS);

        session = sessionRepository.save(session);
        return sessionMapper.toResponse(session);
    }

    @Transactional
    public List<SessionResponse> createSessionsBySyllabus(UUID syllabusId, List<SessionItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new AppException(ErrorCode.SESSION_LIST_REQUIRED);
        }

        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        Set<Integer> distinctSessionNumbers = requests.stream()
                .map(SessionItemRequest::getSessionNumber)
                .collect(java.util.stream.Collectors.toSet());

        if (distinctSessionNumbers.size() != requests.size()) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

        List<Session> sessionsToSave = new ArrayList<>();

        for (SessionItemRequest request : requests) {
            Session existing = sessionRepository
                    .findBySyllabus_SyllabusIdAndSessionNumber(syllabusId, request.getSessionNumber())
                    .orElse(null);

            if (existing == null) {
                Session newSession = Session.builder()
                        .syllabus(syllabus)
                        .sessionNumber(request.getSessionNumber())
                        .sessionTitle(request.getSessionTitle())
                        .content(request.getContent())
                        .teachingMethods(request.getTeachingMethods())
                        .duration(request.getDuration())
                        .status(DEFAULT_STATUS)
                        .build();
                sessionsToSave.add(newSession);
                continue;
            }

            if (!DEFAULT_STATUS.equalsIgnoreCase(existing.getStatus())) {
                throw new AppException(ErrorCode.SESSION_NOT_DRAFT);
            }

            boolean changed =
                    !java.util.Objects.equals(existing.getSessionTitle(), request.getSessionTitle()) ||
                    !java.util.Objects.equals(existing.getContent(), request.getContent()) ||
                    !java.util.Objects.equals(existing.getTeachingMethods(), request.getTeachingMethods()) ||
                    !java.util.Objects.equals(existing.getDuration(), request.getDuration());

            if (changed) {
                existing.setSessionTitle(request.getSessionTitle());
                existing.setContent(request.getContent());
                existing.setTeachingMethods(request.getTeachingMethods());
                existing.setDuration(request.getDuration());
                sessionsToSave.add(existing);
            }
        }

        return sessionRepository.saveAll(sessionsToSave)
                .stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    @Transactional
    public SessionResponse updateSession(UUID sessionId, SessionRequest request, String accountId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(roleName.equals("COLLABORATOR") || roleName.equals("PDCM"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(session.getStatus().equals("DRAFT") || session.getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
            throw new AppException(ErrorCode.SESSION_NOT_EDITABLE);
        }

        if (sessionRepository.existsBySyllabus_SyllabusIdAndSessionNumberAndSessionIdNot(
                request.getSyllabusId(), request.getSessionNumber(), sessionId)) {
            throw new AppException(ErrorCode.SESSION_NUMBER_EXISTS);
        }

        session.setSyllabus(syllabus);
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
        if (!(roleName.equals("COLLABORATOR") || roleName.equals("PDCM"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!(session.getSyllabus().getStatus().equals("DRAFT") || session.getSyllabus().getStatus().equals(SyllabusStatus.REVISION_REQUESTED.toString()))) {
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
}
