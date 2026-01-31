package ru.practicum.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.model.Statistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface StatMapper {

    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "timestamp", expression = "java(formatDate(statistics.getTimestamp()))")
    StatisticsPostResponseDto toDto(Statistics statistics);

    @Mapping(target = "timestamp", expression = "java(parseDate(dto.getTimestamp()))")
    Statistics fromDto(StatisticsPostResponseDto dto);

    default String formatDate(LocalDateTime date) {
        return date != null ? date.format(TIME_FORMAT) : null;
    }

    default LocalDateTime parseDate(String date) {
        return date != null ? LocalDateTime.parse(date, TIME_FORMAT) : null;
    }
}
