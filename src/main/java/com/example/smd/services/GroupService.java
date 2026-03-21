package com.example.smd.services;

import com.example.smd.dto.excel.GroupImportDTO;
import com.example.smd.dto.request.GroupRequest;
import com.example.smd.dto.response.GroupResponse;
import com.example.smd.dto.response.group.ImportGroupResponse;
import com.example.smd.dto.response.group.ImportGroupResult;
import com.example.smd.entities.Group;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.GroupMapper;
import com.example.smd.repositories.GroupRepository;
import com.example.smd.services.excelService.ExcelImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;

    // GetAll group: lọc theo type trước, sau đó mới tìm theo searchBy (code/name)
    @Transactional(readOnly = true)
    public Page<GroupResponse> getAllGroups(
            String search,
            String searchBy,
            String type,
            int page,
            int size,
            String[] sort
    ) {
        // 1. Xử lý sắp xếp
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(_sort[1]), _sort[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        Pageable pagingSort = PageRequest.of(page, size, Sort.by(orders));

        // 2. Logic: lọc theo type trước
        Page<Group> groupPage;
        String normalizedType = normalizeType(type);
        if (search == null || search.trim().isEmpty()) {
            // Không có search: trả danh sách theo type (hoặc tất cả nếu type rỗng/all)
            if (normalizedType == null) {
                groupPage = groupRepository.findAll(pagingSort);
            } else {
                groupPage = groupRepository.findByType(normalizedType, pagingSort);
            }
        } else {
            String searchTerm = search.trim();
            String normalizedSearchBy = normalizeSearchBy(searchBy);

            // Có search: chỉ tìm theo searchBy = code/name
            switch (normalizedSearchBy) {
                case "code":
                    groupPage = normalizedType == null
                            ? groupRepository.findByGroupCodeContaining(searchTerm, pagingSort)
                            : groupRepository.findByTypeAndGroupCodeContaining(normalizedType, searchTerm, pagingSort);
                    break;
                case "name":
                default:
                    groupPage = normalizedType == null
                            ? groupRepository.findByGroupNameContaining(searchTerm, pagingSort)
                            : groupRepository.findByTypeAndGroupNameContaining(normalizedType, searchTerm, pagingSort);
                    break;
            }
        }

        // 3. Map nguyên Page<Entity> sang Page<DTO>
        return groupPage.map(groupMapper::toResponse);
    }

    // Lấy chi tiết group theo ID
    @Transactional(readOnly = true)
    public GroupResponse getGroupById(String groupId) {
        UUID id = UUID.fromString(groupId);
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        return groupMapper.toResponse(group);
    }

    // Tạo group mới
    @Transactional
    public GroupResponse createGroup(GroupRequest request) {
        // Kiểm tra group code đã tồn tại chưa
        if (groupRepository.existsByGroupCode(request.getGroupCode())) {
            throw new AppException(ErrorCode.GROUP_CODE_EXISTS);
        }

        // Tạo entity Group
        Group group = groupMapper.toEntity(request);

        // Lưu group
        group = groupRepository.save(group);
        log.info("Created new group with ID: {}", group.getGroupId());

        return groupMapper.toResponse(group);
    }

    // Cập nhật thông tin group
    @Transactional
    public GroupResponse updateGroup(String groupId, GroupRequest request) {
        UUID id = UUID.fromString(groupId);
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        // Kiểm tra nếu group code thay đổi và đã tồn tại
        if (!group.getGroupCode().equals(request.getGroupCode())
                && groupRepository.existsByGroupCode(request.getGroupCode())) {
            throw new AppException(ErrorCode.GROUP_CODE_EXISTS);
        }

        // Cập nhật thông tin
        groupMapper.updateEntityFromRequest(group, request);
        group = groupRepository.save(group);

        log.info("Updated group with ID: {}", group.getGroupId());
        return groupMapper.toResponse(group);
    }

    // Helper method để xử lý hướng sắp xếp
    private Sort.Direction getSortDirection(String direction) {
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        } else if (direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        return Sort.Direction.ASC;
    }

    private String normalizeType(String type) {
        if (type == null || type.trim().isEmpty() || "all".equalsIgnoreCase(type.trim())) {
            return null;
        }
        if ("group".equalsIgnoreCase(type.trim())) {
            return "Group";
        }
        if ("elective".equalsIgnoreCase(type.trim())) {
            return "Elective";
        }
        return null;
    }

    private String normalizeSearchBy(String searchBy) {
        if (searchBy == null || searchBy.trim().isEmpty()) {
            return "name";
        }
        if ("code".equalsIgnoreCase(searchBy.trim())) {
            return "code";
        }
        return "name";
    }

    @Transactional
    public ImportGroupResponse importGroups(MultipartFile file) {
        List<ImportGroupResult> details = new ArrayList<>();
        List<Group> groupsToSave = new ArrayList<>();
        Set<String> groupCodesInFile = new HashSet<>();

        try {
            List<GroupImportDTO> rows = ExcelImporter.importFromExcel(file, GroupImportDTO.class);

            for (GroupImportDTO row : rows) {
                String groupCode = trim(row.getGroupCode());
                String groupName = trim(row.getGroupName());
                String description = trim(row.getDescription());
                String type = trim(row.getType());

                if (groupCode == null) {
                    details.add(ImportGroupResult.builder()
                            .groupCode(null)
                            .status("FAILED")
                            .message("Missing required field: Group Code")
                            .build());
                    continue;
                }

                if (!groupCodesInFile.add(groupCode.toUpperCase())) {
                    details.add(ImportGroupResult.builder()
                            .groupCode(groupCode)
                            .status("FAILED")
                            .message("Duplicate group code in file")
                            .build());
                    continue;
                }

                if (groupRepository.existsByGroupCode(groupCode)) {
                    details.add(ImportGroupResult.builder()
                            .groupCode(groupCode)
                            .status("FAILED")
                            .message("Group code already exists")
                            .build());
                    continue;
                }

                Group group = Group.builder()
                        .groupCode(groupCode)
                        .groupName(groupName)
                        .description(description)
                        .type(type)
                        .createdAt(java.time.Instant.now())
                        .build();

                groupsToSave.add(group);
                details.add(ImportGroupResult.builder()
                        .groupCode(groupCode)
                        .status("SUCCESS")
                        .message("Created successfully")
                        .build());
            }

            groupRepository.saveAll(groupsToSave);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import group failed: " + e.getMessage());
        }

        int total = details.size();
        int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = total - success;

        return ImportGroupResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .details(details)
                .build();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
