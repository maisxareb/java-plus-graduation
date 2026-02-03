package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    @Query("SELECT r FROM Request r WHERE r.requester = :requesterId")
    List<Request> findByRequester(@Param("requesterId") Long requesterId);


    @Modifying
    @Transactional
    @Query("UPDATE Request r SET r.status = 'REJECTED' WHERE r.id = :requestId")
    void updateToRejected(@Param("requestId") Long requestId);


    @Modifying
    @Transactional
    @Query("UPDATE Request r SET r.status = 'CANCELED' WHERE r.id = :requestId")
    void updateToCanceled(@Param("requestId") Long requestId);


    @Query("SELECT r FROM Request r WHERE r.event = :eventId")
    List<Request> findAllByEventId(@Param("eventId") Long eventId);


    @Query("SELECT r FROM Request r " +
            "WHERE r.event = :eventId AND r.requester = :requesterId")
    Optional<Request> findByEventIdAndRequesterId(
            @Param("eventId") Long eventId,
            @Param("requesterId") Long requesterId);
}
