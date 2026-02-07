package ru.practicum.request_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.request_service.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request_service.dto.ParticipationRequestDto;

import java.util.List;

@Slf4j
@Component
public class RequestServiceClientFallback implements RequestServiceClient {

    /**
     * Fallback для получения заявок на участие в событии пользователя
     * Возвращает пустой список, чтобы вызывающий код мог продолжить работу
     */
    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.error("Вызов getEventRequests({}, {}) упал в fallback. Возврат пустого списка.", userId, eventId);
        return List.of();
    }

    /**
     * Fallback для получения заявок по списку ID
     * Возвращает пустой список, так как сервис недоступен
     */
    @Override
    public List<ParticipationRequestDto> getRequestsByIds(List<Long> requestIds) {
        log.error("Вызов getRequestsByIds({}) упал в fallback. Возврат пустого списка.", requestIds);
        return List.of();
    }

    /**
     * Fallback для обновления статусов заявок
     * Критичная операция - возврат пустого списка может привести к неконсистентности данных
     */
    @Override
    public List<ParticipationRequestDto> updateRequestStatuses(EventRequestStatusUpdateRequest request) {
        log.error("Вызов updateRequestStatuses({}) упал в fallback. Возврат пустого списка.", request);
        return List.of();
    }

    /**
     * Fallback для проверки,является ли пользователь участником события
     * Возвращает false по умолчанию - "безопасный" вариант
     */
    @Override
    public boolean isParticipant(Long userId, Long eventId) {
        log.error("Вызов isParticipant({}, {}) упал в fallback. Возврат пустого списка.", userId, eventId);
        return false;
    }
}
