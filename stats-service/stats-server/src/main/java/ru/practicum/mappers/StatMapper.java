package ru.practicum.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.model.Statistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface StatMapper {
    StatMapper INSTANCE = Mappers.getMapper(StatMapper.class);
    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mapping(target = "timestamp", expression = "java(statistics.getTimestamp().format(TIME_FORMAT))")
    StatisticsPostResponseDto toDto(Statistics statistics);

    @Mapping(target = "timestamp", expression = "java(LocalDateTime.parse(statisticsPostResponseDto.getTimestamp(), TIME_FORMAT))")
    Statistics fromDto(StatisticsPostResponseDto statisticsPostResponseDto);

    default StatisticsPostResponseDto toDtoStatic(Statistics statistics) {
        return toDto(statistics);
    }

    default Statistics fromDtoStatic(StatisticsPostResponseDto statisticsPostResponseDto) {
        return fromDto(statisticsPostResponseDto);
    }
}
