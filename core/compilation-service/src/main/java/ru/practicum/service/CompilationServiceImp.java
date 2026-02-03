package ru.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.event.client.EventClient;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.DataConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Compilation;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.repository.CompilationRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompilationServiceImp implements CompilationService {
    EventClient eventClient;
    CompilationRepository compilationRepository;
    String errorMessageNotFound = "Подборка с id=%d не найдена";

    @Override
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        try {
            validateNewCompilationDto(newCompilationDto);
            Set<EventShortDto> eventSet = getEventsForCompilation(newCompilationDto.getEvents());
            Compilation compilation = createAndSaveCompilation(newCompilationDto, eventSet);
            return CompilationMapper.toDto(compilation, eventSet);
        } catch (DataAccessException e) {
            log.error("Ошибка доступа", e);
            throw new DataConflictException("Ошибка доступа");
        } catch (Exception e) {
            log.error("Ошибка базы данных", e);
            throw new DataConflictException("Ошибка базы данных");
        }
    }

    @Override
    public void deleteById(long compId) {
        compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, compId)));
        compilationRepository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, compId)));
        Set<Long> eventIds = updateRequest.getEvents();
        Set<EventShortDto> eventsSet;
        if (eventIds == null || eventIds.isEmpty()) {
            eventsSet = new HashSet<>();
        } else {
            eventsSet = eventClient.getEventsByIds(eventIds);
        }
        Boolean pinned = updateRequest.getPinned();
        if (pinned != null) {
            compilation.setPinned(pinned);
        }
        String title = updateRequest.getTitle();
        if (title != null) {
            compilation.setTitle(title);
        }
        compilation.setEvents(eventsSet.stream().mapToLong(EventShortDto::getId).boxed().collect(Collectors.toSet()));
        compilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(compilation, eventsSet);
    }

    @Override
    public void deleteEventFromCompilation(Long eventId) {
        List<Compilation> compilations = compilationRepository.findCompilationsByEventId(eventId);
        compilations.forEach(compilation -> compilation.getEvents().remove(eventId));
        compilationRepository.saveAll(compilations);
    }

    public List<CompilationDto> getAllComps(boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Compilation> compilations = compilationRepository.findByPinned(pinned, page);
        return compilations.stream()
                .map(compilation -> CompilationMapper.toDto(compilation, eventClient.getEventsByIds(compilation.getEvents())))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompById(long compId) {
        if (compId <= 0) {
            throw new BadParameterException("Значение id меньше 1");
        }
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format(errorMessageNotFound, compId)));
        return CompilationMapper.toDto(compilation, eventClient.getEventsByIds(compilation.getEvents()));
    }

    private void validateNewCompilationDto(NewCompilationDto newCompilationDto) {
        if (newCompilationDto == null) {
            throw new IllegalArgumentException("newCompilationDto равен null");
        }
        if (newCompilationDto.getTitle() == null || newCompilationDto.getTitle().isBlank()) {
            throw new IllegalArgumentException("Заголовок равен null");
        }
    }

    private Set<EventShortDto> getEventsForCompilation(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<EventShortDto> eventsWithInitiators = eventClient.getEventsByIds(eventIds);
        return new HashSet<>(eventsWithInitiators);
    }

    private Compilation createAndSaveCompilation(NewCompilationDto newCompilationDto, Set<EventShortDto> events) {
        Compilation compilation = CompilationMapper.toEntity(newCompilationDto, events);
        validateCompilationBeforeSave(compilation);
        return compilationRepository.save(compilation);
    }

    private void validateCompilationBeforeSave(Compilation compilation) {
        if (compilation.getTitle() == null || compilation.getTitle().isBlank()) {
            throw new IllegalArgumentException("Заголовок подборки обязателен");
        }
    }
}
