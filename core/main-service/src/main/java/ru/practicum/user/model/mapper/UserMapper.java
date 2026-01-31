package ru.practicum.user.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.user.model.User;
import ru.practicum.user.model.dto.UserDto;
import ru.practicum.user.model.dto.UserRequest;
import ru.practicum.user.model.dto.UserShortDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toUser(UserRequest userRequest);

    User toUser(UserDto userDto);

    UserDto toUserDto(User user);

    UserShortDto toShortDto(User user);
}