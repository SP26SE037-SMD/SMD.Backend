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
@Table(name = "feedback_form_sections")
public class FeedbackFormSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "section_id")
    UUID sectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_record_id", nullable = false)
    GoogleFormRecord formRecord;

    @Column(name = "title", length = 200)
    String title;

    @Column(name = "order_index")
    Integer orderIndex;

    @Column(name = "after_section_action", length = 20)
    String afterSectionAction;

    @Column(name = "target_section_id")
    UUID targetSectionId;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    List<FeedbackFormQuestion> questions;
}
