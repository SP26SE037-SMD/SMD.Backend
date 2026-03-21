package com.example.smd.services;

import com.example.smd.dto.request.MaterialRequest;
import com.example.smd.dto.response.MaterialResponse;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Material;
import com.example.smd.entities.Syllabus;
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
    MaterialMapper materialMapper;

    // 1. Create
    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        Syllabus syllabus = syllabusRepository.findById(request.getSyllabusId())
                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));

        Material material = materialMapper.toEntity(request);
        material.setStatus("DRAFT");
        material.setSyllabus(syllabus);
        material.setUploadedAt(Instant.now());

        return materialMapper.toResponse(materialRepository.save(material));
    }

    // 2. Update
    @Transactional
    public MaterialResponse update(UUID id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

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
        material.setStatus(status.toString());
        return materialMapper.toResponse(materialRepository.save(material));
    }

    // 4. Delete
    @Transactional
    public void delete(UUID id) {
        try {
            // Kiểm tra xem Material có tồn tại không
            Material material = materialRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));
            if(material.getStatus().equals("DRAFT")) {
                materialRepository.delete(material);
            } else{
                material.setStatus("ARCHIVED");
                materialRepository.save(material);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    // 5. Get All by SyllabusId
    public List<MaterialResponse> getAllBySyllabus(UUID syllabusId) {
        if (!syllabusRepository.existsById(syllabusId)) {
            throw new AppException(ErrorCode.SYLLABUS_NOT_FOUND);
        }
        return materialRepository.findAllBySyllabus_SyllabusId(syllabusId).stream()
                .map(materialMapper::toResponse)
                .toList();
    }
}
