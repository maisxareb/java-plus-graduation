package ru.practicum.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.CompilationService;

@RestController
@RequestMapping("/feign/compilation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompilationControllerFeign {

    CompilationService service;

    @PostMapping("/event/delete")
    public void deleteEventFromCompilation(@RequestParam("eventId") Long eventId) {
        service.deleteEventFromCompilation(eventId);
    }
}
