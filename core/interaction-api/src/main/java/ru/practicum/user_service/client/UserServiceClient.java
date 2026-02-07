package ru.practicum.user_service.client;

import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.user_service.dto.UserDto;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/admin/users/{userId}")
    UserDto getUserById(@PathVariable @NotNull Long userId);

    @GetMapping("/admin/users/by-ids")
    List<UserDto> getUsersByIds(@RequestParam(name = "ids", required = false) List<Long> ids);
}
