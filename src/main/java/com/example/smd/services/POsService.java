package com.example.smd.services;

import com.example.smd.dto.request.po.POsCreateRequest;
import com.example.smd.dto.request.po.POsRequest;
import com.example.smd.dto.response.validate.ComplianceCheckResponse;
import com.example.smd.dto.response.POsResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.PO;
import com.example.smd.entities.Regulation;
import com.example.smd.enums.RoleName;
import com.example.smd.mapper.POsMapper;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.MajorRepository;
import com.example.smd.repositories.POsRepository;
import com.example.smd.repositories.RegulationRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class POsService {
    AccountService accountService;
    POsRepository poRepository;
    MajorRepository majorRepository;
    POsMapper poMapper;
    RegulationRepository regulationRepository;
    GeminiService geminiService;

    @Transactional
    public List<POsResponse> createBulkPos(String majorId, List<POsCreateRequest> requests, String accountId) {
        if (requests == null || requests.isEmpty())
            return Collections.emptyList();

        // 1. Kiểm tra Major tồn tại
        UUID uuidMajorId = UUID.fromString(majorId);
        Major major = majorRepository.findById(uuidMajorId)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.HOCFDC.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

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
    public POsResponse updatePo(String id, POsRequest request, String accountId) {

        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.VP.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        UUID poId = UUID.fromString(id);
        PO po = poRepository.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));

        if (!po.getPoCode().equals(request.getPoCode()) &&
                poRepository.existsByPoCodeAndMajor_MajorId(request.getPoCode(), po.getMajor().getMajorId())) {
            throw new AppException(ErrorCode.PO_CODE_EXISTS);
        }

        if (!PloStatus.DRAFT.toString().equals(po.getStatus().toUpperCase())) {
            throw new AppException(ErrorCode.PO_NOT_DRAFT);
        }

        po.setPoCode(request.getPoCode());
        po.setDescription(request.getDescription());

        return poMapper.toPoResponse(poRepository.save(po));
    }

    public POsResponse getPoDetail(String id, String accountId) {
        UUID poId = UUID.fromString(id);
        PO po = poRepository.findById(poId)
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));

        // Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if (RoleName.STUDENT.toString().equals(account.getRole().getRoleName())
                || RoleName.LECTURER.toString().equals(account.getRole().getRoleName())) {
            if (!PloStatus.PUBLISHED.toString().equals(po.getStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if (PloStatus.DRAFT.toString().equals(po.getStatus())) {
            if (!RoleName.VP.toString().equals(account.getRole().getRoleName())) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }
        return poRepository.findById(UUID.fromString(id))
                .map(poMapper::toPoResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));
    }

    public Page<POsResponse> getPosByMajor(String majorId, int page, int size, String sortBy, String direction, String accountId) {
        // 1. Khởi tạo Pageable với Sort
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        UUID uuidMajorId = UUID.fromString(majorId);

        // 2. Lấy thông tin Account và Role
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // Mặc định để null để Admin/VP có thể xem tất cả các trạng thái
        String finalStatus = null;

        // 3. Phân quyền: Student/Lecturer ép buộc chỉ xem PUBLISHED
        if (RoleName.STUDENT.toString().equals(roleName) || RoleName.LECTURER.toString().equals(roleName)) {
            finalStatus = PloStatus.PUBLISHED.toString();
        }

        Page<PO> poPage;
        if (finalStatus != null) {
            // Nhánh dành cho Student/Lecturer (Chỉ lấy Published)
            poPage = poRepository.findByMajor_MajorIdAndStatus(uuidMajorId, finalStatus, pageable);
        } else {
            // Nhánh dành cho ADMIN/VP (Lấy tất cả các trạng thái của Major đó)
            // Lưu ý: Chỉ cho phép vào đây nếu là VP hoặc ADMIN
            if (!RoleName.VP.toString().equals(roleName) && !RoleName.ADMIN.toString().equals(roleName)
                    && !RoleName.HOCFDC.toString().equals(roleName)) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
            poPage = poRepository.findByMajor_MajorId(uuidMajorId, pageable);
        }

        return poPage.map(poMapper::toPoResponse);
    }

    @Transactional
    public void deletePo(String id, String accountId) {
        // Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!RoleName.VP.toString().equals(roleName)) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        PO po = poRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));

        if (PloStatus.DRAFT.toString().equals(po.getStatus())) {
            poRepository.delete(po);
        } else {
            // Soft Delete (Archive)
            po.setStatus(PloStatus.ARCHIVED.toString());
            poRepository.save(po);
        }
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

    public ComplianceCheckResponse validatePoCheck(List<PO> poList, UUID majorId) {
        if (!majorRepository.existsById(majorId)) {
            throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
        }

        String userPoList = poList.stream()
                .map(po -> po.getPoCode() + ": " + po.getDescription())
                .collect(Collectors.joining("\n"));

        // 2. Lấy Master Rule từ Regulation
        Regulation regulation = regulationRepository.findByCodeAndMajor_MajorId("PO_PLO_RULE", majorId)
                .orElseThrow(() -> new AppException(ErrorCode.REGULATION_NOT_FOUND));
        return geminiService.checkPoPloCompliance(regulation.getValue(), userPoList);

    }
}
