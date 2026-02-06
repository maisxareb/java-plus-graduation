package ru.practicum.service;

import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;

import java.util.List;
import java.util.Map;

public interface UserService {
    UserDto addUser(UserRequest userRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long id);

    UserDto getUserById(long userId);

    Map<Long, UserDto> getAllUsers(List<Long> ids);
}
