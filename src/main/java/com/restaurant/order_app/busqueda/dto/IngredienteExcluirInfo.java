package com.restaurant.order_app.busqueda.dto;

import lombok.Builder;
import lombok.Getter;

/** Ingrediente que el cliente quiere excluir, con su ID resuelto desde la BD. */
@Getter
@Builder
public class IngredienteExcluirInfo {
    private Long id;
    private String nombre;
}
