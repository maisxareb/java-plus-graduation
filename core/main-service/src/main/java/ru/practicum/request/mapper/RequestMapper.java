package ru.practicum.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.event.model.Event;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "id", source = "request.id")
    @Mapping(target = "created", expression = "java(formatDate(request.getCreated()))")
    @Mapping(target = "event", expression = "java(request.getEvent().getId())")
    @Mapping(target = "requester", expression = "java(request.getRequester().getId())")
    @Mapping(target = "status", source = "request.status")
    RequestDto toRequestDto(Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", expression = "java(parseDate(dto.getCreated()))")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "requester", source = "user")
    @Mapping(target = "status", source = "dto.status")
    Request toRequest(RequestDto dto, Event event, User user);

    default String formatDate(LocalDateTime date) {
        return date != null ? date.format(TIME_FORMAT) : null;
    }

    default LocalDateTime parseDate(String date) {
        return date != null ? LocalDateTime.parse(date, TIME_FORMAT) : null;
    }
}
