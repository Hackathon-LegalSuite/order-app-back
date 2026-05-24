package com.restaurant.order_app.busqueda.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.restaurant.order_app.plato.dto.PlatoResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Respuesta de la búsqueda inteligente: mensaje del asistente, ingredientes a excluir y platos encontrados. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BusquedaResponse {

    /** Mensaje generado por Gemini explicando los resultados. */
    private String mensaje;

    /** Ingredientes con ID resuelto que el cliente quiere evitar. Vacío si no mencionó ninguno. */
    private List<IngredienteExcluirInfo> ingredientesExcluir;

    /** Platos que coinciden con los criterios extraídos del prompt. */
    private List<PlatoResponse> platos;
}
