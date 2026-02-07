package ru.practicum.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.event.dto.*;
import ru.practicum.event_service.dto.EventDtoForRequestService;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.request_service.dto.UpdRequestsStatusResult;

import java.util.List;

public interface EventService {

    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto getByUser(Long userId, Long eventId);

    List<EventShortDto> getAllByUser(Long userId, int from, int size);

    EventFullDto updateByUser(Long userId, Long eventId, UpdEventUserRequest updEventUserRequest);

    EventFullDto updateByAdmin(Long eventId, UpdEventAdminRequest updEventAdminRequest);

    List<EventFullDto> searchForAdmin(AdminEventSearchParams params);

    EventFullDto getPublicBy(Long eventId, Long userId, HttpServletRequest request);

    List<EventFullDto> getPublicBy(UserEventSearchParams params, HttpServletRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    UpdRequestsStatusResult updateRequests(Long userId, Long eventId, EventRequestStatusUpdateRequest updDto);

    void like(Long userId, Long eventId);

    List<EventShortDto> getRecommendations(Long userId, int maxResults);

    EventDtoForRequestService incrementConfirmedRequests(Long eventId);

    EventDtoForRequestService getEventDtoForRequestService(Long eventId);
}
