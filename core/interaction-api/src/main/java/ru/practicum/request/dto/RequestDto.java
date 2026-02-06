package ru.practicum.request.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestDto {
    String created;
    Long event;
    Long id;
    Long requester;
    RequestStatus status;
}
