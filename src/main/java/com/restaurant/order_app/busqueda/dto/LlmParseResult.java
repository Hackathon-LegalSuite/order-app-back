package com.restaurant.order_app.busqueda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estructura que el LLM devuelve tras interpretar el prompt del cliente.
 * Cada campo es opcional — el modelo solo llena los que puede inferir del mensaje.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmParseResult {

    /** Mensaje amigable generado por el LLM para mostrarle al cliente. */
    private String mensaje;

    /** Término de búsqueda libre (ej: "hamburguesa", "pollo"). */
    private String busqueda;

    /** Características del plato que el cliente desea (ej: "Picante", "Vegetariano"). */
    private List<String> caracteristicas;

    /** Categoría del menú inferida (ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE). */
    private String categoria;

    /** Precio máximo en pesos colombianos. */
    private BigDecimal precioMaximo;

    /** Ingredientes que el cliente quiere evitar (alergias, preferencias). */
    private List<String> ingredientesExcluir;

    /** Ingredientes que el plato debe tener TODOS (operador AND). Ej: "con pollo y arroz". */
    private List<String> ingredientesRequeridos;

    /** Ingredientes donde el plato debe tener AL MENOS UNO (operador OR). Ej: "con arepa o pan". */
    private List<String> ingredientesCualquiera;
}
