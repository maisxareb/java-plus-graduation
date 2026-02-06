package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.comment.dto.CommentRequest;
import ru.practicum.comment.dto.CommentResponse;
import ru.practicum.model.Comment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(TIME_PATTERN);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "commentRequest.text")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "created", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "event", source = "event")
    Comment toComment(CommentRequest commentRequest, Long author, Long event);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "text", source = "comment.text")
    @Mapping(target = "created", source = "comment.created", qualifiedByName = "formatLocalDateTime")
    @Mapping(target = "author", source = "comment.author")
    @Mapping(target = "event", source = "comment.event")
    CommentResponse toCommentResponse(Comment comment);

    @Named("formatLocalDateTime")
    static String formatLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(TIME_FORMAT);
    }

    static Comment toCommentStatic(CommentRequest commentRequest, Long author, Long event) {
        if (commentRequest == null) {
            return null;
        }
        Comment comment = new Comment();
        comment.setText(commentRequest.getText());
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now());
        comment.setEvent(event);
        return comment;
    }

    static CommentResponse toCommentResponseStatic(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId(comment.getId());
        commentResponse.setText(comment.getText());
        commentResponse.setCreated(comment.getCreated().format(TIME_FORMAT));
        commentResponse.setAuthor(comment.getAuthor());
        commentResponse.setEvent(comment.getEvent());
        return commentResponse;
    }
}