package ru.practicum.compilation.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.client.StatsClient;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.model.dto.CompilationDto;
import ru.practicum.compilation.model.dto.NewCompilationDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.mapper.EventMapper;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class CompilationMapper {

    @Autowired
    protected EventMapper eventMapper;

    @Autowired
    protected StatsClient statsClient;

    public CompilationDto toDto(Compilation compilation) {
        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compilation.getId());
        compilationDto.setPinned(compilation.isPinned());
        compilationDto.setTitle(compilation.getTitle());

        Set<Event> events = compilation.getEvents();
        if (events == null || events.isEmpty()) {
            return compilationDto;
        }

        Map<Long, Long> idViewsMap = statsClient.getMapIdViews(
                events.stream()
                        .map(Event::getId)
                        .collect(Collectors.toList())
        );

        compilationDto.setEvents(compilation.getEvents().stream()
                .map(e -> eventMapper.toShortDto(e, idViewsMap.get(e.getId())))
                .collect(Collectors.toSet()));

        return compilationDto;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", source = "newCompilationDto.title")
    @Mapping(target = "pinned", source = "newCompilationDto.pinned")
    @Mapping(target = "events", source = "events")
    public abstract Compilation toEntity(NewCompilationDto newCompilationDto, Set<Event> events);
}
