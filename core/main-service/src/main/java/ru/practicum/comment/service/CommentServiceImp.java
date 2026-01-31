package ru.practicum.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.dto.CommentRequest;
import ru.practicum.comment.model.dto.CommentResponse;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImp implements CommentService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long eventId, CommentRequest commentRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d не найдено", eventId)));

        Comment comment = commentMapper.toComment(commentRequest, user, event);

        Comment newComment = commentRepository.save(comment);
        return commentMapper.toCommentResponse(newComment);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest commentRequest) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с id = %d не найден", commentId)));

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new ConflictException("Только автор может редактировать комментарий");
        }

        comment.setText(commentRequest.getText());
        Comment updatedComment = commentRepository.save(comment);
        return commentMapper.toCommentResponse(updatedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByEvent(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d не найдено", eventId)));

        return commentRepository.findAllByEvent_IdOrderByCreatedAsc(eventId).stream()
                .map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse getCommentById(Long eventId, Long commentId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d не найдено", eventId)));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с id = %d не найден", commentId)));

        if (!Objects.equals(comment.getEvent().getId(), eventId)) {
            throw new NotFoundException(String.format("Комментарий с id = %d не принадлежит указанному событию с id = %d", commentId, eventId));
        }

        return commentMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с id = %d не найден", commentId)));

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new ConflictException("Только автор может удалять комментарий");
        }

        commentRepository.deleteById(commentId);
    }
}
