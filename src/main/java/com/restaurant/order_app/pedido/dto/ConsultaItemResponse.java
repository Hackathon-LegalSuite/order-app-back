package com.restaurant.order_app.pedido.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.restaurant.order_app.item.EstadoItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** Ítem de pedido retornado en GET /pedido. Mesa y mesero solo aparecen para COCINERO y MESERO. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultaItemResponse {
    private Long itemId;
    private Long platoId;
    private String platoNombre;
    private BigDecimal precio;
    private String imagenUrl;
    private List<String> ingredientesExcluidos;
    private EstadoItem estado;
    private Integer mesa;
    private String mesero;
}
