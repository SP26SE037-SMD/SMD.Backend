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
    PLOs toPlo(PLOsRequest plOsRequest);

    PLOsResponse toPloResponse(PLOs plo);
}
