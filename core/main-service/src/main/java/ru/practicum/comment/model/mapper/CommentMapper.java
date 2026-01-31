package ru.practicum.comment.model.mapper;

import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.dto.CommentRequest;
import ru.practicum.comment.model.dto.CommentResponse;
import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, DateTimeFormatter.class})
public interface CommentMapper {

    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "commentRequest.text")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "created", expression = "java(LocalDateTime.now())")
    @Mapping(target = "event", source = "event")
    Comment toComment(CommentRequest commentRequest, User author, Event event);

    @Mapping(target = "created", source = "created", qualifiedByName = "formatDateTime")
    @Mapping(target = "author", source = "author.id")
    @Mapping(target = "event", source = "event.id")
    CommentResponse toCommentResponse(Comment comment);

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(TIME_FORMAT);
    }
}
