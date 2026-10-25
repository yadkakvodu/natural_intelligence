package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("errorMessage", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleNotFound(RuntimeException ex) {
        // Если ошибка связана с тем, что что-то не найдено
        if (ex.getMessage() != null && ex.getMessage().contains("не найдена")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("errorMessage", ex.getMessage()));
        }

        if (ex.getMessage() != null && ex.getMessage().contains("недоступен")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("errorMessage", "Сервис генерации временно недоступен. Попробуйте позже."));
        }

        // Все остальные непредвиденные ошибки (500)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("errorMessage", "Внутренняя ошибка сервера"));
    }
}