package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Syllabus_Action_Logs {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    Syllabus syllabus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_by", nullable = false)
    Account actionBy;

    @Column(name = "action_type", nullable = false, length = 50)
    String action;

    @Column(name = "action_description", nullable = false, length = 50)
    String note;

    @Column(name = "action_timestamp")
    java.time.Instant createdAt;
}
