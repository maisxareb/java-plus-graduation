package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.service.EventService;

import java.util.Set;

@RestController
@RequestMapping(EventControllerFeign.FEIGN_PATH)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventControllerFeign {
    final EventService eventService;
    public static final String FEIGN_PATH = "/feign/events";
    static final String ID_PATH = "/{eventId}";

    @GetMapping(ID_PATH)
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable(name = "eventId") @Positive Long eventId) {
        return eventService.getEvent(eventId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Set<EventShortDto> getEventsByIds(@RequestParam(name = "eventIds") Set<Long> eventIds) {
        return eventService.getEventsByIds(eventIds);
    }

    @GetMapping("/exists")
    public boolean existsByCategory(@RequestParam(name = "categoryId") Long categoryId) {
        return eventService.existsByCategory(categoryId);
    }

    @PostMapping(ID_PATH + "/update-confirmed-requests")
    public void updateConfirmedRequests(@PathVariable(name = "eventId") @Positive Long eventId, @RequestParam(name = "newAmount") Integer newAmount) {
        eventService.updateConfirmedRequests(eventId, newAmount);
    }

    @PostMapping("/delete")
    public void deleteEventsByAuthor(@RequestParam(name = "authorId") Long authorId) {
        eventService.deleteEventsByAuthor(authorId);
    }
}
