package ru.practicum.user_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.user_service.dto.UserDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserServiceClientFallback {

    public UserDto getUserById(Long userId) {
        log.warn("Fallback: пользователь {} не найден (user-service недоступен или ошибка)", userId);
        return null;
    }

    public List<UserDto> getUsersByIds(List<Long> ids) {
        log.warn("Fallback: список пользователей не получен (user-service недоступен или ошибка)");
        return Collections.emptyList();
    }
}