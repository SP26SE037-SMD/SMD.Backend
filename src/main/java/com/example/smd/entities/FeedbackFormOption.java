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
@Table(name = "feedback_form_options")
public class FeedbackFormOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "option_id")
    UUID optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    FeedbackFormQuestion question;

    @Column(name = "option_text", columnDefinition = "TEXT")
    String optionText;

    @Column(name = "order_index")
    Integer orderIndex;

    @Column(name = "next_section_id")
    UUID nextSectionId;
}
