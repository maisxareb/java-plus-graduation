package ru.practicum.event_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import ru.practicum.event_service.dto.EventDtoForRequestService;

@FeignClient(name = "event-service", fallback = EventServiceClientFallback.class)
public interface EventServiceClient {

    @GetMapping("/events/internal/{eventId}")
    EventDtoForRequestService getEventById(@PathVariable Long eventId);

    @PutMapping("/events/internal/{eventId}/increment-confirmed")
    EventDtoForRequestService incrementConfirmedRequests(@PathVariable Long eventId);
}
