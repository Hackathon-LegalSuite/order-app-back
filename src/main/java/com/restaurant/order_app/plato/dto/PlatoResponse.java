package com.restaurant.order_app.plato.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** DTO de respuesta para un plato del menú. preparada solo aparece en platos de categoría BEBIDA. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatoResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String categoria;
    private Boolean preparada;
    private List<IngredienteEnPlatoResponse> ingredientes;
}
