package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "\"options\"")
public class Options {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "option_id")
    UUID id;

    @Column(name = "order_index")
    Integer optionNo;

    @Column(name = "option_label", columnDefinition = "TEXT")
    String optionText;

    @OneToMany(mappedBy = "selectedOption", fetch = FetchType.LAZY)
    List<FeedbackAnswers> feedbackAnswers;

}
