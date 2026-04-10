package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "form_question_mapping")
public class FormQuestionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_record_id", nullable = false)
    GoogleFormRecord formRecord;

    @Column(name = "question_id", nullable = false)
    UUID questionId;

    @Column(name = "google_item_id", nullable = false)
    String googleItemId;

    @Column(name = "backend_section_id")
    String backendSectionId;
}
