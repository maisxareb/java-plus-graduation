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
    CommentMapper commentMapper;
    String errorMessageNotFound = "Комментарий с id = %d не найден";
    String errorMessageNotAuthor = "Только автор c id = %d может редактировать комментарий";

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long eventId, CommentRequest commentRequest) {
        UserDto user = userClient.getUser(userId);
        EventFullDto event = eventClient.getEvent(eventId);

        Comment comment = commentMapper.toComment(commentRequest, user.getId(), event.getId());

        Comment newComment = commentRepository.save(comment);
        return commentMapper.toCommentResponse(newComment);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest commentRequest) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, commentId)));

        userClient.getUser(userId);

        if (!comment.getAuthor().equals(userId)) {
            throw new ConflictException(String.format(errorMessageNotAuthor, comment.getAuthor()));
        }

        comment.setText(commentRequest.getText());
        Comment updatedComment = commentRepository.save(comment);
        return commentMapper.toCommentResponse(updatedComment);
    }

    @Override
    public List<CommentResponse> getCommentsByEvent(Long eventId) {
        eventClient.getEvent(eventId);

        return commentRepository.findAllByEventOrderByCreatedAsc(eventId).stream()
                .map(commentMapper::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommentResponse getCommentById(Long eventId, Long commentId) {
        eventClient.getEvent(eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, commentId)));

        if (!comment.getEvent().equals(eventId)) {
            throw new NotFoundException(String.format(
                    "Комментарий с id = %d не принадлежит указанному событию с id = %d",
                    commentId, eventId
            ));
        }

        return commentMapper.toCommentResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, commentId)));

        if (!comment.getAuthor().equals(userId)) {
            throw new ConflictException(String.format(errorMessageNotAuthor, comment.getAuthor()));
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    @Transactional
    public void deleteCommentsForUser(Long userId) {
        List<Long> commentIds = commentRepository.findIdsByAuthor(userId);

        if (!commentIds.isEmpty()) {
            List<Comment> comments = commentRepository.findAllById(commentIds);
            commentRepository.deleteAll(comments);
        }
    }

    @Override
    @Transactional
    public void deleteCommentsForEvent(Long eventId) {
        List<Long> commentIds = commentRepository.findIdsByEvent(eventId);

        if (!commentIds.isEmpty()) {
            List<Comment> comments = commentRepository.findAllById(commentIds);
            commentRepository.deleteAll(comments);
        }
    }
}
