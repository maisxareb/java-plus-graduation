package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationDto;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.user_service.exception.NotFoundException;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Все методы только для чтения по умолчанию
public class CompilationServiceImpl implements CompilationService {

    // Репозитории для работы с данными
    private final EventRepository eventRepository; // Для работы с событиями
    private final CompilationRepository compilationRepository; // Для работы с подборками

    private final CompilationMapper compilationMapper; // Маппер для преобразования DTO <-> Entity

    /**
     * Создание новой подборки событий
     * Включает поиск и привязку событий по их ID
     */
    @Override
    @Transactional // Метод записи, требуется транзакция
    public CompilationDto create(NewCompilationDto newDto) {
        log.debug("Метод create(); newDto={}", newDto);

        // Нахдим события по переданным ID
        List<Event> events = this.findEventsBy(newDto.getEvents());

        log.info(newDto.getEvents().toString()); // Логируем ID событий

        // Создаем подборку из DTO
        Compilation compilation = compilationMapper.toEntity(newDto);
        compilation.setEvents(events); // Устанавливаем найденные события
        compilation = compilationRepository.save(compilation); // Сохраняем в БД

        log.info(compilation.getEvents().toString()); // Логируем сохраненные события

        return compilationMapper.toDto(compilation); // Возвращаем DTO
    }

    /**
     * Обновление существующей подборки
     * Поддерживает частичное обновление
     */
    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationDto updDto) {
        log.debug("Метод update(); compId={}, updDto={}", compId, updDto);

        // Находим существующую подборку
        Compilation compilation = this.findCompilationBy(compId);
        // Частично обновляем поля из DTO
        compilation = compilationMapper.updateFromDto(updDto, compilation);

        // Если в DTO переданы новые события, обновляем их список
        if (updDto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updDto.getEvents());
            compilation.setEvents(events);
        }
        compilation = compilationRepository.save(compilation); // Сохраняем изменения

        log.debug("compilation={}", compilation);

        return compilationMapper.toDto(compilation);
    }

    /**
     * Удаление подборки по id
     */
    @Override
    @Transactional
    public void delete(Long compId) {
        log.debug("Метод delete(); compId={}", compId);

        compilationRepository.deleteById(compId);
    }

    /**
     * Получение списка подборок с фильтрацией и пагинацией
     * pinned - фильтр по закрепленным-незакрепленным подборкам
     */
    @Override
    public List<CompilationDto> getAllBy(Boolean pinned, Integer from, Integer size) {
        log.debug("Метод getAllBy(); pinned={}, from={}, size={}", pinned, from, size);

        // Рассчитываем страницу для пагинации
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        // Получаем подборки с фильтрацией или без
        Page<Compilation> compilations = pinned != null
                ? compilationRepository.findByPinned(pinned, pageable) // Фильтр по pinned
                : compilationRepository.findAll(pageable); // Все подборки

        // Преобразуем в DTO
        return compilations.getContent()
                .stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    /**
     * Получение подборки по ID
     */
    @Override
    public CompilationDto getBy(Long compId) {
        log.debug("Метод getBy(); compId={}", compId);

        Compilation compilation = this.findCompilationBy(compId);

        log.debug("compilation={}", compilation);

        return compilationMapper.toDto(compilation);
    }

    //Вспомогательный метод: поиск подборки по ID
    private Compilation findCompilationBy(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка не найдена"));
    }

    /**
     * Вспомогательный метод: поиск событий по набору ID
     * Проверяет что все запрошенные события найдены
     */
    private List<Event> findEventsBy(Set<Long> eventsIds) {
        List<Event> events = eventRepository.findEventsByIdIn(eventsIds);

        // Проверяем что найдены все запрошенные события
        if (events.size() != eventsIds.size()) {
            throw new NotFoundException("Некоторые события не найдены");
        }
        return events;
    }
}
