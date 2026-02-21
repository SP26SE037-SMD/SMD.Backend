package com.example.smd.mapper;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.entities.Major;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MajorMapper {
    Major toMajor(MajorRequest majorRequest);
    MajorResponse toMajorResponse(Major major);
}
