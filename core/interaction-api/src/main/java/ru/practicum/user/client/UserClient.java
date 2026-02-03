package ru.practicum.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.user.dto.UserDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", path = UserClient.PATH)
public interface UserClient {
    String PATH = "/feign/users";

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    UserDto getUser(@PathVariable(name = "id") Long id);

    @GetMapping("/map")
    Map<Long, UserDto> getMapOfUsers(@RequestParam(name = "ids") List<Long> ids);
}
