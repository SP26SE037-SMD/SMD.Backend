package com.example.smd.services;

import com.example.smd.dto.request.MaterialRequest;
import com.example.smd.dto.response.MaterialResponse;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Material;
import com.example.smd.entities.Syllabus;
import com.example.smd.enums.MaterialStatus;
import com.example.smd.enums.PloStatus;
import com.example.smd.enums.RoleName;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MaterialMapper;
import com.example.smd.repositories.MaterialRepository;
import com.example.smd.repositories.SyllabusRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaterialService {
    MaterialRepository materialRepository;
    SyllabusRepository syllabusRepository;
    AccountService accountService;
    MaterialMapper materialMapper;

    // 1. Create
    @Transactional
    public MaterialResponse create(MaterialRequest request, String accountId) {

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.COLLABORATOR.toString().equals(roleName) || RoleName.PDCM.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(syllabus.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(syllabus.getStatus()))) {
            throw new AppException(ErrorCode.MATERIAL_CANNOT_CREATE);
        }

        Material material = materialMapper.toEntity(request);
        material.setStatus("DRAFT");
        material.setSyllabus(syllabus);
        material.setUploadedAt(Instant.now());

        int nextVersion = materialRepository.findLatestMaterialByIdAndSyllabusId(request.getId(), request.getSyllabusId())
                .map(latest -> latest.getVersion() + 1)
                .orElse(1); // Nếu chưa có thì là bản đầu tiên (V1)
        material.setVersion(nextVersion);

        return materialMapper.toResponse(materialRepository.save(material));
    }

    // 2. Update
    @Transactional
    public MaterialResponse update(UUID id, MaterialRequest request, String accountId) {
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.COLLABORATOR.toString().equals(roleName) || RoleName.PDCM.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        if (!("DRAFT".equals(material.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(material.getStatus()))) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }

        materialMapper.updateMaterial(material, request);

        return materialMapper.toResponse(materialRepository.save(material));
    }

    // 3. Update Status
    @Transactional
    public MaterialResponse updateStatus(UUID id, String newStatus) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));
        SyllabusStatus status;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            status = SyllabusStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_MATERIAL_STATUS);
        }

        if (SyllabusStatus.ARCHIVED.toString().equals(material.getStatus())) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_EDITABLE); // Không cho sửa đồ đã lưu trữ
        }

        material.setStatus(status.toString());
        return materialMapper.toResponse(materialRepository.save(material));
    }

    // 4. Delete
    @Transactional
    public void delete(UUID id, String accountId) {

        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!(RoleName.COLLABORATOR.toString().equals(roleName) || RoleName.PDCM.toString().equals(roleName))) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // Kiểm tra xem Material có tồn tại không
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        if (!(SyllabusStatus.IN_PROGRESS.toString().equals(material.getSyllabus().getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(material.getSyllabus().getStatus()))) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }

        if ("DRAFT".equals(material.getStatus())) {
            materialRepository.delete(material);
        } else {
            material.setStatus("ARCHIVED");
            materialRepository.save(material);
        }
    }

    // 5. Get All by SyllabusId
    @Transactional
    public List<MaterialResponse> getAllBySyllabus(UUID syllabusId, String status, String accountId) {
        // 1. Kiểm tra Syllabus tồn tại
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }

        // 2. Lấy Role để phân quyền
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // Chuẩn hóa status đầu vào
        String finalStatus = (status == null || status.trim().isEmpty()) ? null : status.trim().toUpperCase();

        // 3. Ép buộc Role thấp chỉ được xem PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!SyllabusStatus.PUBLISHED.toString().equals(finalStatus)) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(finalStatus)) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        // 4. Truy vấn dữ liệu
        List<Material> materials;
        if (finalStatus != null) {
            // Tìm theo Syllabus ID và Status cụ thể
            materials = materialRepository.findLatestMaterialsBySyllabus(syllabusId, finalStatus);
        } else {
            // Nếu không có filter status (chỉ dành cho Role cao), lấy toàn bộ
            materials = materialRepository.findLatestMaterialsBySyllabusId(syllabusId);
        }

        return materials.stream()
                .map(materialMapper::toResponse)
                .toList();
    }

    public List<MaterialResponse> getAllVersionsById(int id, UUID syllabusId) {

        List<Material> materials = materialRepository.findMaterialByIdAndSyllabusId(id, syllabusId);

        if (materials.isEmpty()) {
            throw new AppException(ErrorCode.MATERIAL_NOT_FOUND);
        }

        return materials.stream()
                .map(materialMapper::toResponse)
                .toList();
    }

    @Transactional
    public void updateMaterialStatusBySyllabus(String syllabusId, String newStatus) {
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
        int affectedRows = materialRepository.updateStatusBySyllabusId(status.toString(), uuidSyllabusId);
    }

    @Transactional
    public MaterialResponse getDetail(UUID materialId, String accountId) {
        // 1. Tìm Material hoặc báo lỗi 404
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        // 2. Lấy thông tin Account để check Role
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // 3. Logic Phân quyền:
        // Nếu là STUDENT hoặc LECTURER, chỉ cho phép xem nếu status là PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            if (!MaterialStatus.PUBLISHED.toString().equalsIgnoreCase(material.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if ("DRAFT".equals(material.getStatus()) ||MaterialStatus.REVISION_REQUESTED.toString().equals(material.getStatus())) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        // 4. Map và trả về Response
        return materialMapper.toResponse(material);
    }
}
