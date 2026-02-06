package ru.practicum.comment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "comment-service", path = CommentClient.FEIGN_CLIENT_PATH)
public interface CommentClient {
    String FEIGN_CLIENT_PATH = "/feign/comments";

    @PostMapping("/user/delete")
    void deleteCommentsForUser(@RequestParam(name = "userId") Long userId);

    @PostMapping("/event/delete")
    void deleteCommentsForEvent(@RequestParam(name = "eventId") Long eventId);
}
