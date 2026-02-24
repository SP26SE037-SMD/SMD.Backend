package com.example.smd.mapper;

import com.example.smd.dto.request.MajorRequest;
import com.example.smd.dto.request.PLOsRequest;
import com.example.smd.dto.response.MajorResponse;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.entities.Major;
import com.example.smd.entities.PLOs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PLOsMapper {
    @Mapping(target = "major", ignore = true) // Chúng ta sẽ set Major thủ công trong Service bằng ID
    PLOs toPlo(PLOsRequest plOsRequest);

    @Mapping(source = "major.majorId", target = "majorId")
    @Mapping(source = "major.majorName", target = "majorName")
    PLOsResponse toPloResponse(PLOs plo);
}
