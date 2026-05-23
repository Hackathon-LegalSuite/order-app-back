package com.restaurant.order_app.pedido.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** DTO de respuesta para un pedido con sus ítems y estados actuales. */
@Getter
@Builder
public class PedidoResponse {
    private Long id;
    private Long mesaId;
    private String clienteNombre;
    private String clienteSessionId;
    private LocalDateTime creadoEn;
    private List<ItemResponse> items;
}
