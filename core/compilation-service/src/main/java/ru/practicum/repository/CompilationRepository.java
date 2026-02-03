package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Compilation;

import java.util.List;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    List<Compilation> findByPinned(boolean pinned, Pageable page);

    @Query(value = "SELECT c.* FROM compilations c " +
            "JOIN compilation_events ce ON c.id = ce.compilation_id " +
            "WHERE ce.event_id = :eventId",
            nativeQuery = true)
    List<Compilation> findCompilationsByEventId(@Param("eventId") Long eventId);
}
