package ru.practicum.request_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests", fallback = RequestServiceClientFallback.class)
public interface RequestServiceClient {

    @GetMapping("/events/{eventId}")
    List<ParticipationRequestDto> getEventRequests(
            @RequestParam("userId") Long userId,
            @PathVariable("eventId") Long eventId);

    @GetMapping("/by-ids")
    List<ParticipationRequestDto> getRequestsByIds(@RequestParam List<Long> requestIds);


    @PutMapping("/status")
    List<ParticipationRequestDto> updateRequestStatuses(
            @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("{eventId}/participant/{userId}")
    boolean isParticipant(@PathVariable Long userId, @PathVariable Long eventId);
}
