package ru.practicum.event.model.mapper;

import org.mapstruct.*;
import ru.practicum.category.model.Category;
import ru.practicum.category.model.mapper.CategoryMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.dto.EventFullDto;
import ru.practicum.event.model.dto.EventShortDto;
import ru.practicum.event.model.dto.NewEventDto;
import ru.practicum.user.model.User;
import ru.practicum.user.model.mapper.UserMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class, UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {
    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "eventDate", expression = "java(parseEventDate(newEventDto.getEventDate()))")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "user")
    @Mapping(target = "confirmedRequests", constant = "0")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "location", ignore = true)
    Event toEvent(NewEventDto newEventDto, Category category, User user);

    @Mapping(target = "id", source = "eventFullDto.id")
    @Mapping(target = "createdOn", expression = "java(parseEventDate(eventFullDto.getCreatedOn()))")
    @Mapping(target = "publishedOn", expression = "java(parseEventDate(eventFullDto.getPublishedOn()))")
    @Mapping(target = "eventDate", expression = "java(parseEventDate(eventFullDto.getEventDate()))")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", source = "user")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "location", ignore = true)
    Event toEvent(EventFullDto eventFullDto, User user);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "eventDate", expression = "java(formatEventDate(event.getEventDate()))")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    EventShortDto toShortDto(Event event, long views);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "eventDate", expression = "java(formatEventDate(event.getEventDate()))")
    @Mapping(target = "createdOn", expression = "java(formatEventDate(event.getCreatedOn()))")
    @Mapping(target = "publishedOn", expression = "java(event.getPublishedOn() != null ? formatEventDate(event.getPublishedOn()) : null)")
    @Mapping(target = "category", source = "event.category")
    @Mapping(target = "initiator", source = "event.initiator")
    EventFullDto toFullDto(Event event, long views);

    default String formatEventDate(LocalDateTime date) {
        return date != null ? date.format(TIME_FORMAT) : null;
    }

    default LocalDateTime parseEventDate(String date) {
        return date != null ? LocalDateTime.parse(date, TIME_FORMAT) : null;
    }
}
