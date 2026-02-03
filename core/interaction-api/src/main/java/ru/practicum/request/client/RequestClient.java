package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.request.dto.RequestDto;

import java.util.List;

@FeignClient(name = "request-service", path = RequestClient.FEIGN_CLIENT_PATH)
public interface RequestClient {
    String FEIGN_CLIENT_PATH = "/feign/requests";

    @GetMapping
    List<RequestDto> getAllRequestsEventId(@RequestParam("eventId") Long eventId);

    @PostMapping("/update/all")
    void updateAll(@RequestBody List<RequestDto> requestDtoList, @RequestParam("eventId") Long eventId);

    @PostMapping("update/one")
    void updateOne(@RequestBody RequestDto requestDto, @RequestParam("eventId") Long eventId);

    @PostMapping("/user/delete")
    void deleteAllWithUser(@RequestParam(name = "userId") Long userId);

    @PostMapping("/event/delete")
    void deleteAllWithEvent(@RequestParam(name = "eventId") Long eventId);
}
