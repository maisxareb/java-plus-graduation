package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.model.Compilation;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CompilationMapper {

    @Mapping(target = "events", source = "eventShortDtoSet", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    CompilationDto toDto(Compilation compilation, Set<EventShortDto> eventShortDtoSet);

    @Mapping(target = "events", source = "events", qualifiedByName = "eventSetToLongSet")
    Compilation toEntity(NewCompilationDto newCompilationDto, Set<EventShortDto> events);

    @Named("eventSetToLongSet")
    static Set<Long> eventSetToLongSet(Set<EventShortDto> events) {
        if (events == null) {
            return Set.of();
        }
        return events.stream()
                .map(EventShortDto::getId)
                .collect(Collectors.toSet());
    }

    @AfterMapping
    default void afterMapping(@MappingTarget Compilation compilation, Set<EventShortDto> events) {
        if (events != null && !events.isEmpty()) {
            compilation.setEvents(events.stream()
                    .map(EventShortDto::getId)
                    .collect(Collectors.toSet()));
        }
    }
}
