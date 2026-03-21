package com.example.smd.mapper;

import com.example.smd.dto.request.GroupRequest;
import com.example.smd.dto.response.GroupResponse;
import com.example.smd.entities.Group;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    // Chuyển đổi từ Entity Group sang DTO GroupResponse
    public GroupResponse toResponse(Group group) {
        if (group == null) {
            return null;
        }

        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .groupCode(group.getGroupCode())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .createdAt(group.getCreatedAt())
                .type(group.getType())
                .build();
    }

    // Chuyển đổi từ DTO GroupRequest sang Entity Group
    public Group toEntity(GroupRequest request) {
        if (request == null) {
            return null;
        }

        return Group.builder()
                .groupCode(request.getGroupCode())
                .groupName(request.getGroupName())
                .description(request.getDescription())
                .createdAt(java.time.Instant.now())
                .type(request.getType())
                .build();
    }

    // Cập nhật entity từ request
    public void updateEntityFromRequest(Group group, GroupRequest request) {
        if (group == null || request == null) {
            return;
        }

        group.setGroupCode(request.getGroupCode());
        group.setGroupName(request.getGroupName());
        group.setDescription(request.getDescription());
        group.setType(request.getType());
    }
}
