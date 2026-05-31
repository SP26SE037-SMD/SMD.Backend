package com.example.smd.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionDiffResponse {
    private List<String> addedSessions = new ArrayList<>();
    private List<String> removedSessions = new ArrayList<>();
    private List<SessionChangeDTO> changedSessions = new ArrayList<>();

    // Getters and Setters
    @Data
    @NoArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SessionChangeDTO {
        private String identifier;
        private List<String> changes;

        public SessionChangeDTO(String identifier, List<String> changes) {
            this.identifier = identifier;
            this.changes = changes;
        }
    }

    public List<String> getAddedSessions() { return addedSessions; }
    public List<String> getRemovedSessions() { return removedSessions; }
    public List<SessionChangeDTO> getChangedSessions() { return changedSessions; }
}
