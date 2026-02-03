package ru.practicum.mapper;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.model.Compilation;

import java.util.Set;
import java.util.stream.Collectors;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation compilation, Set<EventShortDto> eventShortDtoSet) {
        CompilationDto compilationDto = new CompilationDto();
        compilationDto.setId(compilation.getId());
        compilationDto.setPinned(compilation.isPinned());
        compilationDto.setTitle(compilation.getTitle());

        if (eventShortDtoSet == null || eventShortDtoSet.isEmpty()) {
            return compilationDto;
        }
        compilationDto.setEvents(eventShortDtoSet);
        return compilationDto;
    }

    public static Compilation toEntity(NewCompilationDto newCompilationDto, Set<EventShortDto> events) {
        Compilation compilation = new Compilation();
        compilation.setTitle(newCompilationDto.getTitle());
        compilation.setEvents(events.stream().mapToLong(EventShortDto::getId).boxed().collect(Collectors.toSet()));
        compilation.setPinned(newCompilationDto.isPinned());
        return compilation;
    }
}
