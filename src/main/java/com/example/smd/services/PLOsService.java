package com.example.smd.services;

import com.example.smd.dto.request.plo.PLOsCreateRequest;
import com.example.smd.dto.request.plo.PLOsRequest;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.entities.Curriculum;
import com.example.smd.entities.PLOs;
import com.example.smd.entities.PO;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PLOsMapper;
import com.example.smd.repositories.CurriculumRepository;
import com.example.smd.repositories.PLOsRepository;
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
public class PLOsService {
    AccountService accountService;
    PLOsRepository plOsRepository;
    CurriculumRepository curriculumRepository;
    PLOsMapper plOsMapper;

    @Transactional
    public List<PLOsResponse> createBulkPlos(String curriculumId, List<PLOsCreateRequest> requests, String accountId) {
        if (requests == null || requests.isEmpty()) return Collections.emptyList();

        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!roleName.equals("HOCFDC")) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        // 1. Kiểm tra Curriculum tồn tại
        UUID uuidCurriculumId = UUID.fromString(curriculumId);
        Curriculum curriculum = curriculumRepository.findById(uuidCurriculumId)
                .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));


        // 2. Check trùng mã nội bộ trong JSON gửi lên (Local Check)
        Set<String> uniqueCodes = new HashSet<>();
        for (PLOsCreateRequest req : requests) {
            if (!uniqueCodes.add(req.getPloCode())) {
                throw new AppException(ErrorCode.PLO_CODE_EXISTS);
            }
        }

        // 3. Check trùng mã với Database (Global Check cho riêng Curriculum này)
        List<String> ploCodes = requests.stream().map(PLOsCreateRequest::getPloCode).toList();
        if (plOsRepository.existsByPloCodeInAndCurriculum_CurriculumId(ploCodes, uuidCurriculumId)) {
            throw new AppException(ErrorCode.PLO_CODE_EXISTS);
        }

        // 4. Map và Save hàng loạt
        List<PLOs> plosToSave = requests.stream().map(request -> {
            PLOs plo = plOsMapper.toPloCreate(request); // Sử dụng Mapper cho PLOsCreateRequest
            plo.setCurriculum(curriculum);
            plo.setStatus(PloStatus.DRAFT.toString());
            return plo;
        }).toList();

        return plOsRepository.saveAll(plosToSave).stream()
                .map(plOsMapper::toPloResponse)
                .toList();
    }

    @Transactional
    public PLOsResponse updatePlo(String id, PLOsRequest request, String accountId) {
        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!roleName.equals("HOCFDC")) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        UUID plOsId = UUID.fromString(id);
        PLOs plo = plOsRepository.findById(plOsId)
                .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND));

        // Nếu thay đổi code, cần check xem code mới có trùng trong Major hiện tại không
        if (!plo.getPloCode().equals(request.getPloCode()) &&
                plOsRepository.existsByPloCodeAndCurriculum_CurriculumId(request.getPloCode(), plo.getCurriculum().getCurriculumId())) {
            throw new AppException(ErrorCode.PLO_CODE_EXISTS);
        }

        if (!plo.getStatus().equals(PloStatus.DRAFT.toString())) {
            throw new AppException(ErrorCode.PLO_NOT_DRAFT);
        }

        plo.setPloCode(request.getPloCode());
        plo.setDescription(request.getDescription());

        return plOsMapper.toPloResponse(plOsRepository.save(plo));
    }

    public PLOsResponse getPloDetail(String id, String accountId) {
        try {
            UUID plOsId = UUID.fromString(id);
            PLOs plo = plOsRepository.findById(plOsId)
                    .orElseThrow(() -> new AppException(ErrorCode.PO_NOT_FOUND));

            //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
            var account = accountService.getAccountById(accountId);
            if (account.getRole().getRoleName().equals("STUDENT") || account.getRole().getRoleName().equals("LECTURER")) {
                if (!plo.getStatus().equals(PloStatus.PUBLISHED.toString())) {
                    throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
                }
            }

            if (plo.getStatus().equals(PloStatus.DRAFT.toString())) {
                if (!account.getRole().getRoleName().equals("HOCFDC")) {
                    throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
                }
            }

            return plOsRepository.findById(plOsId)
                    .map(plOsMapper::toPloResponse)
                    .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc một mã lỗi định dạng ID không hợp lệ
        }
    }

    public Page<PLOsResponse> getPlosByCurriculum(String curriculumId, int page, int size, String accountId) {

        UUID id = UUID.fromString(curriculumId);

        // 1. Kiểm tra Curriculum tồn tại
        if (!curriculumRepository.existsById(id)) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }

        // 2. Lấy thông tin Account và xác định Filter Status
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();

        // Mặc định null để Admin/VP xem được tất cả (Draft, Published,...)
        String finalStatus = null;

        if (roleName.equals("STUDENT") || roleName.equals("LECTURER")) {
            finalStatus = PloStatus.PUBLISHED.toString();
        }

        // 3. Khởi tạo Pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by("ploCode").ascending());

        Page<PLOs> ploPage;
        if (finalStatus != null) {
            // Nhánh lọc theo PUBLISHED cho Student/Lecturer
            ploPage = plOsRepository.findByCurriculum_CurriculumIdAndStatus(id, finalStatus, pageable);
        } else {
            // Nhánh lấy tất cả cho VP/Admin (phải check role để bảo mật)
            if (!roleName.equals("VP") && !roleName.equals("ADMIN") && !roleName.equals("HOCFDC") && !roleName.equals("HOPDC")) {
                throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
            ploPage = plOsRepository.findByCurriculum_CurriculumId(id, pageable);
        }

        return ploPage.map(plOsMapper::toPloResponse);
    }

    @Transactional
    public void deletePlo(String id, String accountId) {
        //Kiểm tra Role tạo
        var account = accountService.getAccountById(accountId);
        String roleName = account.getRole().getRoleName();
        if (!roleName.equals("VP")) {
            throw new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
        }

        UUID ploId = UUID.fromString(id);

        // 1. Kiểm tra PLO có tồn tại không
        PLOs plo = plOsRepository.findById(ploId)
                .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND));

        // 2. Thực hiện xóa
        if (plo.getStatus().equals(PloStatus.DRAFT.toString())) {
            plOsRepository.delete(plo);
        } else {
            plo.setStatus(PloStatus.ARCHIVED.toString());
            plOsRepository.save(plo);
        }

    }

    @Transactional
    public void updateStatusByCurriculum(String curriculumId, String newStatus) {
        // 1. Validate trạng thái Enum
        PloStatus status;
        try {
            status = PloStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AppException(ErrorCode.INVALID_PLO_STATUS);
        }

        UUID uuidCurriculumId = UUID.fromString(curriculumId);

        // 2. Kiểm tra sự tồn tại của Curriculum (Tái sử dụng logic kiểm tra của bạn)
        if (!curriculumRepository.existsById(uuidCurriculumId)) {
            throw new AppException(ErrorCode.CURRICULUM_NOT_FOUND);
        }

        // 3. Thực thi UPDATE hàng loạt (Chỉ tốn 1 câu lệnh SQL duy nhất)
        int affectedRows = plOsRepository.updateStatusByCurriculumId(status.toString(), uuidCurriculumId);

        if (affectedRows == 0) {
            throw new AppException(ErrorCode.PLO_NOT_FOUND);
        }
    }
}
