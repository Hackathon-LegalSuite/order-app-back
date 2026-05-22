package com.restaurant.order_app.plato;

import com.restaurant.order_app.plato.dto.PlatoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Expone los endpoints del menú para consulta por parte del cliente. */
@RestController
@RequestMapping("/platos")
@RequiredArgsConstructor
public class PlatoController {

    private final PlatoService platoService;

    /** GET /platos — retorna todos los platos del menú con sus ingredientes. */
    @GetMapping
    public ResponseEntity<List<PlatoResponse>> listarTodos() {
        return ResponseEntity.ok(platoService.listarTodos());
    }

    /** GET /platos/{id} — retorna el detalle de un plato por su ID. */
    @GetMapping("/{id}")
    public ResponseEntity<PlatoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(platoService.buscarPorId(id));
    }

    /** GET /platos/categoria/{categoria} — filtra platos por categoría: ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE. */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<PlatoResponse>> listarPorCategoria(@PathVariable String categoria) {
        return ResponseEntity.ok(platoService.listarPorCategoria(categoria));
    }
}
