package ru.practicum.service;

import ru.practicum.dto.CommentFullDto;
import ru.practicum.dto.CommentPublicDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.dto.UpdCommentDto;

import java.util.List;

public interface CommentService {

    List<CommentPublicDto> getAllBy(Long eventId);

    List<CommentFullDto> getAllBy(Long userId, Long eventId);

    CommentFullDto add(NewCommentDto dto, Long eventId, Long userId);

    CommentFullDto hide(Long eventId, Long commentId, boolean published);

    void delete(Long userId, Long commentId);

    CommentFullDto update(Long userId, Long commentId, UpdCommentDto updDto);
}