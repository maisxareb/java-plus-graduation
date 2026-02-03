package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCategory(Long category);

    Page<Event> findByInitiator(Long initiator, PageRequest page);

    List<Event> findByInitiator(Long initiator);

    Optional<Event> findByIdAndInitiator(Long id, Long initiator);

    List<Event> findByIdIn(Set<Long> ids);

    @Query("SELECT e FROM Event e WHERE e.id IN :ids")
    List<Event> findEventsByIdSet(@Param("ids") Set<Long> ids);

    boolean existsByCategory(Long category);
}
