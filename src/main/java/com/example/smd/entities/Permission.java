package com.example.smd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "permission")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "permission_id")
    UUID permissionId;

    @Column(name = "permission_name", unique = true, nullable = false, length = 50)
    String permissionName;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "created_at")
    Instant createdAt;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
