package ru.practicum.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;

@Mapper(componentModel = "spring")
public interface UserSpecialMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    UserShortDto toShortDto(UserDto user);

    static UserShortDto toShortDtoStatic(UserDto user) {
        if (user == null) {
            return null;
        }
        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(user.getId());
        userShortDto.setName(user.getName());
        return userShortDto;
    }
}
