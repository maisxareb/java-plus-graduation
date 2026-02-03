package ru.practicum.service;

import ru.practicum.request.dto.RequestDto;

import java.util.List;

public interface RequestService {
    List<RequestDto> getAll(Long userId);

    RequestDto create(Long userId, Long eventId);

    RequestDto cancelRequest(Long userId, Long requestId);

    List<RequestDto> getAllRequestsEventId(Long eventId);

    void updateAll(List<RequestDto> requestDtoList, Long eventId);

    void update(RequestDto requestDto, Long event);

    void deleteAllWithUser(Long userId);

    void deleteAllWithEvent(Long eventId);
}
