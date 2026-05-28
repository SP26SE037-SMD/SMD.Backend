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
@Table(name = "system_setting")
public class System_Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    String code;

    String value;

    String description;
}
