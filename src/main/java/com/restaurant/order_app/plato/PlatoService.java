package com.restaurant.order_app.plato;

import com.restaurant.order_app.plato.dto.IngredienteEnPlatoResponse;
import com.restaurant.order_app.plato.dto.PlatoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lógica de negocio para consultas del menú. */
@Service
@RequiredArgsConstructor
public class PlatoService {

    private final PlatoRepository platoRepository;

    /** Retorna el detalle de un plato por ID. Lanza 404 si no existe. */
    public PlatoResponse buscarPorId(Long id) {
        Plato plato = platoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plato no encontrado"));
        return toResponse(plato);
    }

    /** Retorna los platos filtrados por categoría. Lanza 400 si la categoría no existe. */
    public List<PlatoResponse> listarPorCategoria(String categoria) {
        Categoria cat;
        try {
            cat = Categoria.valueOf(categoria.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría inválida: " + categoria);
        }
        return platoRepository.findByCategoria(cat).stream()
                .map(this::toResponse)
                .toList();
    }

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
