package com.example.smd.mapper;

import com.example.smd.dto.request.CLOsRequest;
import com.example.smd.dto.request.PLOsRequest;
import com.example.smd.dto.response.CLOsResponse;
import com.example.smd.dto.response.PLOsResponse;
import com.example.smd.entities.CLOs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CLOsMapper {
    @Mapping(target = "subject", ignore = true) // Chúng ta sẽ set Major thủ công trong Service bằng ID
    CLOs toClo(CLOsRequest clOsRequest);

    @Mapping(source = "subject.subjectId", target = "subjectId")
    @Mapping(source = "subject.subjectName", target = "subjectName")
    CLOsResponse toCloResponse(CLOs clo);
}
