package ru.practicum.compilation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "compilation-service", path = "/feign/compilation")
public interface CompilationClient {
    @PostMapping("/event/delete")
    void deleteEventFromCompilation(@RequestParam("eventId") Long eventId);
}
