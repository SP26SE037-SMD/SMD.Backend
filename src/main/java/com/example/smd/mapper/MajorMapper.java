package com.example.smd.mapper;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.entities.Major;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MajorMapper {
    // Chuyển đổi từ DTO MajorRequest sang Entity Major
    Major toMajor(MajorRequest majorRequest);

    // Chuyển đổi từ Entity Major sang DTO MajorResponse
    MajorResponse toMajorResponse(Major major);
}
