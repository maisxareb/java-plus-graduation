package ru.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.request.dto.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "requests", schema = "public")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @Column(name = "created", columnDefinition = "TIMESTAMP WITHOUT TIME ZONE", nullable = false)
    LocalDateTime created;

    @Column(name = "event", nullable = false)
    Long event;

    @Column(name = "requester", nullable = false)
    Long requester;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    RequestStatus status;
}
