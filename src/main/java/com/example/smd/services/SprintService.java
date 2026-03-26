package com.example.smd.services;

import com.example.smd.dto.request.sprint.SprintRequest;
import com.example.smd.dto.response.sprint.SprintResponse;
import com.example.smd.enums.SprintStatus;
import com.example.smd.entities.Sprint;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.SprintMapper;
import com.example.smd.repositories.SprintRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SprintService {
    AccountService accountService;
    SprintRepository sprintRepository;
    SprintMapper sprintMapper;

    @Transactional
    public SprintResponse create(SprintRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!("HOCFDC".equals(roleName) || "HOPDC".equals(roleName))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Sprint sprint = sprintMapper.toSprint(request);

        if (sprint.getStatus() == null || sprint.getStatus().isEmpty()) {
            sprint.setStatus("Planning");
        }

        sprint = sprintRepository.save(sprint);
        return sprintMapper.toSprintResponse(sprint);
    }

    public Page<SprintResponse> getAll(String search, String status, Pageable pageable) {
        Page<Sprint> pageData;
        if (search != null && !search.isEmpty()) {
            pageData = sprintRepository.findBySprintNameContainingIgnoreCase(search, pageable);
        } else if (status != null && !status.isEmpty()) {
            pageData = sprintRepository.findByStatus(status, pageable);
        } else {
            pageData = sprintRepository.findAll(pageable);
        }

        return pageData.map(sprintMapper::toSprintResponse);
    }

    public SprintResponse getDetail(UUID id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));
        return sprintMapper.toSprintResponse(sprint);
    }

    @Transactional
    public SprintResponse update(UUID id, SprintRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!("HOCFDC".equals(roleName) || "HOPDC".equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));

        sprintMapper.updateSprint(sprint, request);

        sprint = sprintRepository.save(sprint);
        return sprintMapper.toSprintResponse(sprint);
    }

    @Transactional
    public void delete(UUID id, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!("HOCFDC".equals(roleName) || "HOPDC".equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        if (!sprintRepository.existsById(id)) {
            throw new AppException(ErrorCode.SPRINT_NOT_FOUND);
        }
        sprintRepository.deleteById(id);
    }

    @Transactional
    public SprintResponse updateStatus(UUID id, String status) {
        boolean isValid = java.util.Arrays.stream(SprintStatus.values())
                .anyMatch(s -> s.name().equalsIgnoreCase(status));
        if (!isValid) {
            throw new AppException(ErrorCode.INVALID_SPRINT_STATUS);
        }

        SprintStatus sprintStatus = SprintStatus.valueOf(status.toUpperCase());

        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPRINT_NOT_FOUND));

        sprint.setStatus(sprintStatus.name());
        sprint = sprintRepository.save(sprint);
        return sprintMapper.toSprintResponse(sprint);
    }
}
