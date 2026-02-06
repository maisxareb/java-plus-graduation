package ru.practicum.service;

import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.client.EventClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventState;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.CreateConditionException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.model.Request;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.repository.RequestRepository;
import ru.practicum.request.dto.RequestDto;
import ru.practicum.request.dto.RequestStatus;
import ru.practicum.user.client.UserClient;
import ru.practicum.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true)
public class RequestServiceImp implements RequestService {

    EntityManager entityManager;
    RequestRepository repository;
    EventClient eventClient;
    UserClient userClient;
    RequestMapper requestMapper;

    @Override
    public List<RequestDto> getAll(Long userId) {
        userClient.getUser(userId);
        return repository.findByRequester(userId).stream()
                .map(requestMapper::toRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public RequestDto create(Long userId, Long eventId) {
        UserDto requestor = userClient.getUser(userId);

        Optional<Request> duplicatedRequest = repository.findByEventIdAndRequesterId(eventId, userId);
        if (duplicatedRequest.isPresent()) {
            throw new CreateConditionException(String.format("Запрос от пользователя id = %d на событие c id = %d уже существует", userId, eventId));
        }

        EventFullDto event = eventClient.getEvent(eventId);
        if (event.getInitiator().getId() == userId) {
            throw new CreateConditionException("Пользователь не может создавать запрос на участие в своем событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new CreateConditionException(String.format("Событие с id = %d не опубликовано", eventId));
        }

        if (event.getParticipantLimit() != 0) {
            if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
                throw new CreateConditionException(String.format("У события с id = %d достигнут лимит участников %d", eventId, event.getParticipantLimit()));
            }
        }

        Request request = new Request();
        request.setRequester(requestor.getId());
        request.setEvent(event.getId());
        request.setCreated(LocalDateTime.now());

        if ((event.getParticipantLimit() == 0) || (!event.isRequestModeration())) {
            request.setStatus(RequestStatus.CONFIRMED);
            int confirmedRequestsAmount = event.getConfirmedRequests();
            confirmedRequestsAmount++;
            event.setConfirmedRequests(confirmedRequestsAmount);
            eventClient.updateConfirmedRequests(event.getId(), confirmedRequestsAmount);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        return requestMapper.toRequestDto(repository.save(request));
    }

    @Override
    @Transactional
    public RequestDto cancelRequest(Long userId, Long requestId) {
        userClient.getUser(userId);

        repository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Запрос с id = %d не найден", requestId)));

        repository.updateToCanceled(requestId);
        repository.flush();
        entityManager.clear();

        return requestMapper.toRequestDto(repository.findById(requestId).get());
    }

    @Override
    public List<RequestDto> getAllRequestsEventId(Long eventId) {
        if (eventId < 0) {
            throw new BadParameterException("Id события должен быть больше 0");
        }

        List<Request> partRequests = repository.findAllByEventId(eventId);
        if (partRequests == null || partRequests.isEmpty()) {
            return new ArrayList<>();
        }

        return partRequests.stream()
                .map(requestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateAll(List<RequestDto> requestDtoList, Long eventId) {
        List<Long> userIds = requestDtoList.stream()
                .map(RequestDto::getRequester)
                .collect(Collectors.toList());
        Map<Long, UserDto> users = userClient.getMapOfUsers(userIds);

        List<Request> prList = requestDtoList.stream()
                .map(pr -> requestMapper.toRequest(pr, eventId, pr.getId()))
                .toList();

        repository.saveAll(prList);
    }

    @Override
    @Transactional
    public void update(RequestDto prDto, Long event) {
        userClient.getUser(prDto.getRequester());

        repository.save(requestMapper.toRequest(prDto, event, prDto.getRequester()));
    }

    @Override
    @Transactional
    public void deleteAllWithUser(Long userId) {
        List<Request> requestsForDelete = repository.findByRequester(userId);
        repository.deleteAll(requestsForDelete);
    }

    @Override
    @Transactional
    public void deleteAllWithEvent(Long eventId) {
        List<Request> requestsForDelete = repository.findAllByEventId(eventId);
        repository.deleteAll(requestsForDelete);
    }
}
