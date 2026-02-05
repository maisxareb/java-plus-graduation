package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.model.User;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserRequest;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    User toUser(UserRequest userRequest);

    UserDto toUserDto(User user);

    default User toUserStatic(UserRequest userRequest) {
        return toUser(userRequest);
    }

    default UserDto toUserDtoStatic(User user) {
        return toUserDto(user);
    }
}