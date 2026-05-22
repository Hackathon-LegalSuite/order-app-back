package com.restaurant.order_app.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/** Manejador global de excepciones. Intercepta errores de toda la app y retorna respuestas estructuradas. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Retorna 404 cuando se accede a un endpoint que no existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Endpoint no encontrado",
                request.getRequestURI(),
                LocalDateTime.now()
        );
    }

    /** Retorna el código HTTP correcto cuando el servicio lanza ResponseStatusException (401, 400, 403, etc.). */
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getReason(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /** Captura cualquier excepción no manejada y retorna 500. */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor",
                request.getRequestURI(),
                LocalDateTime.now()
        );
    }
}
