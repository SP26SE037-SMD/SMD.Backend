package com.example.smd.services;

import com.example.smd.entities.Regulation;
import com.example.smd.entities.Session;
import com.example.smd.entities.Syllabus;
import com.example.smd.exception.AppException;
import com.example.smd.exception.ErrorCode;
import com.example.smd.repositories.RegulationRepository;
import com.example.smd.repositories.SessionRepository;
import com.example.smd.repositories.SyllabusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRegulationValidationService {

//    private static final String RL1_CODE = "RL1";
//    private static final String RL2_CODE = "RL2";
//
//    private final RegulationRepository regulationRepository;
//    private final SyllabusRepository syllabusRepository;
//    private final SessionRepository sessionRepository;
//
//    @Transactional(readOnly = true)
//    public void validateCanCreateMoreSessions(UUID syllabusId) {
//        Syllabus syllabus = syllabusRepository.findById(syllabusId)
//                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
//
//        if (syllabus.getSubject() == null || syllabus.getSubject().getCredits() == null) {
//            throw new AppException(ErrorCode.INVALID_KEY, "Subject credits are required to validate max sessions");
//        }
//
//        Regulation rl1 = regulationRepository.findByCode(RL1_CODE)
//                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation RL1 is not configured"));
//        Regulation rl2 = regulationRepository.findByCode(RL2_CODE)
//                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation RL2 is not configured"));
//
//        if (rl1.getValue() == null || rl1.getValue() <= 0 || rl2.getValue() == null || rl2.getValue() <= 0) {
//            throw new AppException(ErrorCode.INVALID_KEY, "Regulation RL1/RL2 values must be greater than 0");
//        }
//
//        long totalAllowedMinutes = (long) syllabus.getSubject().getCredits() * rl2.getValue() * 60L;
//        long maxSessions = totalAllowedMinutes / rl1.getValue();
//        long currentSessions = sessionRepository.findBySyllabus_SyllabusIdOrderBySessionNumberAsc(syllabusId).size();
//
//        if (currentSessions >= maxSessions) {
//            throw new AppException(ErrorCode.INVALID_KEY,
//                    "Maximum number of sessions reached by regulation. Max sessions=" + maxSessions);
//        }
//    }
//
//    @Transactional(readOnly = true)
//    public void validateDurationByRegulation(UUID syllabusId, Integer newDurationMinutes, UUID excludeSessionId) {
//        Syllabus syllabus = syllabusRepository.findById(syllabusId)
//                .orElseThrow(() -> new AppException(ErrorCode.SYLLABUS_NOT_FOUND));
//
//        if (syllabus.getSubject() == null || syllabus.getSubject().getCredits() == null) {
//            throw new AppException(ErrorCode.INVALID_KEY, "Subject credits are required to validate session duration");
//        }
//
//        Regulation rl1 = regulationRepository.findByCode(RL1_CODE)
//                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation RL1 is not configured"));
//        Regulation rl2 = regulationRepository.findByCode(RL2_CODE)
//                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY, "Regulation RL2 is not configured"));
//
//        if (rl1.getValue() == null || rl1.getValue() <= 0) {
//            throw new AppException(ErrorCode.INVALID_KEY, "Regulation RL1 value must be greater than 0 (minutes per session)");
//        }
//        if (rl2.getValue() == null || rl2.getValue() <= 0) {
//            throw new AppException(ErrorCode.INVALID_KEY, "Regulation RL2 value must be greater than 0 (hours per credit)");
//        }
//
//        int duration = newDurationMinutes == null ? 0 : newDurationMinutes;
//        if (duration > rl1.getValue()) {
//            throw new AppException(ErrorCode.INVALID_KEY,
//                    "Session duration exceeds RL1 limit: max " + rl1.getValue() + " minutes per session");
//        }
//
//        long totalAllowedMinutes = (long) syllabus.getSubject().getCredits() * rl2.getValue() * 60L;
//        long maxSessions = totalAllowedMinutes / rl1.getValue();
//
//        List<Session> existingSessions = sessionRepository.findBySyllabus_SyllabusIdOrderBySessionNumberAsc(syllabusId);
//
//        // For create flow, also enforce max number of sessions.
//        if (excludeSessionId == null && existingSessions.size() >= maxSessions) {
//            throw new AppException(ErrorCode.INVALID_KEY,
//                    "Maximum number of sessions reached by regulation. Max sessions=" + maxSessions);
//        }
//
//        long usedMinutes = existingSessions.stream()
//                .filter(session -> excludeSessionId == null || !session.getSessionId().equals(excludeSessionId))
//                .map(Session::getDuration)
//                .filter(value -> value != null && value > 0)
//                .mapToLong(Integer::longValue)
//                .sum();
//
//        if (usedMinutes + duration > totalAllowedMinutes) {
//            throw new AppException(ErrorCode.INVALID_KEY,
//                    "Total session duration exceeds allowed limit by RL2. Max=" + totalAllowedMinutes +
//                            " minutes, current=" + usedMinutes + " minutes, new=" + duration + " minutes");
//        }
//    }
}
