package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.request_service.enums.RequestStatus;

import java.time.Instant;

@Entity
@Table(name = "requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column
    @Builder.Default
    private Instant created = Instant.now();

    @Enumerated(EnumType.STRING)
    private RequestStatus status;
}