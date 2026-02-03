package ru.practicum.controller;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.RequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@RequestMapping
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestController {
    static final String PUBLIC_API_PATH = "/users/{userId}/requests";
    static final String FEIGN_CLIENT_PATH = "/feign/requests";
    final RequestService service;

    @GetMapping(PUBLIC_API_PATH)
    public List<RequestDto> getAll(@PathVariable("userId") Long userId) {
        return service.getAll(userId);
    }

    @PostMapping(PUBLIC_API_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto create(@PathVariable("userId") Long userId, @RequestParam("eventId") Long eventId) {
        return service.create(userId, eventId);
    }

    @PatchMapping(PUBLIC_API_PATH + "/{requestId}/cancel")
    public RequestDto cancelRequest(@PathVariable("userId") Long userId, @PathVariable("requestId") Long requestId) {
        return service.cancelRequest(userId, requestId);
    }

    @GetMapping(FEIGN_CLIENT_PATH)
    public List<RequestDto> getAllRequestsEventId(@RequestParam("eventId") Long eventId) {
        return service.getAllRequestsEventId(eventId);
    }

    @PostMapping(FEIGN_CLIENT_PATH + "/update/all")
    public void updateAll(@RequestBody List<RequestDto> requestDtoList, @RequestParam("eventId") Long eventId) {
        service.updateAll(requestDtoList, eventId);
    }

    @PostMapping(FEIGN_CLIENT_PATH + "/update/one")
    public void updateOne(@RequestBody RequestDto requestDto, @RequestParam("eventId") Long eventId) {
        service.update(requestDto, eventId);
    }

    @PostMapping(FEIGN_CLIENT_PATH + "/user/delete")
    public void deleteAllWithUser(@RequestParam(name = "userId") Long userId) {
        service.deleteAllWithUser(userId);
    }

    @PostMapping(FEIGN_CLIENT_PATH + "/event/delete")
    public void deleteAllWithEvent(@RequestParam(name = "eventId") Long eventId) {
        service.deleteAllWithEvent(eventId);
    }
}
