package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventShortDto {
    long id;
    @NotBlank
    String annotation;
    @NotNull
    CategoryDto category;
    int confirmedRequests;
    @NotBlank
    String eventDate;
    @NotNull
    UserShortDto initiator;
    boolean paid;
    @NotBlank
    String title;
    long views;
}
