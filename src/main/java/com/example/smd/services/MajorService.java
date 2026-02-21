package com.example.smd.services;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.entities.Major;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.mapper.MajorMapper;
import com.example.smd.repositories.MajorRepository;
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
public class MajorService {
    MajorRepository majorRepository;
    MajorMapper majorMapper;

    // GetAll có phân trang
    public Page<MajorResponse> getAllMajors(String search, String searchBy, int page, int size, String[] sort) {
        // 1. Xử lý sắp xếp (Sắp xếp theo field CamelCase của Java)
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        Page<Major> majorPage;

        // 2. Logic tìm kiếm để lấy Page<Entity>
        if (search == null || search.trim().isEmpty()) {
            majorPage = majorRepository.findAll(pageable);
        } else {
            switch (searchBy.toLowerCase()) {
                case "code":
                    majorPage = majorRepository.findByMajorCodeContainingIgnoreCase(search, pageable);
                    break;
                case "name":
                    majorPage = majorRepository.findByMajorNameContainingIgnoreCase(search, pageable);
                    break;
                default:
                    majorPage = majorRepository.findByMajorNameContainingIgnoreCaseOrMajorCodeContainingIgnoreCase(
                            search, search, pageable);
                    break;
            }
        }

        // 3. Map nguyên List Entity sang DTO bằng method reference của MapStruct
        return majorPage.map(majorMapper::toMajorResponse);
    }

    // Create Major
    public MajorResponse createMajor(MajorRequest request) {
        if (majorRepository.existsByMajorCode(request.getMajorCode())) {
            throw new AppException(ErrorCode.MAJOR_CODE_EXISTS);
        }

        Major major = majorMapper.toMajor(request);
        var response =  majorRepository.save(major);
        return majorMapper.toMajorResponse(response);
    }

    // Update Major
    public MajorResponse updateMajor(UUID id, MajorRequest request) {
        Major major = majorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        major.setMajorName(request.getMajorName());
        major.setDescription(request.getDescription());

        var response = majorRepository.save(major);
        return majorMapper.toMajorResponse(response);
    }

    // Delete Major (Xóa mềm)
    public void deleteMajor(UUID id) {
        if (!majorRepository.existsById(id)) {
            throw new AppException(ErrorCode.MAJOR_NOT_FOUND);
        }
        majorRepository.deleteById(id);
    }

    public MajorResponse getMajorDetail(String majorCode) {
        Major major = majorRepository.findByMajorCode(majorCode)
                .orElseThrow(() -> new AppException(ErrorCode.MAJOR_NOT_FOUND));

        return majorMapper.toMajorResponse(major);
    }
}
