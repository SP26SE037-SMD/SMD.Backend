package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "rubric_criterion")
public class RubricCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "criterion_id")
    UUID criterionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_id", nullable = false)
    Rubric rubric;

    @Column(name = "criteria_code", nullable = false, length = 50, unique = true)
    String code;

    @Column(name = "criterion_name", columnDefinition = "TEXT")
    String criterionName;

    @Column(name = "weight", precision = 5, scale = 2)
    BigDecimal weight;

    @Column(name = "display_order")
    Integer displayOrder;

    @OneToMany(mappedBy = "criterion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    List<CriteriaLevel> criteriaLevels;
}
