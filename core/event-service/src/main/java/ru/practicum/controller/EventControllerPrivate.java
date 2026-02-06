package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.request.dto.RequestDto;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventControllerPrivate {
    final EventService eventService;
    static final String EVENT_ID_PATH = "/{eventId}";
    static final String EVENT_ID_REQUEST_PATH = "/{eventId}/requests";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto postEvent(@PathVariable(name = "userId") Long userId,
                                  @Valid @RequestBody NewEventDto newEventDto) {
        EventFullDto eventDto = eventService.create(newEventDto, userId);
        log.info("Создается новое событие title={}, date={}", eventDto.getTitle(), eventDto.getEventDate());
        return eventDto;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventsByUser(@PathVariable(name = "userId") @Positive Long userId,
                                               @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
                                               @RequestParam(name = "size", defaultValue = "10") @PositiveOrZero int size) {
        List<EventShortDto> eventShortDtos = eventService.getAllByUser(userId, from, size);
        log.info("Получен список событий пользователя с id={}", userId);
        return eventShortDtos;
    }

    @GetMapping(EVENT_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventByUserAndId(@PathVariable(name = "userId") @Positive Long userId,
                                            @PathVariable(name = "eventId") @Positive Long eventId) {
        EventFullDto eventFullDto = eventService.getByUserAndId(userId, eventId);
        log.info("Получено событие с Id={}  пользователя с id={}", eventId, userId);
        return eventFullDto;
    }

    @PatchMapping(EVENT_ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable(name = "userId") @Positive Long userId,
                                    @PathVariable(name = "eventId") @Positive Long eventId,
                                    @Valid @RequestBody UpdateEventUserRequest updateRequest) {
        EventFullDto eventFullDto = eventService.updateEvent(userId, eventId, updateRequest);
        log.info("Обновлено событие с Id={} , добавленное пользователем с id={}", eventId, userId);
        return eventFullDto;
    }

    @GetMapping(EVENT_ID_REQUEST_PATH)
    @ResponseStatus(HttpStatus.OK)
    public List<RequestDto> getParticipationInfo(@PathVariable(name = "userId") @Positive Long userId,
                                                 @PathVariable(name = "eventId") @Positive Long eventId) {
        List<RequestDto> partRequestDtoList = eventService.getParticipationInfo(userId, eventId);
        log.info("Получена информация о запросах на участие в событии с Id={} пользователя с id={}", eventId, userId);
        return partRequestDtoList;
    }

    @PatchMapping(EVENT_ID_REQUEST_PATH)
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateEventStatus(@PathVariable(name = "userId") @Positive Long userId,
                                                            @PathVariable(name = "eventId") @Positive Long eventId,
                                                            @RequestBody EventRequestStatusUpdateRequest statusUpdateRequest) {
        EventRequestStatusUpdateResult updateStatusResult = eventService.updateStatus(userId, eventId, statusUpdateRequest);
        log.info("Обновлен статус события с Id={} пользователя с id={}. Статус = {}", eventId, userId, statusUpdateRequest.getStatus().toString());
        return updateStatusResult;
    }
}
