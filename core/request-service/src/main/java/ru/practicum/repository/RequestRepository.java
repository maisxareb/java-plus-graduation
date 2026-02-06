package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    @Query("SELECT r FROM Request r WHERE r.requester = :requesterId")
    List<Request> findByRequester(Long requesterId);

    @Modifying
    @Query("UPDATE Request r SET r.status = 'REJECTED' WHERE r.id = :requestId")
    void updateToRejected(Long requestId);

    @Modifying
    @Query("UPDATE Request r SET r.status = 'CANCELED' WHERE r.id = :requestId")
    void updateToCanceled(Long requestId);

    @Query("SELECT r FROM Request r WHERE r.event = :eventId")
    List<Request> findAllByEventId(Long eventId);

    @Query("SELECT r FROM Request r " +
            "WHERE r.event = :eventId AND r.requester = :requesterId")
    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long requesterId);
}
