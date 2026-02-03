package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.CommentRequest;
import ru.practicum.comment.dto.CommentResponse;
import ru.practicum.service.CommentService;

import java.util.List;

@RestController
@RequestMapping
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentController {

    final CommentService commentService;
    public static final String PUBLIC_API_PATH = "/event/{eventId}/comment";
    static final String EVENT_ID = "eventId";
    static final String COMMENT_ID = "commentId";
    static final String X_USER_ID = "X-User-Id";
    static final String COMMENT_ID_PATH = "/{commentId}";
    static final String FEIGN_CLIENT_PATH = "/feign/comments";


    @PostMapping(PUBLIC_API_PATH)
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable(EVENT_ID) Long eventId,
                                         @RequestHeader(X_USER_ID) Long userId,
                                         @Valid @RequestBody CommentRequest commentRequest) {
        return commentService.createComment(userId, eventId, commentRequest);
    }

    @PatchMapping(PUBLIC_API_PATH + COMMENT_ID_PATH)
    public CommentResponse updateComment(@PathVariable(EVENT_ID) Long eventId,
                                         @PathVariable(COMMENT_ID) Long commentId,
                                         @RequestHeader(X_USER_ID) Long userId,
                                         @Valid @RequestBody CommentRequest commentRequest) {
        return commentService.updateComment(userId, commentId, commentRequest);
    }

    @GetMapping(PUBLIC_API_PATH)
    public List<CommentResponse> getComments(@PathVariable(EVENT_ID) Long eventId) {
        return commentService.getCommentsByEvent(eventId);
    }

    @GetMapping(PUBLIC_API_PATH + COMMENT_ID_PATH)
    public CommentResponse getComment(@PathVariable(EVENT_ID) Long eventId,
                                      @PathVariable(COMMENT_ID) Long commentId) {
        return commentService.getCommentById(eventId, commentId);
    }

    @DeleteMapping(PUBLIC_API_PATH + COMMENT_ID_PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable(EVENT_ID) Long eventId,
                              @PathVariable(COMMENT_ID) Long commentId,
                              @RequestHeader(X_USER_ID) Long userId) {
        commentService.deleteComment(userId, commentId);
    }

    @PostMapping(FEIGN_CLIENT_PATH + "/user/delete")
    public void deleteCommentsForUser(@RequestParam(name = "userId") Long userId) {
        commentService.deleteCommentsForUser(userId);
    }

    @PostMapping(FEIGN_CLIENT_PATH + "/event/delete")
    public void deleteCommentsForEvent(@RequestParam(name = "eventId") Long eventId) {
        commentService.deleteCommentsForEvent(eventId);
    }
}
