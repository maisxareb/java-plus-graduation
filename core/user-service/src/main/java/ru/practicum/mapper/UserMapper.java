package ru.practicum.mapper;

import ru.practicum.model.User;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;

public class UserMapper {
    public static User toUser(UserRequest userRequest) {
        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setName(userRequest.getName());
        return user;
    }

    public static UserDto toUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        return userDto;
    }
}
