package com.restaurant.order_app.pedido.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

/** Representa un plato dentro de la solicitud de creación de pedido. */
@Getter
public class ItemRequest {

    @NotNull
    private Long platoId;

    /** IDs de ingredientes a excluir. Solo puede contener ingredientes con obligatorio=false. */
    private List<Long> ingredientesExcluidos;
}
