package com.example.smd.mapper;

import com.example.smd.dto.request.SessionRequest;
import com.example.smd.dto.response.SessionResponse;
import com.example.smd.entities.Session;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(Session session) {
        if (session == null) {
            return null;
        }

        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .syllabusId(session.getSyllabus() != null ? session.getSyllabus().getSyllabusId() : null)
                .sessionNumber(session.getSessionNumber())
                .sessionTitle(session.getSessionTitle())
                .content(session.getContent())
                .teachingMethods(session.getTeachingMethods())
                .duration(session.getDuration())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }

    public Session toEntity(SessionRequest request) {
        if (request == null) {
            return null;
        }

        return Session.builder()
                .sessionNumber(request.getSessionNumber())
                .sessionTitle(request.getSessionTitle())
                .content(request.getContent())
                .teachingMethods(request.getTeachingMethods())
                .duration(request.getDuration())
                .build();
    }

    public void updateEntity(Session session, SessionRequest request) {
        session.setSessionNumber(request.getSessionNumber());
        session.setSessionTitle(request.getSessionTitle());
        session.setContent(request.getContent());
        session.setTeachingMethods(request.getTeachingMethods());
        session.setDuration(request.getDuration());
    }
}
