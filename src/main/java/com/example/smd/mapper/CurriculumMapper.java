package com.example.smd.mapper;

import com.example.smd.dto.request.curriculum.CurriculumCreateRequest;
import com.example.smd.dto.request.curriculum.CurriculumRequest;
import com.example.smd.dto.response.CurriculumResponse;
import com.example.smd.entities.Curriculum;
import com.example.smd.entities.Major;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CurriculumMapper {
    
    // Chuyển đổi từ Request sang Entity (không map major, sẽ set trong service)
    @Mapping(target = "curriculumId", ignore = true)
    @Mapping(target = "curriculumComboSubjects", ignore = true)
    @Mapping(target = "major", ignore = true)
    Curriculum toCurriculum(CurriculumRequest request);

    @Mapping(target = "curriculumId", ignore = true)
    @Mapping(target = "curriculumComboSubjects", ignore = true)
    @Mapping(target = "major", ignore = true)
    Curriculum toCreateCurriculum( CurriculumCreateRequest request);
    
    // Chuyển đổi từ Entity sang Response
    @Mapping(source = "major", target = "major", qualifiedByName = "mapMajorInfo")
    CurriculumResponse toCurriculumResponse(Curriculum curriculum);

    // Custom mapping cho Major -> MajorInfo
    @Named("mapMajorInfo")
    default CurriculumResponse.MajorInfo mapMajorInfo(Major major) {
        if (major == null) {
            return null;
        }
        return CurriculumResponse.MajorInfo.builder()
                .majorId(major.getMajorId().toString())
                .majorCode(major.getMajorCode())
                .majorName(major.getMajorName())
                .build();
    }

}
