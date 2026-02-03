package ru.practicum.request.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadParameterException;
import ru.practicum.exception.CreateConditionException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.model.dto.RequestDto;
import ru.practicum.request.mapper.RequestMapper;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.repository.UserRepository;
import ru.practicum.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImp implements RequestService {

    private final EntityManager entityManager;
    private final RequestRepository repository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RequestRepository requestRepository;

    @Override
    public List<RequestDto> getAll(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));
        return repository.findByRequester(userId).stream().map(RequestMapper::toRequestDto).toList();
    }

    @Override
    public RequestDto create(Long userId, Long eventId) {
        User requestor = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));

        Request duplicatedRequest = requestRepository.findByIdAndRequester(eventId, userId);
        if (duplicatedRequest != null) {
            throw new CreateConditionException(String.format("Запрос от пользователя id = %d на событие c id = %d уже существует", userId, eventId));
        }
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException(String.format("Событие с id = %d не найдено", eventId)));

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
        request.setRequester(requestor);
        request.setEvent(event);
        request.setCreated(LocalDateTime.now());
        if ((event.getParticipantLimit() == 0) || (!event.isRequestModeration())) {
            request.setStatus(RequestStatus.CONFIRMED);
            int confirmedRequestsAmount = event.getConfirmedRequests();
            confirmedRequestsAmount++;
            event.setConfirmedRequests(confirmedRequestsAmount);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }
        return RequestMapper.toRequestDto(repository.save(request));
    }

    @Override
    public RequestDto cancelRequest(Long userId, Long requestId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));
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
    public void updateAll(List<RequestDto> requestDtoList, Event event) {
        List<Long> userIds = requestDtoList.stream()
                .map(RequestDto::getRequester)
                .collect(Collectors.toList());
        Map<Long, User> users = userService.getAllUsers(userIds).stream()
                .map(UserMapper::toUser)
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, RequestDto> prDtoMap = requestDtoList.stream()
                .collect(Collectors.toMap(RequestDto::getId, e -> e));
        Map<Long, User> requestUserMap = requestDtoList.stream()
                .collect(Collectors.toMap(RequestDto::getId, pr -> users.get(pr.getRequester())));

        List<Request> prList = requestDtoList.stream()
                .map(pr -> RequestMapper.toRequest(pr, event, requestUserMap.get(pr.getId())))
                .collect(Collectors.toList());

        requestRepository.saveAll(prList);
    }

    @Transactional
    public void update(RequestDto prDto, Event event) {
        User user = UserMapper.toUser(userService.getUserById(prDto.getRequester()));
        requestRepository.save(RequestMapper.toRequest(prDto, event, user));
    }
}
