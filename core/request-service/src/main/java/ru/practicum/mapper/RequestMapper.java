package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.model.Request;
import ru.practicum.request.dto.RequestDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "created", source = "created", qualifiedByName = "localDateTimeToString")
    RequestDto toRequestDto(Request request);

    @Mapping(target = "created", source = "dto.created", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "requester", source = "user")
    Request toRequest(RequestDto dto, Long event, Long user);

    default Request toRequestForUpdate(RequestDto dto) {
        return toRequest(dto, dto.getEvent(), dto.getRequester());
    }

    @Named("localDateTimeToString")
    default String localDateTimeToString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(TIME_FORMAT) : null;
    }

    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateTimeStr) {
        return dateTimeStr != null ? LocalDateTime.parse(dateTimeStr, TIME_FORMAT) : null;
    }

    default RequestDto toRequestDtoStatic(Request request) {
        return toRequestDto(request);
    }

    default Request toRequestStatic(RequestDto dto, Long event, Long user) {
        return toRequest(dto, event, user);
    }
}
