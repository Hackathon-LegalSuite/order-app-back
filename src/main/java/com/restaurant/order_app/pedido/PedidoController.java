package com.restaurant.order_app.pedido;

import com.restaurant.order_app.exception.MensajeResponse;
import com.restaurant.order_app.pedido.dto.ConsultaItemResponse;
import com.restaurant.order_app.pedido.dto.CrearPedidoRequest;
import com.restaurant.order_app.pedido.dto.PedidoResponse;

import java.util.List;
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

    /** POST /pedido — crea un nuevo pedido desde la mesa. Valida ingredientes excluidos y mesaId del JWT. */
    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@Valid @RequestBody CrearPedidoRequest request) {
        return ResponseEntity.ok(pedidoService.crearPedido(request));
    }

    /** GET /pedido — retorna los ítems del pedido. CLIENTE ve solo los suyos; COCINERO/MESERO ven todos con mesa y mesero. */
    @GetMapping
    public ResponseEntity<List<ConsultaItemResponse>> listar() {
        return ResponseEntity.ok(pedidoService.listar());
    }

    /** PATCH /pedido/item/{itemId}/estado — avanza el estado del ítem al siguiente: EN_ESPERA → EN_PROGRESO → LISTO → ENTREGADO. */
    @PatchMapping("/item/{itemId}/estado")
    public ResponseEntity<MensajeResponse> avanzarEstado(@PathVariable Long itemId) {
        return ResponseEntity.ok(new MensajeResponse(pedidoService.avanzarEstado(itemId)));
    }

    /** DELETE /pedido/{pedidoId}/item/{itemId} — elimina un ítem del pedido. Si era el último, elimina el pedido. */
    @DeleteMapping("/{pedidoId}/item/{itemId}")
    public ResponseEntity<MensajeResponse> eliminarItem(@PathVariable Long pedidoId, @PathVariable Long itemId) {
        return ResponseEntity.ok(new MensajeResponse(pedidoService.eliminarItem(pedidoId, itemId)));
    }

}
