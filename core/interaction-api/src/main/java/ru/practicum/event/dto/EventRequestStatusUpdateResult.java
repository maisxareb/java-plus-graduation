package ru.practicum.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.request.dto.RequestDto;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateResult {
    List<RequestDto> confirmedRequests = new ArrayList<>();
    List<RequestDto> rejectedRequests = new ArrayList<>();
}
