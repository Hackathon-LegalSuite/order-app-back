package com.restaurant.order_app.pedido.dto;

import com.restaurant.order_app.item.EstadoItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** DTO de respuesta para un ítem dentro de un pedido. */
@Getter
@Builder
public class ItemResponse {
    private Long id;
    private Long platoId;
    private String platoNombre;
    private List<String> ingredientesExcluidos;
    private EstadoItem estado;
}
