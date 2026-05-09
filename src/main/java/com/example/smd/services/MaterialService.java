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
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SyllabusRepository;
import org.springframework.transaction.annotation.Transactional;
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
    SessionRepository sessionRepository;
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

        if (!SyllabusStatus.IN_PROGRESS.toString().equals(material.getSyllabus().getStatus())) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }

        materialMapper.updateMaterial(material, request);

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

        if (!SyllabusStatus.IN_PROGRESS.toString().equals(material.getSyllabus().getStatus())) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }

        materialRepository.delete(material);
    }

    // 5. Get All by SyllabusId
    @Transactional
    public List<MaterialResponse> getAllBySyllabus(UUID syllabusId, String status, String accountId) {
        // 1. Kiểm tra Syllabus tồn tại
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

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
        if (finalStatus != null && !finalStatus.equals(syllabus.getStatus())) {
            return java.util.Collections.emptyList();
        }
        
        List<Material> materials = materialRepository.findLatestMaterialsBySyllabusId(syllabusId);

        return materials.stream()
                .map(materialMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
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
            if (!MaterialStatus.PUBLISHED.toString().equalsIgnoreCase(material.getSyllabus().getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (SyllabusStatus.IN_PROGRESS.toString().equals(material.getSyllabus().getStatus())) {
            if (!(RoleName.PDCM.toString().equals(account.getRole().getRoleName()) || RoleName.COLLABORATOR.toString().equals(account.getRole().getRoleName()))) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        // 4. Map và trả về Response
        return materialMapper.toResponse(material);
    }
}
