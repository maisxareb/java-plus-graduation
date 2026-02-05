package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.user.dto.UserShortDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {
    long id;
    @NotBlank
    String annotation;
    @NotNull
    CategoryDto category;
    int confirmedRequests;
    @NotBlank
    String createdOn;
    @NotBlank
    String description;
    @NotBlank
    String eventDate;
    @NotNull
    UserShortDto initiator;
    @NotNull
    Location location;
    boolean paid;
    int participantLimit = 0;
    @NotBlank
    String publishedOn;
    boolean requestModeration = false;
    EventState state = EventState.PENDING;
    @NotBlank
    String title;
    long views;
}
