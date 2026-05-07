package com.example.smd.mapper;

import com.example.smd.dto.response.clo.CloSessionMappingResponse;
import com.example.smd.entities.CLO_Session;
import com.example.smd.entities.CLOs;
import com.example.smd.entities.Session;
import com.example.smd.entities.Syllabus;
import org.springframework.stereotype.Component;

@Component
public class CloSessionMappingMapper {

    public CloSessionMappingResponse toResponse(CLO_Session mapping) {
        if (mapping == null) {
            return null;
        }

        CLOs clo = mapping.getClo();
        Session session = mapping.getSession();
        Syllabus syllabus = session != null ? session.getSyllabus() : null;

        return CloSessionMappingResponse.builder()
                .id(mapping.getId() != null ? mapping.getId().toString() : null)
                .cloId(clo != null && clo.getCloId() != null ? clo.getCloId().toString() : null)
                .cloCode(clo != null ? clo.getCloCode() : null)
                .sessionId(session != null && session.getSessionId() != null
                        ? session.getSessionId().toString()
                        : null)
                .sessionNumber(session != null ? session.getSessionNumber() : null)
                .sessionTitle(session != null ? session.getSessionTitle() : null)
                .sessionStatus(session != null && session.getSyllabus() != null ? session.getSyllabus().getStatus() : null)
                .syllabusId(syllabus != null && syllabus.getSyllabusId() != null
                        ? syllabus.getSyllabusId().toString()
                        : null)
                .build();
    }
}
