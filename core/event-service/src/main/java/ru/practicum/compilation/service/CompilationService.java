package ru.practicum.compilation.service;

import org.springframework.stereotype.Service;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationDto;

import java.util.List;

@Service
public interface CompilationService {
    CompilationDto create(NewCompilationDto newCompilationDto);

    CompilationDto update(Long compId, UpdateCompilationDto updateCompilationDto);

    void delete(Long compId);

    CompilationDto getBy(Long compId);

    List<CompilationDto> getAllBy(Boolean pinned, Integer from, Integer size);
}