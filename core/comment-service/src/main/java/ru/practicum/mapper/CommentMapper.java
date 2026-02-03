package ru.practicum.mapper;

import ru.practicum.comment.dto.CommentRequest;
import ru.practicum.comment.dto.CommentResponse;
import ru.practicum.model.Comment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommentMapper {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Comment toComment(CommentRequest commentRequest, Long author, Long event) {
        Comment comment = new Comment();
        comment.setText(commentRequest.getText());
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        comment.setEvent(event);
        return comment;
    }

    public static CommentResponse toCommentResponse(Comment comment) {
        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId(comment.getId());
        commentResponse.setText(comment.getText());
        commentResponse.setCreated(comment.getCreated().format(TIME_FORMAT));
        commentResponse.setAuthor(comment.getAuthor());
        commentResponse.setEvent(comment.getEvent());
        return commentResponse;
    }
}
