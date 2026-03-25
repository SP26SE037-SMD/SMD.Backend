package com.example.smd.services;

import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.response.BlockResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.entities.Blocks;
import com.example.smd.entities.Material;
import com.example.smd.enums.SyllabusStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.BlockMapper;
import com.example.smd.repositories.BlockRepository;
import com.example.smd.repositories.EmbeddingRepository;
import com.example.smd.repositories.MaterialRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlockService {
    BlockRepository blockRepository;
    MaterialRepository materialRepository;
    EmbeddingService embeddingService;
    BlockMapper blockMapper;

    // 1. Create List Blocks (Bulk Insert)
    @Transactional
    public List<BlockResponse> createBlocks(UUID materialId, List<BlockRequest> requests) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        if(!("DRAFT".equals(material.getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(material.getStatus()))) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }

        List<Blocks> blocksList = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            Blocks block = blockMapper.toEntity(requests.get(i));
            block.setMaterial(material);
            block.setIdx(i); // Số tăng dần theo thứ tự mảng
            blocksList.add(block);
        }

        // 1. Lưu danh sách Blocks vào DB trước để có ID
        List<Blocks> savedBlocks = blockRepository.saveAll(blocksList);

        // 2. Với mỗi block đã lưu, tạo Embedding tương ứng
        for (Blocks block : savedBlocks) {
            // Chỉ tạo embedding nếu nội dung text không rỗng
            if (block.getContentText() != null && !block.getContentText().isBlank()) {
                embeddingService.createEmbedding(block.getContentText(), block);
            }
        }

        return savedBlocks.stream()
                .map(blockMapper::toResponse)
                .toList();
    }

    // 2. Get All by Material (Sắp xếp theo idx)
    public List<BlockResponse> getAllByMaterial(UUID materialId) {
        return blockRepository.findAllByMaterial_MaterialIdOrderByIdxAsc(materialId).stream()
                .map(blockMapper::toResponse)
                .toList();
    }

    // 3. Update Single Block
    @Transactional
    public BlockResponse updateBlock(UUID blockId, BlockRequest request) {
        Blocks block = blockRepository.findById(blockId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));

        blockMapper.updateBlock(block, request);
        return blockMapper.toResponse(blockRepository.save(block));
    }

    // 4. Delete
    @Transactional
    public void delete(UUID blockId) {
        Blocks block = blockRepository.findById(blockId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));

        if(!("DRAFT".equals(block.getMaterial().getStatus()) || SyllabusStatus.REVISION_REQUESTED.toString().equals(block.getMaterial().getStatus()))) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }
        blockRepository.deleteById(blockId);
    }

    public PagedResponse<BlockResponse> getAllByMaterial(UUID materialId, int page, int size) {
        // Sắp xếp theo idx tăng dần để đảm bảo thứ tự nội dung
        Sort sort = Sort.by("idx").ascending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // Lấy dữ liệu từ Repo (Trả về Page<Blocks>)
        Page<Blocks> blocksPage = blockRepository.findAllByMaterial_MaterialId(materialId, pageable);

        // Map Entity Page sang DTO Page
        Page<BlockResponse> responsePage = blocksPage.map(blockMapper::toResponse);

        // Sử dụng hàm static 'of' từ PagedResponse của bạn
        return PagedResponse.of(responsePage);
    }

    // Lấy chi tiết một Block
    public BlockResponse getDetail(UUID blockId) {
        Blocks block = blockRepository.findById(blockId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));

        return blockMapper.toResponse(block);
    }
}