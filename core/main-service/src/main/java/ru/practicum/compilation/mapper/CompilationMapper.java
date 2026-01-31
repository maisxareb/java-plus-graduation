package ru.practicum.compilation.mapper;

import ru.practicum.client.StatsClient;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.model.dto.CompilationDto;
import ru.practicum.compilation.model.dto.NewCompilationDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CompilationMapper {


    public static CompilationDto toDto(Compilation compilation) {
        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compilation.getId());
        compilationDto.setPinned(compilation.isPinned());
        compilationDto.setTitle(compilation.getTitle());

        Set<Event> events = compilation.getEvents();
        if (events == null || events.size() == 0) {
            return compilationDto;
        }
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));

        compilationDto.setEvents(compilation.getEvents().stream()
                .map(e -> EventMapper.toShortDto(e, idViewsMap.get(e.getId())))
                .collect(Collectors.toSet()));
        return compilationDto;
    }

    public static Compilation toEntity(NewCompilationDto newCompilationDto, Set<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setEvents(events);
        compilation.setPinned(newCompilationDto.isPinned());
        return compilation;
    }
}
