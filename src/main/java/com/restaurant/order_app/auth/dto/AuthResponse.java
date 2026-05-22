package com.restaurant.order_app.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/** DTO de respuesta para ambos flujos de autenticación. mesaId solo se incluye en el login del cliente. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String token;
    private String rol;
    private String nombre;
    private Long mesaId;
    private String expiresIn;
}
