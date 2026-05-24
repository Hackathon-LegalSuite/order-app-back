package com.restaurant.order_app.busqueda.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** Cuerpo de la solicitud de búsqueda inteligente. */
@Getter
public class BusquedaRequest {

    @NotBlank
    private String prompt;
}
