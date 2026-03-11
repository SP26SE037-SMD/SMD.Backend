package com.example.smd.services;

import com.example.smd.dto.request.ComboRequest;
import com.example.smd.dto.response.ComboResponse;
import com.example.smd.entities.Combo;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.ComboMapper;
import com.example.smd.repositories.ComboRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;
    private final ComboMapper comboMapper;

    // GetAll combo có phân trang và tìm kiếm theo combo code hoặc combo name
    @Transactional(readOnly = true)
    public Page<ComboResponse> getAllCombos(String search, String searchBy, int page, int size, String[] sort) {
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

        // 2. Logic tìm kiếm dựa trên searchBy parameter
        Page<Combo> comboPage;
        if (search == null || search.trim().isEmpty()) {
            // Không có search, lấy tất cả combos
            comboPage = comboRepository.findAll(pagingSort);
        } else {
            String searchTerm = search.trim();
            // Xác định loại tìm kiếm dựa trên searchBy
            switch (searchBy != null ? searchBy.toLowerCase() : "all") {
                case "code":
                    // Tìm theo combo code
                    comboPage = comboRepository.findByComboCodeContaining(searchTerm, pagingSort);
                    break;
                case "name":
                    // Tìm theo combo name
                    comboPage = comboRepository.findByComboNameContaining(searchTerm, pagingSort);
                    break;
                case "all":
                default:
                    comboPage = comboRepository.findAll(pagingSort);
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
}
