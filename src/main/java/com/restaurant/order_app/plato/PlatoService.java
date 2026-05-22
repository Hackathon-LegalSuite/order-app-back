package com.restaurant.order_app.plato;

import com.restaurant.order_app.plato.dto.IngredienteEnPlatoResponse;
import com.restaurant.order_app.plato.dto.PlatoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Lógica de negocio para consultas del menú. */
@Service
@RequiredArgsConstructor
public class PlatoService {

    private final PlatoRepository platoRepository;

    /** Retorna todos los platos del menú mapeados a su DTO de respuesta. */
    public List<PlatoResponse> listarTodos() {
        return platoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Convierte una entidad Plato a su DTO, mapeando cada PlatoIngrediente a IngredienteEnPlatoResponse. */
    private PlatoResponse toResponse(Plato plato) {
        List<IngredienteEnPlatoResponse> ingredientes = plato.getIngredientes().stream()
                .map(pi -> IngredienteEnPlatoResponse.builder()
                        .id(pi.getIngrediente().getId())
                        .nombre(pi.getIngrediente().getNombre())
                        .obligatorio(pi.getObligatorio())
                        .build())
                .toList();

        return PlatoResponse.builder()
                .id(plato.getId())
                .nombre(plato.getNombre())
                .descripcion(plato.getDescripcion())
                .precio(plato.getPrecio())
                .categoria(plato.getCategoria().name())
                .preparada(plato.getPreparada())
                .imagenUrl(plato.getImagenUrl())
                .ingredientes(ingredientes)
                .build();
    }
}
