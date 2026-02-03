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
public class RequestServiceImp implements RequestService {

    EntityManager entityManager;
    RequestRepository repository;
    EventClient eventClient;
    UserClient userClient;


    @Override
    public List<RequestDto> getAll(Long userId) {
        UserDto userDto = userClient.getUser(userId);
        return repository.findByRequester(userId).stream().map(RequestMapper::toRequestDto).toList();
    }

    @Override
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
        return RequestMapper.toRequestDto(repository.save(request));
    }

    @Override
    public RequestDto cancelRequest(Long userId, Long requestId) {
        UserDto user = userClient.getUser(userId);
        Request request = repository.findById(requestId).orElseThrow(() -> new NotFoundException(String.format("Запрос с id = %d не найден", requestId)));
        repository.updateToCanceled(requestId);
        repository.flush();
        entityManager.clear();
        return RequestMapper.toRequestDto(repository.findById(requestId).get());
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
                .map(RequestMapper::toRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateAll(List<RequestDto> requestDtoList, Long event) {
        List<Long> userIds = requestDtoList.stream()
                .map(RequestDto::getRequester)
                .collect(Collectors.toList());
        Map<Long, UserDto> users = userClient.getMapOfUsers(userIds);

        Map<Long, RequestDto> prDtoMap = requestDtoList.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));

        Map<Long, UserDto> requestUserMap = requestDtoList.stream()
                .collect(Collectors.toMap(RequestDto::getId, pr -> users.get(pr.getRequester())));

        List<Request> prList = requestDtoList.stream()
                .map(pr -> RequestMapper.toRequest(pr, event, pr.getId()))
                .toList();

        repository.saveAll(prList);
    }

    @Transactional
    public void update(RequestDto prDto, Long event) {
        UserDto user = userClient.getUser(prDto.getRequester());
        repository.save(RequestMapper.toRequest(prDto, event, user.getId()));
    }

    @Override
    public void deleteAllWithUser(Long userId) {
        List<Request> requestsForDelete = repository.findByRequester(userId);
        requestsForDelete.forEach(request -> repository.deleteById(request.getId()));
    }

    @Override
    public void deleteAllWithEvent(Long eventId) {
        List<Request> requestsForDelete = repository.findAllByEventId(eventId);
        requestsForDelete.forEach(request -> repository.deleteById(request.getId()));
    }
}
