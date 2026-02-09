package ru.practicum.event_service.dto;

import lombok.Data;
import ru.practicum.event_service.enums.EventState;

@Data
public class EventDtoForRequestService {

    private Long id;
    private Long initiatorId;
    private EventState state;
    private Integer participantLimit;
    private Long confirmedRequests;
    private Boolean requestModeration;
}
