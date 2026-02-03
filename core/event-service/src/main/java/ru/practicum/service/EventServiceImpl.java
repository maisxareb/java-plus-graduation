package ru.practicum.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.client.CategoryClient;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.client.StatsClient;
import ru.practicum.comment.client.CommentClient;
import ru.practicum.compilation.client.CompilationClient;
import ru.practicum.dto.StatisticsPostResponseDto;
import ru.practicum.event.dto.*;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.CreateConditionException;
import ru.practicum.exception.DataConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Event;
import ru.practicum.model.EventParam;
import ru.practicum.mapper.EventMapper;
import ru.practicum.repository.EventRepository;
import ru.practicum.request.client.RequestClient;
import ru.practicum.request.dto.RequestDto;
import ru.practicum.request.dto.RequestStatus;
import ru.practicum.user.client.UserClient;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;
import ru.practicum.mapper.UserSpecialMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {
    static DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    EventRepository eventJpaRepository;
    CategoryClient categoryClient;
    UserClient userClient;
    RequestClient requestClient;
    CommentClient commentClient;
    CompilationClient compilationClient;
    EntityManager entityManager;

    @Transactional
    @Override
    public EventFullDto create(NewEventDto newEventDto, Long userId) {
        LocalDateTime newEventDateTime = LocalDateTime.parse(newEventDto.getEventDate(), TIME_FORMAT);
        if (HOURS.between(LocalDateTime.now(), newEventDateTime) < 2) {
            throw new ValidationException("Начало события должно быть минимум на два часа позднее текущего момента");
        }
        CategoryDto category = categoryClient.getById(newEventDto.getCategory());
        UserDto user = userClient.getUser(userId);
        if (newEventDto.getDescription().trim().isEmpty() || newEventDto.getAnnotation().trim().isEmpty() || newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Описание пустое");
        }
        Event event = EventMapper.toEvent(newEventDto, category.getId(), user.getId());
        Event savedEvent = eventJpaRepository.save(event);
        return EventMapper.toFullDto(savedEvent, 0, category, UserSpecialMapper.toShortDto(user));
    }

    public List<EventShortDto> getEventsByCategory(Long catId) {
        if (catId <= 0) {
            throw new BadParameterException("Id категории должен быть >0");
        }
        List<Event> events = eventJpaRepository.findByCategory(catId);
        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(events.stream().map(Event::getId).collect(Collectors.toList()));
        CategoryDto category = categoryClient.getById(catId);
        List<Long> initiatorIds = events.stream().mapToLong(Event::getInitiator).boxed().toList();
        Map<Long, UserDto> userDtoMap = userClient.getMapOfUsers(initiatorIds);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e,
                        idViewsMap.getOrDefault(e.getId(), 0L),
                        category,
                        UserSpecialMapper.toShortDto(userDtoMap.get(e.getInitiator()))))
                .collect(Collectors.toList());
    }

    public List<EventShortDto> getAllByUser(Long userId, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventJpaRepository.findByInitiator(userId, page).getContent();
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(events.stream()
                .map(Event::getId)
                .collect(Collectors.toList()));
        if (events.isEmpty()) {
            return new ArrayList<>();
        }
        UserDto user = userClient.getUser(userId);
        List<Long> categoryIds = events.stream().mapToLong(Event::getCategory).boxed().toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.getMap(categoryIds);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e,
                        idViewsMap.getOrDefault(e.getId(), 0L),
                        categoryDtoMap.get(e.getCategory()),
                        UserSpecialMapper.toShortDto(user)))
                .collect(Collectors.toList());
    }

    public EventFullDto getByUserAndId(Long userId, Long eventId) {
        Event event = eventJpaRepository.findByIdAndInitiator(eventId, userId).orElseThrow(() -> new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId)));
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(List.of(event.getId()));
        UserShortDto userShortDto = UserSpecialMapper.toShortDto(userClient.getUser(userId));
        CategoryDto categoryDto = categoryClient.getById(event.getCategory());
        return EventMapper.toFullDto(event, idViewsMap.getOrDefault(event.getId(), 0L), categoryDto, userShortDto);
    }

    @Override
    public EventFullDto getEvent(Long eventId) {
        Event event = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("События с id=%d не найдено", eventId)));
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(List.of(event.getId()));
        UserShortDto userShortDto = UserSpecialMapper.toShortDto(userClient.getUser(event.getInitiator()));
        CategoryDto categoryDto = categoryClient.getById(event.getCategory());
        return EventMapper.toFullDto(event, idViewsMap.getOrDefault(event.getId(), 0L), categoryDto, userShortDto);
    }

    public EventFullDto getEvent(Long eventId, HttpServletRequest request) {
        EventFullDto eventDto = this.getEvent(eventId);
        if (eventDto.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(String.format("Событие с id=%d не опубликовано", eventId));
        }
        StatisticsPostResponseDto endpointHitDto = new StatisticsPostResponseDto();
        endpointHitDto.setApp("main-event-service");
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now().format(TIME_FORMAT));
        endpointHitDto.setUri(request.getRequestURI());
        StatsClient.postHit(endpointHitDto);
        return eventDto;
    }

    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventJpaRepository.findByIdAndInitiator(eventId, userId).orElseThrow(() -> new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId)));
        if (event.getState() == EventState.PUBLISHED) {
            throw new BadParameterException("Нельзя обновлять событие в состоянии 'Опубликовано'");
        }
        String annotation = updateRequest.getAnnotation();
        if (!(annotation == null || annotation.isBlank())) {
            event.setAnnotation(annotation);
        }
        Long categoryId = updateRequest.getCategory();
        if (categoryId != null && categoryId > 0) {
            CategoryDto categoryDto = categoryClient.getById(categoryId);
            if (categoryDto != null) {
                event.setCategory(categoryDto.getId());
            }
        }
        String newDateString = updateRequest.getEventDate();
        if (!(newDateString == null || newDateString.isBlank())) {
            LocalDateTime newDate = LocalDateTime.parse(newDateString, TIME_FORMAT);
            if (HOURS.between(LocalDateTime.now(), newDate) < 2) {
                throw new ValidationException("Начало события должно быть минимум на два часа позднее текущего момента");
            }
            event.setEventDate(newDate);
        }
        Location location = updateRequest.getLocation();
        if (location != null) {
            event.setLocation(location);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < 0) {
                throw new ValidationException("Participant limit cannot be negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        String stateString = updateRequest.getStateAction();
        if (stateString != null && !stateString.isBlank()) {
            switch (StateActionUser.valueOf(stateString)) {
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
            }
        }
        String title = updateRequest.getTitle();
        if (!(title == null || title.isBlank())) {
            event.setTitle(title);
        }
        eventJpaRepository.save(event);
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(List.of(event.getId()));
        Event updatedEvent = eventJpaRepository.findById(event.getId())
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найден", event.getId())));
        UserShortDto userShortDto = UserSpecialMapper.toShortDto(userClient.getUser(updatedEvent.getInitiator()));
        CategoryDto categoryDto = categoryClient.getById(updatedEvent.getCategory());
        return EventMapper.toFullDto(updatedEvent, idViewsMap.getOrDefault(updatedEvent.getId(), 0L), categoryDto, userShortDto);
    }

    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequest adminRequest) {
        Event event = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("События с id=%d не найдено", eventId)));
        String annotation = adminRequest.getAnnotation();
        if (!(annotation == null || annotation.isBlank())) {
            event.setAnnotation(annotation);
        }
        Long categoryId = adminRequest.getCategory();
        if (categoryId != null && categoryId > 0) {
            CategoryDto categoryDto = categoryClient.getById(categoryId);
            if (categoryDto != null) {
                event.setCategory(categoryDto.getId());
            }
        }
        String description = adminRequest.getDescription();
        if (!(description == null || description.isBlank())) {
            event.setDescription(description);
        }
        String newDateString = adminRequest.getEventDate();
        if (!(newDateString == null || newDateString.isBlank())) {
            LocalDateTime newDate = LocalDateTime.parse(newDateString, TIME_FORMAT);
            if (HOURS.between(LocalDateTime.now(), newDate) < 2) {
                throw new ValidationException("Начало события должно быть минимум на два часа позднее текущего момента");
            }
            event.setEventDate(newDate);
        }
        Location location = adminRequest.getLocation();
        if (location != null) {
            event.setLocation(location);
        }
        if (adminRequest.getPaid() != null) {
            event.setPaid(adminRequest.getPaid());
        }
        if (adminRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(adminRequest.getParticipantLimit());
        }
        if (adminRequest.getRequestModeration() != null) {
            event.setRequestModeration(adminRequest.getRequestModeration());
        }
        String stateString = adminRequest.getStateAction();
        if (stateString != null && !stateString.isBlank()) {
            switch (StateActionAdmin.valueOf(stateString)) {
                case PUBLISH_EVENT:
                    if (HOURS.between(LocalDateTime.now(), event.getEventDate()) < 1) {
                        throw new CreateConditionException("Начало события должно быть минимум на один час позже момента публикации");
                    }
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new DataConflictException(String.format("Попытка опубликовать событие с id=%d, которое уже опубликовано.", event.getId()));
                    }
                    if (event.getState() == EventState.CANCELED) {
                        throw new DataConflictException(String.format("Попытка опубликовать событие с id=%d, которое уже отменено.", event.getId()));
                    }
                    event.setState(EventState.PUBLISHED);
                    break;
                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new DataConflictException(String.format("Попытка отменить событие с id=%d, которое уже опубликовано.", event.getId()));
                    }
                    event.setState(EventState.CANCELED);
                    break;
            }
        }
        String title = adminRequest.getTitle();
        if (!(title == null || title.isBlank())) {
            event.setTitle(title);
        }
        eventJpaRepository.save(event);
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(List.of(event.getId()));
        Event updatedEvent = eventJpaRepository.findById(event.getId())
                .orElseThrow(() -> new NotFoundException(String.format("Событие с id=%d не найден", event.getId())));
        UserShortDto userShortDto = UserSpecialMapper.toShortDto(userClient.getUser(updatedEvent.getInitiator()));
        CategoryDto categoryDto = categoryClient.getById(updatedEvent.getCategory());
        return EventMapper.toFullDto(updatedEvent, idViewsMap.getOrDefault(updatedEvent.getId(), 0L), categoryDto, userShortDto);
    }

    @Override
    public Set<EventShortDto> getEventsByIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Event> eventList = eventJpaRepository.findEventsByIdSet(eventIds);
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(eventList.stream().map(Event::getId).collect(Collectors.toList()));
        return new HashSet<>(eventList.stream().map(event -> EventMapper.toShortDto(
                        event,
                        idViewsMap.getOrDefault(event.getId(), 0L),
                        categoryClient.getById(event.getCategory()),
                        UserSpecialMapper.toShortDto(userClient.getUser(event.getInitiator()))))
                .toList()
        );
    }

    @Override
    @Transactional
    public List<RequestDto> getParticipationInfo(Long userId, Long eventId) {
        Event event = eventJpaRepository.findByIdAndInitiator(eventId, userId).orElseThrow(() -> new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId)));
        return requestClient.getAllRequestsEventId(event.getId());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventJpaRepository.findByIdAndInitiator(eventId, userId).orElseThrow(() -> new NotFoundException(String.format("События с id=%d и initiatorId=%d не найдено", eventId, userId)));
        List<RequestDto> requests = requestClient.getAllRequestsEventId(eventId);
        int limit = event.getParticipantLimit();
        if (updateRequest.getStatus() == UpdateRequestState.REJECTED) {
            return rejectRequests(event, requests, updateRequest);
        } else {
            if ((limit == 0 || !event.isRequestModeration())) {
                return confirmAllRequests(event, requests, updateRequest);
            } else {
                return confirmRequests(event, requests, updateRequest);
            }
        }
    }

    @Transactional
    public List<EventFullDto> getEventsAdmin(EventParam p) {
        List<Long> users = p.getUsers();
        List<String> states = p.getStates();
        List<Long> categories = p.getCategories();
        LocalDateTime rangeStart = p.getRangeStart();
        LocalDateTime rangeEnd = p.getRangeEnd();
        int from = p.getFrom();
        int size = p.getSize();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> criteriaQuery = criteriaBuilder.createQuery(Event.class);
        Root<Event> eventRoot = criteriaQuery.from(Event.class);
        criteriaQuery = criteriaQuery.select(eventRoot);
        List<Event> resultEvents;
        Predicate complexPredicate = null;
        if (rangeStart != null && rangeEnd != null) {
            complexPredicate
                    = criteriaBuilder.between(eventRoot.get("eventDate").as(LocalDateTime.class), rangeStart, rangeEnd);
        }
        if (users != null && !users.isEmpty()) {
            Predicate predicateForUsersId
                    = eventRoot.get("initiator").in(users);
            if (complexPredicate == null) {
                complexPredicate = predicateForUsersId;
            } else {
                complexPredicate = criteriaBuilder.and(complexPredicate, predicateForUsersId);
            }
        }
        if (categories != null && !categories.isEmpty()) {
            Predicate predicateForCategoryId
                    = eventRoot.get("category").in(categories);
            if (complexPredicate == null) {
                complexPredicate = predicateForCategoryId;
            } else {
                complexPredicate = criteriaBuilder.and(complexPredicate, predicateForCategoryId);
            }
        }
        if (states != null && !states.isEmpty()) {
            Predicate predicateForStates
                    = eventRoot.get("state").as(String.class).in(states);
            if (complexPredicate == null) {
                complexPredicate = predicateForStates;
            } else {
                complexPredicate = criteriaBuilder.and(complexPredicate, predicateForStates);
            }
        }
        if (complexPredicate != null) {
            criteriaQuery.where(complexPredicate);
        }
        TypedQuery<Event> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(from);
        typedQuery.setMaxResults(size);
        resultEvents = typedQuery.getResultList();
        Map<Long, Long> idViewsMap = StatsClient.getMapIdViews(resultEvents.stream().map(Event::getId).collect(Collectors.toList()));
        List<Long> categoryIds = resultEvents.stream().mapToLong(Event::getCategory).boxed().toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.getMap(categoryIds);
        List<Long> initiatorIds = resultEvents.stream().mapToLong(Event::getInitiator).boxed().toList();
        Map<Long, UserDto> userDtoMap = userClient.getMapOfUsers(initiatorIds);
        return resultEvents.stream()
                .map(e -> EventMapper.toFullDto(e,
                        idViewsMap.getOrDefault(e.getId(), 0L),
                        categoryDtoMap.get(e.getCategory()),
                        UserSpecialMapper.toShortDto(userDtoMap.get(e.getInitiator()))))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<EventShortDto> getEvents(EventParam p) {
        String text = p.getText();
        List<Long> categories = p.getCategories();
        LocalDateTime rangeStart = p.getRangeStart();
        LocalDateTime rangeEnd = p.getRangeEnd();
        Boolean paid = p.getPaid();
        Boolean onlyAvailable = p.getOnlyAvailable();
        int from = p.getFrom();
        int size = p.getSize();
        String sort = p.getSort();
        HttpServletRequest request = p.getRequest();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> criteriaQuery = criteriaBuilder.createQuery(Event.class);
        Root<Event> eventRoot = criteriaQuery.from(Event.class);
        criteriaQuery.select(eventRoot);
        Predicate complexPredicate;
        if (rangeStart != null && rangeEnd != null) {
            complexPredicate = criteriaBuilder.between(eventRoot.get("eventDate").as(LocalDateTime.class), rangeStart, rangeEnd);
        } else {
            complexPredicate = criteriaBuilder.between(eventRoot.get("eventDate").as(LocalDateTime.class), LocalDateTime.now(), LocalDateTime.of(9999, 1, 1, 1, 1, 1));
        }
        if (text != null && !text.isBlank()) {
            String decodeText = URLDecoder.decode(text, StandardCharsets.UTF_8);
            Expression<String> annotationLowerCase = criteriaBuilder.lower(eventRoot.get("annotation"));
            Expression<String> descriptionLowerCase = criteriaBuilder.lower(eventRoot.get("description"));
            Predicate predicateForAnnotation = criteriaBuilder.like(annotationLowerCase, "%" + decodeText.toLowerCase() + "%");
            Predicate predicateForDescription = criteriaBuilder.like(descriptionLowerCase, "%" + decodeText.toLowerCase() + "%");
            Predicate predicateForText = criteriaBuilder.or(predicateForAnnotation, predicateForDescription);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForText);
        }
        if (categories != null && !categories.isEmpty()) {
            if (categories.stream().anyMatch(c -> c <= 0)) {
                throw new ValidationException("Id категории должен быть > 0");
            }
            Predicate predicateForCategoryId = eventRoot.get("category").in(categories);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForCategoryId);
        }
        if (paid != null) {
            Predicate predicateForPaid = criteriaBuilder.equal(eventRoot.get("paid"), paid);
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForPaid);
        }
        if (onlyAvailable != null) {
            Predicate predicateForOnlyAvailable = criteriaBuilder.lt(eventRoot.get("confirmedRequests"), eventRoot.get("participantLimit"));
            complexPredicate = criteriaBuilder.and(complexPredicate, predicateForOnlyAvailable);
        }
        Predicate predicateForPublished = criteriaBuilder.equal(eventRoot.get("state"), EventState.PUBLISHED);
        complexPredicate = criteriaBuilder.and(complexPredicate, predicateForPublished);
        criteriaQuery.where(complexPredicate);
        TypedQuery<Event> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(from);
        typedQuery.setMaxResults(size);
        List<Event> resultEvents = typedQuery.getResultList();
        StatisticsPostResponseDto endpointHitDto = new StatisticsPostResponseDto();
        endpointHitDto.setApp("main-event-service");
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now().format(TIME_FORMAT));
        endpointHitDto.setUri(request.getRequestURI());
        StatsClient.postHit(endpointHitDto);
        Map<Long, Long> idViews = StatsClient.getMapIdViews(resultEvents.stream().map(Event::getId).collect(Collectors.toList()));
        Comparator<EventShortDto> comparator;
        if (sort != null && sort.equals("EVENT_DATE")) {
            comparator = Comparator.comparing(e -> LocalDateTime.parse(e.getEventDate(), TIME_FORMAT));
        } else {
            comparator = Comparator.comparing(EventShortDto::getViews);
        }
        List<Long> categoryIds = resultEvents.stream().mapToLong(Event::getCategory).boxed().toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.getMap(categoryIds);
        List<Long> initiatorIds = resultEvents.stream().mapToLong(Event::getInitiator).boxed().toList();
        Map<Long, UserDto> userDtoMap = userClient.getMapOfUsers(initiatorIds);
        return resultEvents.stream()
                .map(e -> EventMapper.toShortDto(e,
                        idViews.getOrDefault(e.getId(), 0L),
                        categoryDtoMap.get(e.getCategory()),
                        UserSpecialMapper.toShortDto(userDtoMap.get(e.getInitiator()))))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public Set<EventFullDto> getEventsByIdSet(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Event> eventList = eventJpaRepository.findByIdIn(eventIds);
        if (eventList == null || eventList.isEmpty()) {
            return new HashSet<>();
        }
        Map<Long, Long> idViews = StatsClient.getMapIdViews(eventList.stream().map(Event::getId).collect(Collectors.toList()));
        List<Long> categoryIds = eventList.stream().mapToLong(Event::getCategory).boxed().toList();
        Map<Long, CategoryDto> categoryDtoMap = categoryClient.getMap(categoryIds);
        List<Long> initiatorIds = eventList.stream().mapToLong(Event::getInitiator).boxed().toList();
        Map<Long, UserDto> userDtoMap = userClient.getMapOfUsers(initiatorIds);
        return eventList.stream()
                .map(e -> EventMapper.toFullDto(e,
                        idViews.getOrDefault(e.getId(), 0L),
                        categoryDtoMap.get(e.getCategory()),
                        UserSpecialMapper.toShortDto(userDtoMap.get(e.getInitiator()))))
                .collect(Collectors.toSet());
    }

    @Transactional
    protected EventRequestStatusUpdateResult rejectRequests(Event event, List<RequestDto> requests, EventRequestStatusUpdateRequest updateRequest) {
        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        Map<Long, RequestDto> prDtoMap = requests.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        for (long id : updateRequest.getRequestIds()) {
            RequestDto prDto = prDtoMap.get(id);
            if (prDto == null) {
                throw new NotFoundException(String.format("Запросу на обновление статуса, не найдено событие с id=%d", id));
            }
            if (prDto.getStatus().equals(RequestStatus.PENDING)) {
                prDto.setStatus(RequestStatus.REJECTED);
                updateResult.getRejectedRequests().add(prDto);
            } else {
                throw new CreateConditionException(String.format("Нельзя отклонить уже обработанную заявку id=%d", id));
            }
        }
        requestClient.updateAll(updateResult.getRejectedRequests(), event.getId());
        return updateResult;
    }

    @Transactional
    protected EventRequestStatusUpdateResult confirmAllRequests(Event event, List<RequestDto> requests, EventRequestStatusUpdateRequest updateRequest) {
        int confirmedRequestsAmount = event.getConfirmedRequests();
        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        Map<Long, RequestDto> prDtoMap = requests.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        for (long id : updateRequest.getRequestIds()) {
            RequestDto prDto = prDtoMap.get(id);
            if (prDto == null) {
                throw new NotFoundException(String.format("Запросу на обновление статуса, не найдено событие с id=%d", id));
            }
            if (prDto.getStatus().equals(RequestStatus.PENDING)) {
                prDto.setStatus(RequestStatus.CONFIRMED);
                confirmedRequestsAmount++;
                event.setConfirmedRequests(confirmedRequestsAmount);
                eventJpaRepository.save(event);
            } else {
                throw new CreateConditionException(String.format("Нельзя подтвердить уже обработанную заявку id=%d", id));
            }
        }
        requestClient.updateAll(updateResult.getConfirmedRequests(), event.getId());
        return updateResult;
    }

    @Transactional
    protected EventRequestStatusUpdateResult confirmRequests(Event event, List<RequestDto> requests, EventRequestStatusUpdateRequest updateRequest) {
        int confirmedRequestsAmount = event.getConfirmedRequests();
        int limit = event.getParticipantLimit();
        boolean limitAchieved = false;
        EventRequestStatusUpdateResult updateResult = new EventRequestStatusUpdateResult();
        Map<Long, RequestDto> prDtoMap = requests.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        for (long id : updateRequest.getRequestIds()) {
            limitAchieved = confirmedRequestsAmount >= limit;
            RequestDto prDto = prDtoMap.get(id);
            if (prDto == null) {
                throw new NotFoundException(String.format("Запросу на обновление статуса, не найдено событие с id=%d", id));
            }
            if (prDto.getStatus().equals(RequestStatus.PENDING)) {
                if (limitAchieved) {
                    prDto.setStatus(RequestStatus.REJECTED);
                    requestClient.updateOne(prDto, event.getId());
                    updateResult.getRejectedRequests().add(prDto);
                } else {
                    prDto.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequestsAmount++;
                    event.setConfirmedRequests(confirmedRequestsAmount);
                    eventJpaRepository.save(event);
                    updateResult.getConfirmedRequests().add(prDto);
                }
            } else {
                throw new CreateConditionException(String.format("Нельзя подтвердить уже обработанную заявку id=%d", id));
            }
        }
        requestClient.updateAll(updateResult.getRejectedRequests(), event.getId());
        requestClient.updateAll(updateResult.getConfirmedRequests(), event.getId());
        if (limitAchieved) {
            throw new CreateConditionException(String.format(
                    "Превышен лимит на кол-во участников. Лимит = %d, кол-во подтвержденных заявок =%d",
                    limit, confirmedRequestsAmount));
        }
        return updateResult;
    }

    @Override
    public boolean existsByCategory(Long category) {
        return eventJpaRepository.existsByCategory(category);
    }

    @Override
    public void updateConfirmedRequests(Long eventId, Integer newAmount) {
        Event event = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("События с id=%d не найдено", eventId)));
        event.setConfirmedRequests(newAmount);
        eventJpaRepository.save(event);
    }

    @Override
    public void deleteEventsByAuthor(Long authorId) {
        List<Event> eventsForDelete = eventJpaRepository.findByInitiator(authorId);
        eventsForDelete.forEach(event -> {
            requestClient.deleteAllWithEvent(event.getId());
            commentClient.deleteCommentsForEvent(event.getId());
            compilationClient.deleteEventFromCompilation(event.getId());
            eventJpaRepository.deleteById(event.getId());
        });
    }
}
