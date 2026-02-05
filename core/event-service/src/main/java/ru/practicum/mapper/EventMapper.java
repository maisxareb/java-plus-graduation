package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.model.Event;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface EventMapper {

    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "publishedOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "eventDate", source = "newEventDto.eventDate", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "category", source = "categoryId")
    @Mapping(target = "initiator", source = "userId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    Event toEvent(NewEventDto newEventDto, Long categoryId, Long userId);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "eventDate", source = "event.eventDate", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "userShortDto")
    EventShortDto toShortDto(Event event, long views, CategoryDto category, UserShortDto userShortDto);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "eventDate", source = "event.eventDate", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "createdOn", source = "event.createdOn", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "publishedOn", source = "event.publishedOn", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "views", source = "views")
    @Mapping(target = "category", source = "categoryDto")
    @Mapping(target = "initiator", source = "userShortDto")
    EventFullDto toFullDto(Event event, long views, CategoryDto categoryDto, UserShortDto userShortDto);

    @Named("localDateTimeToString")
    default String localDateTimeToString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMAT) : null;
    }

    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateTimeStr) {
        return dateTimeStr != null ? LocalDateTime.parse(dateTimeStr, TIME_FORMAT) : null;
    }

    default Event toEventStatic(NewEventDto newEventDto, Long category, Long user) {
        return toEvent(newEventDto, category, user);
    }

    default EventShortDto toShortDtoStatic(Event event, long views, CategoryDto category, UserShortDto userShortDto) {
        return toShortDto(event, views, category, userShortDto);
    }

    default EventFullDto toFullDtoStatic(Event event, long views, CategoryDto categoryDto, UserShortDto userShortDto) {
        return toFullDto(event, views, categoryDto, userShortDto);
    }
}
