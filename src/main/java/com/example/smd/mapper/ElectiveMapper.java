package com.example.smd.mapper;

import com.example.smd.dto.request.ElectiveRequest;
import com.example.smd.dto.response.ElectiveResponse;
import com.example.smd.entities.Elective;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ElectiveMapper {
    Elective toElective(ElectiveRequest request);
    ElectiveResponse toElectiveResponse(Elective elective);

    @Mapping(target = "electiveCode", ignore = true)
    void updateElective(@MappingTarget Elective elective, ElectiveRequest request);
}