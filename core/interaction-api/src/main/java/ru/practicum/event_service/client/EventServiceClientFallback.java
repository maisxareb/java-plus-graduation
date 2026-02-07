package ru.practicum.event_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.event_service.dto.EventDtoForRequestService;

@Slf4j
@Component
public class EventServiceClientFallback implements EventServiceClient {

    /**
     * Fallback-метод для getEventById
     * Вызвается при недоступности event-service или ошибках связи
     *
     * @param eventId ID события, которое нужно получить
     * @return null, так как сервис недоступен
     */
    @Override
    public EventDtoForRequestService getEventById(Long eventId) {
        log.warn("Fallback: событие {} не найдено (event-service недоступен или ошибка)", eventId);
        return null;
    }

    /**
     * Fallback-метод для incrementConfirmedRequests
     * Вызывается при недоступности event-service при попытке увеличить счетчик подтвержденных заявок
     *
     * @param eventId ID события, для которого нужно увеличить счетчик
     * @return null, так как операция не может быть выполненa
     */
    @Override
    public EventDtoForRequestService incrementConfirmedRequests(Long eventId) {
        log.warn("Fallback: операция прошла некорректно для события {} (event-service недоступен или ошибка)", eventId);
        return null;
    }
}