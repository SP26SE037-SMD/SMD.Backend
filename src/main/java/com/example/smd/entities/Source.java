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
@Table(name = "source")
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID sourceId;

    @Column(name = "source_name", columnDefinition = "TEXT")
    String sourceName;

    @Column(name = "source_type")
    String type;

    @Column(length = 100)
    String author;

    @Column(length = 100)
    String publisher;

    @Column(name = "publication_year") // Đổi tên cột cho rõ nghĩa
    int publishedYear;

    @Column(length = 20)
    String isbn;

    String url;
}
