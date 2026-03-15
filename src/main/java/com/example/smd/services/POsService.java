package com.example.smd.services;

import com.example.smd.dto.request.po.POsCreateRequest;
import com.example.smd.dto.request.po.POsRequest;
import com.example.smd.dto.response.POsResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.PO;
import com.example.smd.mapper.POsMapper;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.POsRepository;
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

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class POsService {
    POsRepository poRepository;
    MajorRepository majorRepository;
    POsMapper poMapper;

    @Transactional
    public List<POsResponse> createBulkPos(String majorId, List<POsCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) return Collections.emptyList();

        // 1. Kiểm tra Major tồn tại
        UUID uuidMajorId = UUID.fromString(majorId);
        Major major = majorRepository.findById(uuidMajorId)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        // 2. Check trùng mã nội bộ trong JSON gửi lên
        Set<String> uniqueCodes = new HashSet<>();
        for (POsCreateRequest req : requests) {
            if (!uniqueCodes.add(req.getPoCode())) {
                throw new AppException(ErrorCode.PO_CODE_EXISTS);
            }
        }

        // 3. Check trùng mã với Database (Global Check cho riêng Major này)
        List<String> poCodes = requests.stream().map(POsCreateRequest::getPoCode).toList();
        if (poRepository.existsByPoCodeInAndMajor_MajorId(poCodes, uuidMajorId)) {
            throw new AppException(ErrorCode.PO_CODE_EXISTS);
        }

        // 4. Map và Save hàng loạt
        List<PO> posToSave = requests.stream().map(request -> {
            PO po = poMapper.toPoCreate(request); // Đảm bảo Mapper của bạn map từ POsCreateRequest
            po.setMajor(major);
            po.setStatus(PloStatus.DRAFT.toString());
            return po;
        }).toList();

        return poRepository.saveAll(posToSave).stream()
                .map(poMapper::toPoResponse)
                .toList();
    }

    @Transactional
    public POsResponse updatePo(String id, POsRequest request) {
        UUID poId = UUID.fromString(id);
        PO po = poRepository.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));

        if (!po.getPoCode().equals(request.getPoCode()) &&
                poRepository.existsByPoCodeAndMajor_MajorId(request.getPoCode(), po.getMajor().getMajorId())) {
            throw new AppException(ErrorCode.PO_CODE_EXISTS);
        }

        po.setPoCode(request.getPoCode());
        po.setDescription(request.getDescription());

        return poMapper.toPoResponse(poRepository.save(po));
    }

    public POsResponse getPoDetail(String id) {
        return poRepository.findById(UUID.fromString(id))
                .map(poMapper::toPoResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));
    }

    public Page<POsResponse> getPosByMajor(String majorId, int page, int size) {
        UUID id = UUID.fromString(majorId);
        if (!majorRepository.existsById(id)) {
            throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("poCode").ascending());
        return poRepository.findByMajor_MajorId(id, pageable)
                .map(poMapper::toPoResponse);
    }

    @Transactional
    public void deletePo(String id) {
        PO po = poRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));

        // Soft Delete (Archive)
        po.setStatus(PloStatus.ARCHIVED.toString());
        poRepository.save(po);
    }

    @Transactional
    public void updateStatusByMajor(String majorId, String newStatus) {
        PloStatus status;
        try {
            status = PloStatus.valueOf(newStatus.toUpperCase());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_PO_STATUS);
        }

        UUID uuidMajorId = UUID.fromString(majorId);
        if (!majorRepository.existsById(uuidMajorId)) {
            throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
        }

        int affectedRows = poRepository.updateStatusByMajorId(status.toString(), uuidMajorId);
        if (affectedRows == 0) {
            throw new AppException(ErrorCode.PO_NOT_FOUND);
        }
    }
}
