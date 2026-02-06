package ru.practicum.event.client;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;

import java.util.Set;

@FeignClient(name = "event-service", path = EventClient.FEIGN_PATH)
public interface EventClient {
    String FEIGN_PATH = "/feign/events";
    String ID_PATH = "/{eventId}";

    @GetMapping(ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getEvent(@PathVariable(name = "eventId") @Positive Long eventId);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    Set<EventShortDto> getEventsByIds(@RequestParam(name = "eventIds") Set<Long> eventIds);

    @GetMapping("/exists")
    boolean existsByCategory(@RequestParam(name = "categoryId") Long categoryId);

    @PostMapping(ID_PATH + "/update-confirmed-requests")
    void updateConfirmedRequests(@PathVariable(name = "eventId") @Positive Long eventId, @RequestParam(name = "newAmount") Integer newAmount);

    @PostMapping("/delete")
    void deleteEventsByAuthor(@RequestParam(name = "authorId") Long authorId);
}
