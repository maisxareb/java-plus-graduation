package ru.practicum.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final String status;
    private final String reason;
    private final String message;
    private final String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
}
