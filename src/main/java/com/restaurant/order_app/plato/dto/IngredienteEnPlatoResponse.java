package com.restaurant.order_app.plato.dto;

import lombok.Builder;
import lombok.Getter;

/** DTO que representa un ingrediente dentro de un plato, incluyendo si puede excluirse. */
@Getter
@Builder
public class IngredienteEnPlatoResponse {
    private Long id;
    private String nombre;
    private Boolean obligatorio;
}
