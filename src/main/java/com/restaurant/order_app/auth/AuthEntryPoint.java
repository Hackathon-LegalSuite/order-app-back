package com.restaurant.order_app.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Punto de entrada para requests rechazados por falta o invalidez del JWT.
 * Devuelve un 401 con el mismo formato JSON del resto de la API.
 */
@Component
public class AuthEntryPoint implements AuthenticationEntryPoint {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Se invoca automáticamente cuando Spring Security bloquea un request no autenticado.
     * Lee el atributo jwt_error del request (cargado por JwtFilter) para dar un mensaje específico.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String mensaje = (String) request.getAttribute("jwt_error");
        if (mensaje == null) mensaje = "Se requiere autenticación";

        String json = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                HttpServletResponse.SC_UNAUTHORIZED,
                mensaje.replace("\"", "\\\""),
                request.getRequestURI(),
                LocalDateTime.now().format(FORMATTER)
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
