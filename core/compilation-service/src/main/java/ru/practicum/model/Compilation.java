package ru.practicum.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "compilations", schema = "public")
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "title")
    String title;

    @Column(name = "pinned")
    boolean pinned;

    @ElementCollection
    @CollectionTable(
            name = "compilation_events",
            schema = "public",
            joinColumns = @JoinColumn(name = "compilation_id")
    )
    @Column(name = "event_id")
    Set<Long> events;
}
