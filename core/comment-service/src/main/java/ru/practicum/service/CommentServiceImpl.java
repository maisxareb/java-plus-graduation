package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.dto.CommentFullDto;
import ru.practicum.dto.CommentPublicDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.dto.UpdCommentDto;
import ru.practicum.event_service.client.EventServiceClient;
import ru.practicum.event_service.dto.EventDtoForRequestService;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentState;
import ru.practicum.repository.CommentRepository;
import ru.practicum.user_service.client.UserServiceClient;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.exception.ConflictException;
import ru.practicum.user_service.exception.NotFoundException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final UserServiceClient userServiceClient; // Клиент для взаимодействия с сервисом пользователей
    private final EventServiceClient eventServiceClient; // Клиент для взаимодействия с сервисом событий
    private final CommentRepository commentRepository; // Репозиторий для работы с комментариями в БД

    private final CommentMapper commentMapper; // Маппер для преобразования между DTO и сущностями

    /**
     * Скрывает или публикует комментарий к событию.
     * Используется администратором для модерации комментариев.
     */
    @Override
    public CommentFullDto hide(Long eventId, Long commentId, boolean published) {
        log.info("Метод hide(); eventId={}; commentId={}", eventId, commentId);

        // Проверка принадлежности комментария указанному событию
        if (!commentRepository.existsByIdAndEventId(commentId, eventId)) {
            throw new ConflictException("Комментарий не принадлежит указанному событию; eventId={}; commentId={}",
                    eventId, commentId);
        }

        // Получение комментария из БД
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment id={} не найден", commentId));

        // Изменение состояния комментария (публикация/скрытие)
        if (published) {
            comment.setState(CommentState.PUBLIC);
        } else {
            comment.setState(CommentState.HIDE);
        }
        comment = commentRepository.save(comment); // Сохранение изменений

        return commentMapper.toFullDto(comment); // Возврат DTO с полной информацией
    }

    /**
     * Получает все публичные комментарии для указанного события.
     * Используется для отображения комментариев к событию.
     */
    @Override
    public List<CommentPublicDto> getAllBy(Long eventId) {
        log.info("Метод getAllBy(); eventId = {}", eventId);

        List<Comment> comments = commentRepository.findByEventId(eventId);

        // Преобразование сущностей в DTO для публичного отображения
        return comments.stream()
                .map(commentMapper::toPublicDto)
                .toList();
    }

    /**
     * Добавляет новый комментарий к событию.
     * Проверяет, что пользователь не является инициатором события.
     */
    @Override
    public CommentFullDto add(NewCommentDto dto, Long eventId, Long userId) {
        log.info("Метод add(); eventId={}, userId={}; dto={}", eventId, userId, dto);

        // Получение информации о событии через Feign-клиент
        EventDtoForRequestService eventDto = eventServiceClient.getEventById(eventId);

        // Проверка: инициатор события не может комментировать свое событие
        if (eventDto.getInitiatorId().equals(userId)) {
            throw new ConflictException(
                    "Инициатор не может комментировать свои события; eventId={}, userId={}", eventId, userId);
        }

        // Получение информации о пользователе через Feign-клиент
        UserDto userDto = userServiceClient.getUserById(userId);

        // Создание и сохранение комментария
        Comment comment = commentMapper.toEntity(dto);
        comment.setAuthorId(userDto.getId());
        comment.setEventId(eventId);
        comment = commentRepository.save(comment);

        return commentMapper.toFullDto(comment);
    }

    /**
     * Получает все комментарии конкретного пользователя к конкретному событию.
     * Используется для просмотра собственных комментариев к событию.
     */
    @Override
    public List<CommentFullDto> getAllBy(Long userId, Long eventId) {
        log.info("Метод getUserCommentsForEvent(); eventId={}; commentId={}", userId, eventId);

        // Проверка существования события
        try {
            eventServiceClient.getEventById(eventId);
        } catch (Exception e) {
            log.warn("Событие с ID={} не найдено в сервисе событий или проблема с сервисом событий", eventId);
            throw new NotFoundException("Событие с ID=%d не найдено", eventId);
        }

        // Получение комментариев пользователя к событию
        List<Comment> comments = commentRepository.findAllByEventIdAndAuthorId(eventId, userId);

        // Преобразование в DTO с полной информацией
        return comments.stream()
                .map(commentMapper::toFullDto)
                .toList();
    }

    /**
     * Удаляет комментарий пользователя.
     * Проверяет права пользователя на удаление (только автор может удалить).
     */
    @Override
    public void delete(Long userId, Long commentId) {
        log.info("Метод delete(); userId={}, commentId={}", userId, commentId);

        this.checkExistsUserAndComment(userId, commentId); // Проверка прав доступа

        commentRepository.deleteById(commentId); // Удаление комментария
    }

    /**
     * Обновляет существующий комментарий.
     * Проверяет, что пользователь является автором комментария.
     */
    @Override
    public CommentFullDto update(Long userId, Long commentId, UpdCommentDto updDto) {
        log.info("Метод update(); userId={}, commentId={}, dto: {}", userId, commentId, updDto);

        this.checkExistsUserAndComment(userId, commentId); // Проверка прав доступа

        // Получение, обновление и сохранение комментария
        Comment comment = commentRepository.findById(commentId).get();
        commentMapper.updateFromDto(updDto, comment);
        comment = commentRepository.save(comment);

        return commentMapper.toFullDto(comment);
    }

    /**
     * Вспомогательный метод для проверки существования пользователя и комментария,
     * а также прав пользователя на работу с комментарием.
     * Логика проверки:
     * 1. Сначала проверяем, принадлежит ли комментарий пользователю
     * 2. Если нет - проверяем существование пользователя и комментария
     * 3. Если оба существуют, но комментарий не принадлежит пользователю - конфликт
     */
    private void checkExistsUserAndComment(Long userId, Long commentId) {
        log.info("Метод checkExistsUserAndComment(); userId={}, commentId={}", userId, commentId);

        if (!commentRepository.existsByIdAndAuthorId(commentId, userId)) {
            // Проверка существования пользователя
            if (userServiceClient.getUserById(userId) == null) {
                throw new NotFoundException("User id={}, не существует", userId);
            }
            // Проверка существования комментария
            else if (!commentRepository.existsById(commentId)) {
                throw new NotFoundException("Comment id={}, не существует", commentId);
            }
            // Пользователь и комментарий существуют, но комментарий не принадлежит пользователю
            else {
                throw new ConflictException("Пользователь не является автором комментария; " +
                        "userId={}, commentId={}", userId, commentId);
            }
        }
    }
}