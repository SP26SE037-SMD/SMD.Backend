package com.example.smd.controller;

import com.example.smd.dto.request.GroupRequest;
import com.example.smd.dto.response.GroupResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.dto.response.ResponseObject;
import com.example.smd.dto.response.group.ImportGroupResponse;
import com.example.smd.services.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Group", description = "Group Management APIs")
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupService groupService;

    // API lấy danh sách group có phân trang và tìm kiếm
    @GetMapping
    @Operation(
                summary = "Get all groups with pagination, search, and type filter",
                description = "Filter by type (Group/Elective) first, then search by group code or group name. " +
                "Sort format: field 1 là tên trường (groupCode, groupName, type), " +
                "field 2 là hướng sắp xếp (asc hoặc desc). " +
                "Ví dụ: sort=groupCode,asc"
    )
    public ResponseObject<PagedResponse<GroupResponse>> getAllGroups(
            @RequestParam(required = false, name = "search")
            @io.swagger.v3.oas.annotations.Parameter(
                description = "Search keyword for code or name"
            ) String search,

            @RequestParam(required = false, name = "searchBy")
            @Parameter(
                                description = "Search type: 'code' hoặc 'name'"
            ) String searchBy,

            @RequestParam(required = false, name = "type")
            @Parameter(
                    description = "Type filter: 'Group', 'Elective', or 'all'"
            ) String type,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "groupCode,asc") String[] sort
    ) {
        return ResponseObject.<PagedResponse<GroupResponse>>builder()
                .status(1000)
                .data(PagedResponse.of(groupService.getAllGroups(search, searchBy, type, page, size, sort)))
                .message("Get all groups successfully")
                .build();
    }

    // API lấy chi tiết group theo ID
    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID")
    public ResponseObject<GroupResponse> getGroupById(@PathVariable String id) {
        return ResponseObject.<GroupResponse>builder()
                .status(1000)
                .data(groupService.getGroupById(id))
                .message("Get group successfully")
                .build();
    }

    // API tạo group mới
    @PostMapping
        @PreAuthorize("hasAuthority('GROUP_CREATE')")
    @Operation(summary = "Create new group")
    public ResponseObject<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request) {
        return ResponseObject.<GroupResponse>builder()
                .status(1000)
                .data(groupService.createGroup(request))
                .message("Create group successfully")
                .build();
    }

    // API cập nhật group theo ID
        @PreAuthorize("hasAuthority('GROUP_UPDATE')")
    @PutMapping("/{id}")
    @Operation(summary = "Update group by ID")
    public ResponseObject<GroupResponse> updateGroup(
            @PathVariable String id,
            @Valid @RequestBody GroupRequest request) {
        return ResponseObject.<GroupResponse>builder()
                .status(1000)
                .data(groupService.updateGroup(id, request))
                .message("Update group successfully")
                .build();
    }

    @PostMapping(value = "/import", consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('GROUP_CREATE')")
    @Operation(summary = "Import groups from Excel")
    public ResponseObject<ImportGroupResponse> importGroups(@RequestParam("file") MultipartFile file) {
        return ResponseObject.<ImportGroupResponse>builder()
                .status(1000)
                .data(groupService.importGroups(file))
                .message("Import groups successfully")
                .build();
    }
}
