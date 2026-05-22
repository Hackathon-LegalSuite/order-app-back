package com.restaurant.order_app.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** DTO de entrada para el login del cliente vía QR. */
@Getter
public class ClienteAuthRequest {

    @NotBlank
    private String nombre;

    /** Código único de la mesa (ej: MESA-05). */
    @NotBlank
    private String codigoMesa;
}
