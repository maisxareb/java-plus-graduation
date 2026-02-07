package ru.practicum.user_service.exception;

public class BadRequestException extends ApiError {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Object... args) {
        super(message, args);
    }
}
