package com.restaurant.order_app.busqueda;

import com.restaurant.order_app.busqueda.dto.BusquedaRequest;
import com.restaurant.order_app.busqueda.dto.BusquedaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone el endpoint de búsqueda inteligente del menú usando lenguaje natural. */
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
public class BusquedaController {

    private final BusquedaService busquedaService;

    /** POST /menu/buscar — interpreta el prompt del cliente y retorna platos coincidentes con un mensaje del asistente. */
    @PostMapping("/buscar")
    public ResponseEntity<BusquedaResponse> buscar(@Valid @RequestBody BusquedaRequest request) {
        return ResponseEntity.ok(busquedaService.buscar(request));
    }
}
