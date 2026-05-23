package com.restaurant.order_app.pedido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

/** Cuerpo de la solicitud para crear un nuevo pedido. Mesa y nombre se extraen del JWT. */
@Getter
public class CrearPedidoRequest {

    @NotEmpty
    @Valid
    private List<ItemRequest> items;
}
