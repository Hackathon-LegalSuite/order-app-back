package com.restaurant.order_app.pedido;

import com.restaurant.order_app.pedido.dto.CrearPedidoRequest;
import com.restaurant.order_app.pedido.dto.PedidoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/** Expone los endpoints de pedidos para el cliente autenticado por QR. */
@RestController
@RequestMapping("/pedido")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    /** POST /pedidos — crea un nuevo pedido desde la mesa. Valida ingredientes excluidos y mesaId del JWT. */
    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody CrearPedidoRequest request) {
        return ResponseEntity.ok(pedidoService.crearPedido(request));
    }

}
