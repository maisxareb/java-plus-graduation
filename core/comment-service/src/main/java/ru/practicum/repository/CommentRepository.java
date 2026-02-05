package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByEventOrderByCreatedAsc(Long event);

    List<Comment> findAllByAuthor(Long author);

    @Query("SELECT c.id FROM Comment c WHERE c.author = :author")
    List<Long> findIdsByAuthor(@Param("author") Long author);

    @Query("SELECT c.id FROM Comment c WHERE c.event = :event")
    List<Long> findIdsByEvent(@Param("event") Long event);
}
