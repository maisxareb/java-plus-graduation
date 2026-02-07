package ru.practicum.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.stats.CollectorClient;
import ru.practicum.client.stats.RecommendationsClient;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event_service.dto.EventDtoForRequestService;
import ru.practicum.request_service.client.RequestServiceClient;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.dto.UpdRequestStatus;
import ru.practicum.request_service.dto.UpdRequestsStatusResult;
import ru.practicum.request_service.enums.RequestStatus;
import ru.practicum.user_service.client.UserServiceClient;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.exception.BadRequestException;
import ru.practicum.user_service.exception.ConflictException;
import ru.practicum.user_service.exception.NotFoundException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static java.time.ZoneOffset.UTC;
import static ru.practicum.event.model.EventState.CANCELED;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Все методы по умолчанию только для чтения
public class EventServiceImpl implements EventService {

    // Внешние сервисы для взаимодействия с другими микросервисами
    private final UserServiceClient userServiceClient; // Клиент сервиса пользователей
    private final EventRepository eventRepository; // Репозиторий событий
    private final RequestServiceClient requestServiceClient; // Клиент сервиса заявок на участие
    private final CategoryRepository categoryRepository; // Репозиторий категорий

    private final EventMapper eventMapper; // Маппер для преобразования DTO <-> Entity

    // GRPC-клиенты для сбора статистики и рекомендаций
    private final CollectorClient grpcCollectorClient; // Для записи просмотров и лайков
    private final RecommendationsClient grpcAnalyzerClient; // Для получения рекомендаций

    //Создание нового события пользователем
    @Override
    @Transactional
    public EventFullDto create(Long userId, final NewEventDto newDto) {
        log.debug("Метод create(); userId={}, newDto={}", userId, newDto);

        this.checkStartDate(newDto.getEventDate()); // Проверка даты события
        UserDto userDto = userServiceClient.getUserById(userId); // Получение пользователя
        if (userDto == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        Category category = this.findCategoryBy(newDto.getCategory()); // Поиск категории

        // Создание и сохранение события
        Event event = eventMapper.toEntity(newDto);
        event.setLocation(newDto.getLocation());
        event.setInitiatorId(userDto.getId());
        event.setCategory(category);
        event = eventRepository.save(event);

        log.debug("Создан event={}", event);

        return eventMapper.toFullDto(event);
    }

    //Получение всех событий пользователя с пагинацией
    @Override
    public List<EventShortDto> getAllByUser(Long userId, int from, int size) {
        log.debug("Метод getAllByUser(); userId={}", userId);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        Page<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);

        return events.map(eventMapper::toShortDto).getContent();
    }

    //Получение конкретного события пользователя
    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        log.debug("Метод getByUser(); eventId={}, userId={}", eventId, userId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} у user id={} не найдено", eventId, userId));

        return eventMapper.toFullDto(event);
    }

    /**
     * Обновление события пользователем
     * Можно обновлять только события в состоянии PENDING или CANCELED
     */
    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdEventUserRequest updDto) {
        log.debug("Метод userUpdate(); userId={}, eventId: {}, dto={}", userId, eventId, updDto);

        this.checkEventDateForUpdate(updDto); // Проверка даты обновления

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event id={} не найдено; User id={} ", eventId, userId));

        // Проверка состояния события
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event id={} нельзя изменить; его status={}", eventId, event.getState());
        }

        log.debug("Найден Event в репозитории; event={}", event);

        if (!(event.getState().equals(CANCELED) || event.getState().equals(EventState.PENDING))) {
            throw new ConflictException("Event id={} нельзя обновить пока оно опубликовано", eventId);
        }

        // Обновление категории
        if (updDto.getCategory() != null) {
            event.setCategory(this.findCategoryBy(updDto.getCategory()));
        }

        // Обработка изменения состояния
        if (updDto.getStateAction() != null) {
            switch (updDto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING); // Отправка на модерацию
                case CANCEL_REVIEW -> event.setState(CANCELED); // Отмена
            }
        }

        eventMapper.updateFromDto(updDto, event); // Частичное обновление
        event = eventRepository.save(event);

        log.debug("Метод userUpdate(); Event обновлен в репозитории event={}", event);

        return eventMapper.toFullDto(event);
    }

    //Получение заявок на участие в событии пользователя
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.debug("Метод getEventRequests(); userId={}, eventId={}", userId, eventId);

        List<ParticipationRequestDto> requests = requestServiceClient.getEventRequests(userId, eventId);

        return requests;
    }

    //Обновление статусов заявок на участие в событии
    @Override
    @Transactional
    public UpdRequestsStatusResult updateRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest updDto) {
        log.debug("Метод updateRequests(), userId={}, eventId={}", userId, eventId);

        Event event = this.findEventBy(eventId);
        List<ParticipationRequestDto> requestDtos = requestServiceClient.getRequestsByIds(
                updDto.getRequestIds().stream().toList()
        );

        if (requestDtos.isEmpty()) {
            return UpdRequestsStatusResult.builder()
                    .confirmedRequests(List.of())
                    .rejectedRequests(List.of())
                    .build();
        }

        UpdRequestsStatusResult result;

        switch (updDto.getStatus()) {
            case UpdRequestStatus.CONFIRMED -> { // Подтверждение заявок
                if (event.getConfirmedRequests() == event.getParticipantLimit().longValue()) {
                    throw new ConflictException("На Event id={} больше нет мест", eventId);
                }

                // Расчет доступных мест
                int availableSlots = event.getParticipantLimit() == 0
                        ? requestDtos.size() // Если лимит 0 - нет ограничений
                        : event.getParticipantLimit().intValue() - event.getConfirmedRequests().intValue();

                // Разделение заявок на подтверждаемые и отклоняемые
                List<Long> toConfirmIds = requestDtos.stream()
                        .limit(availableSlots)
                        .map(ParticipationRequestDto::getId)
                        .toList();

                List<Long> toRejectIds = requestDtos.size() > availableSlots
                        ? requestDtos.stream()
                        .skip(availableSlots)
                        .map(ParticipationRequestDto::getId)
                        .toList()
                        : List.of();

                // Обновление статусов через внешний сервис
                List<ParticipationRequestDto> confirmedDtos = List.of();
                List<ParticipationRequestDto> rejectedDtos = List.of();

                if (!toConfirmIds.isEmpty()) {
                    confirmedDtos = requestServiceClient.updateRequestStatuses(
                            EventRequestStatusUpdateRequest.builder()
                                    .requestIds(new HashSet<>(toConfirmIds))
                                    .status(UpdRequestStatus.CONFIRMED)
                                    .build()
                    );
                }

                if (!toRejectIds.isEmpty()) {
                    rejectedDtos = requestServiceClient.updateRequestStatuses(
                            EventRequestStatusUpdateRequest.builder()
                                    .requestIds(new HashSet<>(toRejectIds))
                                    .status(UpdRequestStatus.REJECTED)
                                    .build()
                    );
                }

                // Обновление счетчика подтвержденных заявок
                event.setConfirmedRequests(event.getConfirmedRequests() + confirmedDtos.size());
                eventRepository.save(event);

                result = UpdRequestsStatusResult.builder()
                        .confirmedRequests(confirmedDtos)
                        .rejectedRequests(rejectedDtos)
                        .build();
            }

            case UpdRequestStatus.REJECTED -> { // Отклонение заявок
                if (requestDtos.stream().anyMatch(dto ->
                        dto.getStatus() == RequestStatus.CONFIRMED)) {
                    throw new ConflictException("Нельзя отклонить подтверждённые заявки");
                }

                List<ParticipationRequestDto> rejectedDtos = requestServiceClient.updateRequestStatuses(
                        EventRequestStatusUpdateRequest.builder()
                                .requestIds(updDto.getRequestIds())
                                .status(UpdRequestStatus.REJECTED)
                                .build()
                );

                result = UpdRequestsStatusResult.builder()
                        .confirmedRequests(List.of())
                        .rejectedRequests(rejectedDtos)
                        .build();
            }

            default -> throw new IllegalArgumentException("Неизвестный статус: " + updDto.getStatus());
        }

        return result;
    }

    //FАдминское обновление события
    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdEventAdminRequest updDto) {
        log.debug("Метод adminUpdateEvent(); eventId: {}, dto={}", eventId, updDto);

        Event event = this.findEventBy(eventId);

        eventMapper.updateFromDto(updDto, event);

        this.checkEventDateForPublish(updDto.getEventDate()); // Проверка даты для публикации

        if (updDto.getStateAction() != null) {
            switch (updDto.getStateAction()) {
                case PUBLISH_EVENT -> { // Публикация события
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(EventState.PUBLISHED);
                        event.setPublishedOn(Instant.now()); // Запись времени публикации
                    } else if (event.getState().equals(CANCELED) ||
                            event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Event id={} нельзя опубликовать; его status={}",
                                eventId, event.getState());
                    }

                    log.debug("Для Event назначен статус={}, время публикации publishedOn={}",
                            event.getState(), event.getPublishedOn());
                }
                case REJECT_EVENT -> { // Отклонение события
                    if (event.getState().equals(EventState.PENDING)) {
                        event.setState(CANCELED);
                    } else if (event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Опубликованные Event не могут быть отклонены");
                    }

                    log.debug("Для Event назначен статус={}", event.getState());
                }
            }
        }

        event = eventRepository.save(event);

        log.debug("Метод adminUpdate(); Event обновлен в репозитории event={}", event);

        return eventMapper.toFullDto(event);
    }

    //Поиск событий для администратора с фильтрами
    @Override
    public List<EventFullDto> searchForAdmin(AdminEventSearchParams params) {
        log.debug("Метод adminSearchEvents; {}", params);

        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        // Фильтры
        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            conditions.add(event.initiatorId.in(params.getUsers()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            conditions.add(event.state.in(params.getStates()));
        }

        if (params.getRangeStart() != null) {
            Instant rangeStart = params.getRangeStart().atZone(UTC).toInstant();
            conditions.add(event.eventDate.after(rangeStart));
        }

        if (params.getRangeEnd() != null) {
            Instant rangeEnd = params.getRangeEnd().atZone(UTC).toInstant();
            conditions.add(event.eventDate.before(rangeEnd));
        }

        // Комбинирование условий
        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(Expressions.TRUE);

        log.debug("{}", finalCondition);

        // Пагинация
        int page = params.getFrom() / params.getSize();
        Pageable pageable = PageRequest.of(page, params.getSize());

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        return events.map(eventMapper::toFullDto).getContent();
    }

    /**
     * Получение публичного события по id с записью просмотра
     */
    @Override
    public EventFullDto getPublicBy(Long eventId, Long userId, HttpServletRequest request) {
        log.debug("Метод getPublicById(); eventId={}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Опубликованного Event id={} нет", eventId));

        grpcCollectorClient.recordView(eventId, userId); // Запись просмотра

        return eventMapper.toFullDto(event);
    }

    /**
     * Поиск публичных событий с фильтрами и сортировкой
     * Используется для главной страницы и поиска событий
     */
    @Override
    public List<EventFullDto> getPublicBy(UserEventSearchParams params, HttpServletRequest request) {
        log.debug("Метод publicSearchMany; {}", params);

        QEvent event = QEvent.event;
        List<BooleanExpression> conditions = new ArrayList<>();

        conditions.add(event.state.eq(EventState.PUBLISHED)); // Только опубликованные

        // Фильтры поиска
        if (params.getText() != null && !params.getText().isEmpty()) {
            conditions.add(
                    event.annotation.containsIgnoreCase(params.getText())
                            .or(event.description.containsIgnoreCase(params.getText())));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            conditions.add(event.category.id.in(params.getCategories()));
        }

        if (params.getPaid() != null) {
            conditions.add(event.paid.eq(params.getPaid()));
        }

        if (params.getRangeStart() != null) {
            Instant rangeStart = params.getRangeStart().atZone(UTC).toInstant();
            conditions.add(event.eventDate.after(rangeStart));
        }

        if (params.getRangeEnd() != null) {
            Instant rangeEnd = params.getRangeEnd().atZone(UTC).toInstant();
            conditions.add(event.eventDate.before(rangeEnd));
        }

        // По умолчанию показываем только будущие события
        if (params.getRangeStart() == null && params.getRangeEnd() == null) {
            conditions.add(event.eventDate.after(Instant.now()));
        }

        if (params.getOnlyAvailable() != null) {
            conditions.add(event.confirmedRequests.lt(event.participantLimit.longValue()));
        }

        BooleanExpression finalCondition = conditions.stream()
                .reduce(BooleanExpression::and)
                .orElse(Expressions.TRUE);

        log.debug("{}", finalCondition);

        int page = params.getFrom() / params.getSize();

        Pageable pageable = null;

        // Сортировка результатов
        switch (params.getSort()) {
            case EVENT_DATE -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.ASC, "eventDate"));
            case RATING -> pageable =
                    PageRequest.of(page, params.getSize(), Sort.by(Sort.Direction.DESC, "views"));
        }

        Page<Event> events = eventRepository.findAll(finalCondition, pageable);

        return events.map(eventMapper::toFullDto).getContent();
    }

    /**
     * лайк событию
     * Доступно только участникам прошедших событий
     */
    @Override
    public void like(Long userId, Long eventId) {
        if (!requestServiceClient.isParticipant(userId, eventId)) {
            throw new BadRequestException("User id={} не является участником event eventId={}", userId, eventId);
        }

        if (!eventRepository.existsByIdAndEventDateBefore(eventId, Instant.now())) {
            throw new BadRequestException("Event eventId={} еще не прошло", eventId);
        }

        grpcCollectorClient.recordLike(eventId, userId); // Запись лайка
    }

    //Получение персонализированных рекомендаций событий для пользователя
    @Override
    public List<EventShortDto> getRecommendations(Long userId, int maxResults) {
        Map<Long, Double> scoreByEvent = grpcAnalyzerClient.getRecommendationsForUser(userId, maxResults);
        Set<Long> eventIds = scoreByEvent.keySet();
        List<Event> events = eventRepository.findAllByIdIn(eventIds);

        return events.stream()
                .map(eventMapper::toShortDto)
                .peek(dto -> dto.setRating(scoreByEvent.get(dto.getId()))) // Установка рейтинга
                .toList();
    }

    @Override
    @Transactional
    public EventDtoForRequestService incrementConfirmedRequests(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        eventRepository.save(event);
        return eventMapper.toEventDtoForRequestService(event);
    }

    //Получение DTO события для сервиса заявок
    @Override
    public EventDtoForRequestService getEventDtoForRequestService(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        return eventMapper.toEventDtoForRequestService(event);
    }

    //Вспомогательные методы для поиска сущностей и валидации
    private Category findCategoryBy(Long categoryId) {
        log.debug("Поиск Category id={} в репозитории", categoryId);

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Объект Category id={} не найден", categoryId));
    }

    private Event findEventBy(Long eventId) {
        log.debug("Поиск Event id={} в репозитории", eventId);

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Объект Event id={} не найден", eventId));
    }

    private void checkStartDate(LocalDateTime eventDate) {
        log.debug("Проверка даты при СОЗДАНИИ");

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата Event при СОЗДАНИИ должна быть в будущем, мин. через 2 часа");
        }
    }

    private void checkEventDateForUpdate(UpdEventUserRequest updDto) {
        log.debug("Проверка даты Event при ОБНОВЛЕНИИ");

        if (updDto.getEventDate() != null) {
            this.checkStartDate(updDto.getEventDate());
        }
    }

    private void checkEventDateForPublish(LocalDateTime eventDate) {
        log.debug("Проверка даты Event при ПУБЛИКАЦИИ");

        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Дата Event при ПУБЛИКАЦИИ должна быть в будущем, мин. через 1 час");
        }
    }
}