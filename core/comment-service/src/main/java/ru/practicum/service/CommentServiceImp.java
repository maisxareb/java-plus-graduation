package ru.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.CommentRequest;
import ru.practicum.comment.dto.CommentResponse;
import ru.practicum.event.client.EventClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Comment;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.repository.CommentRepository;
import ru.practicum.user.client.UserClient;
import ru.practicum.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImp implements CommentService {
    UserClient userClient;
    EventClient eventClient;
    CommentRepository commentRepository;
    String errorMessageNotFound = "Комментарий с id = %d не найден";
    String errorMessageNotAuthor = "Только автор c id = %d может редактировать комментарий";

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long eventId, CommentRequest commentRequest) {
        UserDto user = userClient.getUser(userId);
        EventFullDto event = eventClient.getEvent(eventId);

        Comment comment = new Comment();
        comment.setText(commentRequest.getText());
        comment.setCreated(LocalDateTime.now());
        comment.setAuthor(user.getId());
        comment.setEvent(event.getId());

        Comment newComment = commentRepository.save(comment);
        return CommentMapper.toCommentResponse(newComment);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest commentRequest) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, commentId)));
        UserDto user = userClient.getUser(userId);
        if (!comment.getAuthor().equals(userId)) {
            throw new ConflictException(String.format(errorMessageNotAuthor, comment.getAuthor()));
        }
        comment.setText(commentRequest.getText());
        Comment updatedComment = commentRepository.save(comment);
        return CommentMapper.toCommentResponse(updatedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByEvent(Long eventId) {
        EventFullDto eventFullDto = eventClient.getEvent(eventId);

        return commentRepository.findAllByEventOrderByCreatedAsc(eventId).stream()
                .map(CommentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse getCommentById(Long eventId, Long commentId) {
        EventFullDto eventFullDto = eventClient.getEvent(eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, commentId)));

        if (!(comment.getEvent().equals(eventId))) {
            throw new NotFoundException(String.format("Комментарий с id = %d не принадлежит указанному событию  с id = %d", commentId, eventId));
        }

        return CommentMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, commentId)));

        if (!(comment.getAuthor().equals(userId))) {
            throw new ConflictException(String.format(errorMessageNotAuthor, comment.getAuthor()));
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteCommentsForUser(Long userId) {
        List<Comment> commentsForDelete = commentRepository.findAllByAuthor(userId);
        commentsForDelete.forEach(comment -> commentRepository.deleteById(comment.getId()));
    }

    @Override
    public void deleteCommentsForEvent(Long eventId) {
        List<Comment> commentsForDelete = commentRepository.findAllByEventOrderByCreatedAsc(eventId);
        commentsForDelete.forEach(comment -> commentRepository.deleteById(comment.getId()));
    }
}
