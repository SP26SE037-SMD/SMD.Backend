package com.example.smd.entities;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "account")
public class   Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String userID;

    @Column(unique = true, nullable = false)
    String username;

    String passwordHash;

    LocalDate createDate;
    LocalDate updateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleName")
    Role role;

    @PrePersist
    protected void onCreate() {
        this.createDate = LocalDate.now();
        this.updateDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateDate = LocalDate.now();
    }
}
