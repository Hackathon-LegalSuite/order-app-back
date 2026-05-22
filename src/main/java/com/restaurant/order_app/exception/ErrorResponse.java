package com.restaurant.order_app.exception;

import java.time.LocalDateTime;

/** Estructura estándar de respuesta para errores HTTP. Retornada por el GlobalExceptionHandler. */
public record ErrorResponse(
        int status,
        String error,
        String path,
        LocalDateTime timestamp
) {
}
