package com.example.smd.services;

import com.example.smd.dto.excel.ComboImportDTO;
import com.example.smd.dto.request.ComboRequest;
import com.example.smd.dto.response.ComboResponse;
import com.example.smd.dto.response.combo.ImportComboResponse;
import com.example.smd.dto.response.combo.ImportComboResult;
import com.example.smd.entities.Combo;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.ComboMapper;
import com.example.smd.repositories.ComboRepository;
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
public class ComboService {

    private final ComboRepository comboRepository;
    private final ComboMapper comboMapper;

    // GetAll combo: lọc theo type trước, sau đó mới tìm theo searchBy (code/name)
    @Transactional(readOnly = true)
    public Page<ComboResponse> getAllCombos(
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
        Page<Combo> comboPage;
        String normalizedType = normalizeType(type);
        if (search == null || search.trim().isEmpty()) {
            // Không có search: trả danh sách theo type (hoặc tất cả nếu type rỗng/all)
            if (normalizedType == null) {
                comboPage = comboRepository.findAll(pagingSort);
            } else {
                comboPage = comboRepository.findByType(normalizedType, pagingSort);
            }
        } else {
            String searchTerm = search.trim();
            String normalizedSearchBy = normalizeSearchBy(searchBy);

            // Có search: chỉ tìm theo searchBy = code/name
            switch (normalizedSearchBy) {
                case "code":
                    comboPage = normalizedType == null
                            ? comboRepository.findByComboCodeContaining(searchTerm, pagingSort)
                            : comboRepository.findByTypeAndComboCodeContaining(normalizedType, searchTerm, pagingSort);
                    break;
                case "name":
                default:
                    comboPage = normalizedType == null
                            ? comboRepository.findByComboNameContaining(searchTerm, pagingSort)
                            : comboRepository.findByTypeAndComboNameContaining(normalizedType, searchTerm, pagingSort);
                    break;
            }
        }

        // 3. Map nguyên Page<Entity> sang Page<DTO>
        return comboPage.map(comboMapper::toResponse);
    }

    // Lấy chi tiết combo theo ID
    @Transactional(readOnly = true)
    public ComboResponse getComboById(String comboId) {
        UUID id = UUID.fromString(comboId);
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
        return comboMapper.toResponse(combo);
    }

    // Tạo combo mới
    @Transactional
    public ComboResponse createCombo(ComboRequest request) {
        // Kiểm tra combo code đã tồn tại chưa
        if (comboRepository.existsByComboCode(request.getComboCode())) {
            throw new AppException(ErrorCode.COMBO_CODE_EXISTS);
        }

        // Tạo entity Combo
        Combo combo = comboMapper.toEntity(request);

        // Lưu combo
        combo = comboRepository.save(combo);
        log.info("Created new combo with ID: {}", combo.getComboId());

        return comboMapper.toResponse(combo);
    }

    // Cập nhật thông tin combo
    @Transactional
    public ComboResponse updateCombo(String comboId, ComboRequest request) {
        UUID id = UUID.fromString(comboId);
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        // Kiểm tra nếu combo code thay đổi và đã tồn tại
        if (!combo.getComboCode().equals(request.getComboCode())
                && comboRepository.existsByComboCode(request.getComboCode())) {
            throw new AppException(ErrorCode.COMBO_CODE_EXISTS);
        }

        // Cập nhật thông tin
        comboMapper.updateEntityFromRequest(combo, request);
        combo = comboRepository.save(combo);

        log.info("Updated combo with ID: {}", combo.getComboId());
        return comboMapper.toResponse(combo);
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
        if ("combo".equalsIgnoreCase(type.trim())) {
            return "Combo";
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
    public ImportComboResponse importCombos(MultipartFile file) {
        List<ImportComboResult> details = new ArrayList<>();
        List<Combo> combosToSave = new ArrayList<>();
        Set<String> comboCodesInFile = new HashSet<>();

        try {
            List<ComboImportDTO> rows = ExcelImporter.importFromExcel(file, ComboImportDTO.class);

            for (ComboImportDTO row : rows) {
                String comboCode = trim(row.getComboCode());
                String comboName = trim(row.getComboName());
                String description = trim(row.getDescription());
                String type = trim(row.getType());

                if (comboCode == null) {
                    details.add(ImportComboResult.builder()
                            .comboCode(null)
                            .status("FAILED")
                            .message("Missing required field: Combo Code")
                            .build());
                    continue;
                }

                if (!comboCodesInFile.add(comboCode.toUpperCase())) {
                    details.add(ImportComboResult.builder()
                            .comboCode(comboCode)
                            .status("FAILED")
                            .message("Duplicate combo code in file")
                            .build());
                    continue;
                }

                if (comboRepository.existsByComboCode(comboCode)) {
                    details.add(ImportComboResult.builder()
                            .comboCode(comboCode)
                            .status("FAILED")
                            .message("Combo code already exists")
                            .build());
                    continue;
                }

                Combo combo = Combo.builder()
                        .comboCode(comboCode)
                        .comboName(comboName)
                        .description(description)
                        .type(type)
                        .createdAt(java.time.Instant.now())
                        .build();

                combosToSave.add(combo);
                details.add(ImportComboResult.builder()
                        .comboCode(comboCode)
                        .status("SUCCESS")
                        .message("Created successfully")
                        .build());
            }

            comboRepository.saveAll(combosToSave);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Import combo failed: " + e.getMessage());
        }

        int total = details.size();
        int success = (int) details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        int failed = total - success;

        return ImportComboResponse.builder()
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
