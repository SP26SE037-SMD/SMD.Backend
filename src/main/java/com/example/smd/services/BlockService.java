package com.example.smd.services;

import com.example.smd.dto.request.BlockRequest;
import com.example.smd.dto.request.BlockSingleRequest;
import com.example.smd.dto.request.UpdateBlockRequest;
import com.example.smd.dto.response.BlockResponse;
import com.example.smd.dto.response.BlockSimpleResponse;
import com.example.smd.dto.response.PagedResponse;
import com.example.smd.entities.Blocks;
import com.example.smd.entities.Material;
import com.example.smd.enums.MaterialStatus;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

        if(!("DRAFT".equals(material.getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(material.getStatus()))) {
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

    @Transactional
    public BlockResponse createSingleBlock(BlockSingleRequest blockRequest, UUID materialId) {
        // 1. Kiểm tra Material có tồn tại không
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        // 2. "Đẩy" các block cũ từ vị trí idx trở đi lên +1
        // Việc này tạo ra một "khoảng trống" tại vị trí blockRequest.getIdx()
        blockRepository.shiftBlocksIndex(blockRequest.getIdx(), materialId);

        // 3. Tạo block mới tại vị trí idx đã trống
        Blocks newBlock = Blocks.builder()
                .blockStyle(blockRequest.getBlockStyle())
                .blockType(blockRequest.getBlockType())
                .contentText(blockRequest.getContentText())
                .idx(blockRequest.getIdx()) // Vị trí mong muốn
                .material(material)
                .build();

        Blocks savedBlock = blockRepository.save(newBlock);

        return blockMapper.toResponse(savedBlock);
    }

    // 2. Get All by Material (Sắp xếp theo idx)
    public List<BlockResponse> getAllByMaterial(UUID materialId) {
        return blockRepository.findAllByMaterial_MaterialIdOrderByIdxAsc(materialId).stream()
                .map(blockMapper::toResponse)
                .toList();
    }

    public List<BlockSimpleResponse> getBlocksByMaterialAndStyle(UUID materialId, String blockStyle) {
        if (!materialRepository.existsById(materialId)) {
            throw new AppException(ErrorCode.MATERIAL_NOT_FOUND);
        }

        return blockRepository.findAllByMaterial_MaterialIdAndBlockStyleIgnoreCaseOrderByIdxAsc(materialId, blockStyle)
                .stream()
                .map(block -> BlockSimpleResponse.builder()
                        .blockId(block.getBlockId())
                        .blockName(block.getContentText())
                        .idx(block.getIdx())
                        .build())
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

    @Transactional
    public List<BlockResponse> updateBlocks(List<UpdateBlockRequest> requests) {
        List<Blocks> updatedBlocks = new ArrayList<>();

        for (UpdateBlockRequest request : requests) {
            // 1. Tìm từng block theo ID trong request
            Blocks block = blockRepository.findById(request.getBlockId())
                    .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));

            // 2. Sử dụng Mapper để map dữ liệu từ request vào entity hiện tại
            blockMapper.updateBlockList(block, request);

            // 3. Thêm vào danh sách chờ lưu
            updatedBlocks.add(block);
        }

        // 4. Lưu tất cả trong một transaction (Tối ưu performance hơn lưu lẻ tẻ)
        List<Blocks> savedBlocks = blockRepository.saveAll(updatedBlocks);

        return savedBlocks.stream()
                .map(blockMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 4. Delete
    @Transactional
    public void delete(UUID blockId) {
        Blocks block = blockRepository.findById(blockId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));

        if(!("DRAFT".equals(block.getMaterial().getStatus()) || MaterialStatus.REVISION_REQUESTED.toString().equals(block.getMaterial().getStatus()))) {
            throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
        }
        blockRepository.deleteById(blockId);

        List<Blocks> remainingBlocks = blockRepository
                .findAllByMaterial_MaterialIdOrderByIdxAsc(block.getMaterial().getMaterialId());

        // Cập nhật lại idx từ 0, 1, 2...
        for (int i = 0; i < remainingBlocks.size(); i++) {
            remainingBlocks.get(i).setIdx(i);
        }
    }

    @Transactional
    public void deleteBlocks(List<UUID> blockIds) {
        if (blockIds == null || blockIds.isEmpty()) return;

        // 1. Lấy thông tin các block sắp xóa để biết chúng thuộc Material nào
        List<Blocks> blocksToDelete = blockRepository.findAllById(blockIds);
        if (blocksToDelete.isEmpty()) {
            throw new AppException(ErrorCode.BLOCK_NOT_FOUND);
        }

        // 2. Kiểm tra trạng thái Material (Chỉ lấy các Material duy nhất để check)
        Set<Material> materialsToUpdate = blocksToDelete.stream()
                .map(Blocks::getMaterial)
                .collect(Collectors.toSet());

        for (Material material : materialsToUpdate) {
            String status = material.getStatus();
            if (!("DRAFT".equals(status) || "REVISION_REQUESTED".equals(status))) {
                throw new AppException(ErrorCode.MATERIAL_NOT_EDITABLE);
            }
        }

        // 3. Thực hiện xóa danh sách Block
        blockRepository.deleteAll(blocksToDelete);

        // 4. Quan trọng: Đánh số lại idx cho các Material bị ảnh hưởng để tránh "hổng" thứ tự
        for (Material material : materialsToUpdate) {
            List<Blocks> remainingBlocks = blockRepository
                    .findAllByMaterial_MaterialIdOrderByIdxAsc(material.getMaterialId());

            // Cập nhật lại idx từ 0, 1, 2...
            for (int i = 0; i < remainingBlocks.size(); i++) {
                remainingBlocks.get(i).setIdx(i);
            }

            blockRepository.saveAll(remainingBlocks);
        }
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
