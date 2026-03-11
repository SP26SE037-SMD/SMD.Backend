package com.example.smd.services;

import com.example.smd.dto.request.PLOsRequest;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.entities.Curriculum;
import com.example.smd.entities.Major;
import com.example.smd.entities.PLOs;
import com.example.smd.enums.PloStatus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.PLOsMapper;
import com.example.smd.repositories.CurriculumRepository;
import com.example.smd.repositories.MajorRepository;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PLOsService {

    PLOsRepository plOsRepository;
    MajorRepository majorRepository;
    CurriculumRepository curriculumRepository;
    PLOsMapper plOsMapper;

    @Transactional
    public PLOsResponse createPlo(PLOsRequest request) {
        try {
            // 1. Check trùng mã PLO trong cùng 1 Major (Mã PLO có thể trùng ở ngành khác nhưng không được trùng trong cùng ngành)
            UUID curriculumId = UUID.fromString(request.getCurriculumId());
            if (plOsRepository.existsByPloCodeAndCurriculum_CurriculumId(request.getPloCode(), curriculumId)) {
                throw new AppException(ErrorCode.PLO_CODE_EXISTS);
            }

            // 2. Các bước tìm Curriculum và Major như cũ...
            Curriculum curriculum = curriculumRepository.findById(curriculumId)
                    .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

            PLOs plo = plOsMapper.toPlo(request);
            plo.setCurriculum(curriculum);
            plo.setStatus(PloStatus.PUBLISH.toString());

            return plOsMapper.toPloResponse(plOsRepository.save(plo));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc một mã lỗi định dạng ID không hợp lệ
        }
    }

    @Transactional
    public PLOsResponse updatePlo(String id, PLOsRequest request) {
        try {
            UUID plOsId = UUID.fromString(id);
            PLOs plo = plOsRepository.findById(plOsId)
                    .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND));

            // Nếu thay đổi code, cần check xem code mới có trùng trong Major hiện tại không
            if (!plo.getPloCode().equals(request.getPloCode()) &&
                    plOsRepository.existsByPloCodeAndMajor_MajorId(request.getPloCode(), plo.getMajor().getMajorId())) {
                throw new AppException(ErrorCode.PLO_CODE_EXISTS);
            }

            plo.setPloCode(request.getPloCode());
            plo.setPloName(request.getPloName());
            plo.setDescription(request.getDescription());

            return plOsMapper.toPloResponse(plOsRepository.save(plo));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc một mã lỗi định dạng ID không hợp lệ
        }
    }

    public PLOsResponse getPloDetail(String id) {
        try {
            UUID plOsId = UUID.fromString(id);
            return plOsRepository.findById(plOsId)
                    .map(plOsMapper::toPloResponse)
                    .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc một mã lỗi định dạng ID không hợp lệ
        }
    }

    public Page<PLOsResponse> getPlosByMajor(String majorId, int page, int size) {
        try {
            // Check majorId trước khi tìm kiếm
            UUID id = UUID.fromString(majorId);
            if (!majorRepository.existsById(id)) {
                throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("ploCode").ascending());
            return plOsRepository.findByMajor_MajorId(id, pageable)
                    .map(plOsMapper::toPloResponse);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Hoặc một mã lỗi định dạng ID không hợp lệ
        }
    }

    @Transactional
    public void deletePlo(String id) {
        try {
            UUID ploId = UUID.fromString(id);

            // 1. Kiểm tra PLO có tồn tại không
            PLOs plo = plOsRepository.findById(ploId)
                    .orElseThrow(() -> new AppException(ErrorCode.PLO_NOT_FOUND));

            // 2. Kiểm tra ràng buộc dữ liệu (Logic nghiệp vụ cho đồ án Capstone)
            // Nếu PLO đã được ánh xạ vào Course (môn học) thì không được xóa
//            if (plo.getCurriculum() != null) {
//                throw new AppException(ErrorCode.PLO_IN_USE);
//                // Bạn cần định nghĩa thêm ErrorCode này: "PLO đang được sử dụng, không thể xóa"
//            }

            // 3. Thực hiện xóa
            plo.setStatus(PloStatus.ARCHIVE.toString());
            plOsRepository.save(plo);

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
