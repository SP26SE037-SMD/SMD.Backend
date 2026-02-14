package com.example.smd.entities;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

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
    String sourceId;

    @Column(name = "source_name", length = 100)
    String sourceName;

    @Column(length = 20)
    String type;

    @Column(length = 100)
    String author;

    @Column(length = 100)
    String publisher;

    @Column(name = "published_date")
    java.time.Instant publishedDate;

    @Column(length = 20)
    String isbn;
}
