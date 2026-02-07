package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.stats.CollectorClient;
import ru.practicum.event_service.client.EventServiceClient;
import ru.practicum.event_service.dto.EventDtoForRequestService;
import ru.practicum.event_service.enums.EventState;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.dto.UpdRequestStatus;
import ru.practicum.request_service.enums.RequestStatus;
import ru.practicum.user_service.client.UserServiceClient;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.exception.ConflictException;
import ru.practicum.user_service.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    // Внешние клиенты для взаимодействия с другими сервисами
    private final UserServiceClient userServiceClient;        // Клиент сервиса пользователей
    private final EventServiceClient eventServiceClient;      // Клиент сервиса событий
    private final RequestRepository requestRepository;        // Репозиторий для работы с заявками в БД
    private final CollectorClient grpcCollectorClient;        // GRPC-клиент для сбора статистики

    private final RequestMapper requestMapper;                // Маппер для преобразования Request <-> DTO

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        log.debug("Метод createRequest(); userId={}, eventId={}", userId, eventId);

        // Получение данных пользователя и события через внешние сервисы
        UserDto userDto = findUserBy(userId);
        EventDtoForRequestService eventDto = findEventBy(eventId);

        // 1. Проверка: инициатор события не может участвовать в своем же событии
        if (eventDto.getInitiatorId().equals(userId)) {
            throw new ConflictException("Нельзя участвовать в собственном событии");
        }

        // 2. Проверка: пользователь уже отправил заявку на это событие
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request уже создан ранее");
        }

        // Проверка: событие должно быть опубликовано
        if (!eventDto.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // 4. Проверка: не достигнут ли лимит участников
        long limit = eventDto.getParticipantLimit();         // Максимальное количество участников
        long confirm = eventDto.getConfirmedRequests();      // Текущее количество подтвержденных участников

        if (limit > 0 && confirm >= limit) {
            throw new ConflictException("Достигнут лимит запросов на участие в событии");
        }


        // Автоматическое подтверждение, если:
        // - отключена модерация заявок (requestModeration = false)
        // - или лимит участников не установлен (limit = 0)
        RequestStatus status =
                (!eventDto.getRequestModeration() || limit == 0) ? RequestStatus.CONFIRMED : RequestStatus.PENDING;

        // Если заявка автоматически подтверждена, увеличиваем счетчик подтвержденных заявок
        if (status == RequestStatus.CONFIRMED) {
            EventDtoForRequestService updatedEvent = eventServiceClient.incrementConfirmedRequests(eventId);

            // Проверка ответа от сервиса событий
            if (updatedEvent == null) {
                throw new NotFoundException(
                        "Не удалось увеличить confirmedRequests: сервис event-service недоступен");
            }
        }

        // Создание и сохранение заявки
        Request request = Request.builder()
                .requesterId(userDto.getId())
                .eventId(eventId)
                .status(status)
                .build();
        request = requestRepository.save(request);

        // Запись статистики: регистрация на событие
        grpcCollectorClient.recordRegister(userId, eventId);

        return requestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getAllBy(Long userId) {
        log.debug("Метод getAllBy(); userId={}", userId);

        List<Request> result = requestRepository.findAllByRequesterId(userId);

        return result.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        log.debug("Метод cancel(); userId={}, requestId={}", userId, requestId);

        // Проверка существования пользователя
        this.findUserBy(userId);

        // Поиск и проверка заявки
        Request request = this.findRequestBy(requestId);

        // Проверка прав: пользователь должен быть автором заявки
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User id={} не является автором этого запроса", userId);
        }

        // Изменение статуса на CANCELED
        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);

        return requestMapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.debug("Сервис request-service: получение заявок для eventId={} (userId игнорируется)", eventId);

        List<Request> requests = requestRepository.findAllByEventId(eventId);

        List<ParticipationRequestDto> dtos = requests.stream()
                .map(requestMapper::toDto)
                .toList();

        log.debug("Найдено заявок для eventId={}: {}", eventId, dtos.size());
        return dtos;
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds) {
        // Преобразование List в Set для оптимизации поиска в БД
        Set<Long> idSet = new HashSet<>(requestIds);
        return requestRepository.findAllByIdIn(idSet).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ParticipationRequestDto> updateRequestStatuses(Set<Long> requestIds, UpdRequestStatus newStatus) {
        // Получение всех заявок по IDs
        List<Request> requests = requestRepository.findAllByIdIn(requestIds);

        // Конвертация типа статуса из DTO в enum
        RequestStatus status = toRequestStatus(newStatus);

        // Массовое обновление статусов
        requests.forEach(request -> request.setStatus(status));

        // Сохранение всех изменений
        requestRepository.saveAll(requests);

        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isParticipant(Long userId, Long eventId) {
        // Должно быть: existsByEventIdAndRequesterId(eventId, userId)
        return requestRepository.existsByEventIdAndRequesterId(userId, eventId);
    }

    private RequestStatus toRequestStatus(UpdRequestStatus updStatus) {
        return switch (updStatus) {
            case CONFIRMED -> RequestStatus.CONFIRMED;
            case REJECTED  -> RequestStatus.REJECTED;
        };
    }

    private UserDto findUserBy(Long userId) {
        UserDto userDto = userServiceClient.getUserById(userId);
        if (userDto == null) {
            throw new NotFoundException("User id={}, не существует", userId);
        }
        return userDto;
    }

    // Поиск заявки в локальной БД
    private Request findRequestBy(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request id={} не найден", requestId));
    }

    /**
     * Поиск события через внешний сервис
     * Выбрасывает NotFoundException, если событие не найдено
     */
    private EventDtoForRequestService findEventBy(Long eventId) {
        EventDtoForRequestService eventDto = eventServiceClient.getEventById(eventId);
        if (eventDto == null) {
            throw new NotFoundException("Event id={} не найден", eventId);
        }
        return eventDto;
    }
}