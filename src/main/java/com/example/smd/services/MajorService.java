package com.example.smd.services;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.PLOs;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.repositories.MajorRepository;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MajorService {
    AccountService accountService;
    MajorRepository majorRepository;
    MajorMapper majorMapper;

    // GetAll có phân trang
    public Page<MajorResponse> getAllMajors(String accountId,String search, String searchBy, String status, int page, int size, String[] sort) {
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        Page<Major> majorPage;

        //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if(account.getRole().getRoleName().equals("STUDENT") ||  account.getRole().getRoleName().equals("LECTURER")) {
            status = "PUBLISHED";
        }

        // Kiểm tra nếu có filter theo status
        boolean hasStatus = status != null && !status.trim().isEmpty();
        boolean hasSearch = search != null && !search.trim().isEmpty();

        if (!hasSearch) {
            majorPage = hasStatus ? majorRepository.findByStatus(status, pageable)
                    : majorRepository.findAll(pageable);
        } else {
            String searchLower = search.trim();
            if (hasStatus) {
                // Logic Search + Status
                majorPage = switch (searchBy.toLowerCase()) {
                    case "code" -> majorRepository.findByMajorCodeContainingIgnoreCaseAndStatus(searchLower, status, pageable);
                    case "name" -> majorRepository.findByMajorNameContainingIgnoreCaseAndStatus(searchLower, status, pageable);
                    default -> majorRepository.searchAllFieldsWithStatus(searchLower, status, pageable);
                };
            } else {
                // Logic Search cũ (không có status)
                majorPage = switch (searchBy.toLowerCase()) {
                    case "code" -> majorRepository.findByMajorCodeContainingIgnoreCase(searchLower, pageable);
                    case "name" -> majorRepository.findByMajorNameContainingIgnoreCase(searchLower, pageable);
                    default -> majorRepository.findByMajorNameContainingIgnoreCaseOrMajorCodeContainingIgnoreCase(searchLower, searchLower, pageable);
                };
            }
        }

        return majorPage.map(majorMapper::toMajorResponse);
    }

    // Create Major
    public MajorResponse createMajor(MajorRequest request) {
        if (majorRepository.existsByMajorCode(request.getMajorCode())) {
            throw new AppException(ErrorCode.MAJOR_CODE_EXISTS);
        }

        Major major = majorMapper.toMajor(request);
        major.setStatus(PloStatus.DRAFT.toString());
        var response =  majorRepository.save(major);
        return majorMapper.toMajorResponse(response);
    }

    // Update Major
    public MajorResponse updateMajor(UUID id, MajorRequest request) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        if(!major.getStatus().equals(PloStatus.DRAFT.toString())) {
            throw new AppException(ErrorCode.MAJOR_NOT_DRAFT);
        }

        major.setMajorName(request.getMajorName());
        major.setDescription(request.getDescription());
        major.setUpdatedAt(Instant.now());

        var response = majorRepository.save(major);
        return majorMapper.toMajorResponse(response);
    }

    // Delete Major (Xóa mềm)
    public void deleteMajor(UUID id) {
        try {
            Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

            if(major.getStatus().equals("DRAFT")) {
                majorRepository.delete(major);
            } else{
                major.setStatus(PloStatus.ARCHIVED.toString());
                majorRepository.save(major);
            }
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public MajorResponse getMajorDetail(String majorCode) {
        Major major = majorRepository.findByMajorCode(majorCode)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        return majorMapper.toMajorResponse(major);
    }

    public MajorResponse getMajorById(UUID id, String accountId) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        //Phân quyền ROLE Student + Lecture chỉ xem được PUBLISHED
        var account = accountService.getAccountById(accountId);
        if(account.getRole().getRoleName().equals("STUDENT") ||  account.getRole().getRoleName().equals("LECTURER")) {
            if(!major.getStatus().equals(PloStatus.PUBLISHED.toString())) {
                new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        if(major.getStatus().equals(PloStatus.DRAFT.toString())) {
            if(!account.getRole().getRoleName().equals("VP")){
                new AppException(ErrorCode.ACCESS_DENIED_FOR_ROLE);
            }
        }

        return majorMapper.toMajorResponse(major);
    }

    @Transactional
    public MajorResponse updateStatus(String id, String newStatus) {
        // 1. Kiểm tra trạng thái có hợp lệ không
        PloStatus status;
        try {
            // valueOf so sánh chuỗi với tên của các hằng số trong Enum (VD: "DRAFT")
            status = PloStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Ném ra lỗi của hệ thống nếu trạng thái không tồn tại
            throw new AppException(ErrorCode.INVALID_MAJOR_STATUS);
        }

        // 2. Tìm CLO theo ID
        Major major = majorRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        // 3. Cập nhật trạng thái
        major.setStatus(status.toString());
        major.setUpdatedAt(Instant.now());
        return majorMapper.toMajorResponse(majorRepository.save(major));
    }

    public Page<MajorResponse> getMajorsUpdatedInLast24Hours(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());

        // Mốc bắt đầu: 24 tiếng trước
        Instant startTime = Instant.now().minus(24, ChronoUnit.HOURS);
        // Mốc kết thúc: Bây giờ
        Instant endTime = Instant.now();

        // Truyền đủ 2 tham số vào hàm Repo đã viết
        return majorRepository.findByStatusAndUpdatedBetween(status, startTime, endTime, pageable)
                .map(majorMapper::toMajorResponse);
    }
}
