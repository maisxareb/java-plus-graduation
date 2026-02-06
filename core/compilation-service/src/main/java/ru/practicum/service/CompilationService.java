package ru.practicum.service;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> getAllComps(boolean pinned, int from, int size);

    CompilationDto getCompById(long compId);

    CompilationDto create(NewCompilationDto newCompilationDto);

    void deleteById(long compId);

    CompilationDto update(long compId, UpdateCompilationRequest updateRequest);

    void deleteEventFromCompilation(Long eventId);
}
