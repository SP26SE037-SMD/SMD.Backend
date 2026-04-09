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
public class Assessment_Templates {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID templateId;

    @Column(name = "template_name", length = 100)
    String templateName;

    @Column(columnDefinition = "TEXT")
    String description;
}
