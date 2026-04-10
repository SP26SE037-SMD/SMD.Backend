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
@Table(name = "google_form_records")
public class GoogleFormRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    Curriculum curriculum;

    @Column(name = "google_form_id", length = 200)
    String googleFormId;

    @Column(name = "form_url", columnDefinition = "TEXT")
    String formUrl;

    @Column(name = "edit_url", columnDefinition = "TEXT")
    String editUrl;

    @Column(name = "form_type", length = 100)
    String formType;

    @Column(name = "is_active")
    Boolean isActive;

    @Column(name = "created_at")
    Instant createdAt;

    @OneToMany(mappedBy = "formRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<FormQuestionMapping> questionMappings;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.isActive == null) {
            this.isActive = false;
        }
    }
}
