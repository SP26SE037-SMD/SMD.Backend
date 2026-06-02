package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "rubric_level")
public class RubricLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "level_id")
    UUID levelId;

    @Column(name = "level_code", nullable = false, length = 30)
    String levelCode;

    @Column(name = "min_score", precision = 4, scale = 1)
    BigDecimal minScore;

    @Column(name = "max_score", precision = 4, scale = 1)
    BigDecimal maxScore;

    @Column(name = "display_order")
    String displayOrder;
}
