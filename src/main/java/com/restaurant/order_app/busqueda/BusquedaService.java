package com.restaurant.order_app.busqueda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.order_app.busqueda.dto.BusquedaRequest;
import com.restaurant.order_app.busqueda.dto.BusquedaResponse;
import com.restaurant.order_app.busqueda.dto.LlmParseResult;
import com.restaurant.order_app.busqueda.dto.IngredienteExcluirInfo;
import com.restaurant.order_app.ingrediente.Caracteristica;
import com.restaurant.order_app.ingrediente.CaracteristicaRepository;
import com.restaurant.order_app.ingrediente.Ingrediente;
import com.restaurant.order_app.ingrediente.IngredienteRepository;
import com.restaurant.order_app.plato.Categoria;
import com.restaurant.order_app.plato.Plato;
import com.restaurant.order_app.plato.PlatoRepository;
import com.restaurant.order_app.plato.dto.IngredienteEnPlatoResponse;
import com.restaurant.order_app.plato.dto.PlatoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/** Orquesta la búsqueda inteligente: construye el prompt, llama a Groq, filtra platos y resuelve ingredientes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusquedaService {

    private final GroqClient groqClient;
    private final PlatoRepository platoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final CaracteristicaRepository caracteristicaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Interpreta el prompt del cliente con Groq, filtra el menú según los parámetros extraídos
     * y resuelve los IDs de los ingredientes a excluir para facilitar el paso de creación de pedido.
     */
    @Transactional(readOnly = true)
    public BusquedaResponse buscar(BusquedaRequest request) {
        // 1. Cargar contexto del menú para incluirlo en el prompt
        List<String> caracteristicasDisponibles = caracteristicaRepository.findAll()
                .stream().map(Caracteristica::getNombre).toList();
        List<String> ingredientesDisponibles = ingredienteRepository.findAll()
                .stream().map(Ingrediente::getNombre).toList();

        // 2. Construir prompt y llamar a Groq
        String prompt = buildPrompt(request.getPrompt(), caracteristicasDisponibles, ingredientesDisponibles);
        LlmParseResult parsed = llamarGroq(prompt);

        // 3. Filtrar platos con los parámetros extraídos
        List<Plato> todos = platoRepository.findAll();
        List<Plato> filtrados = filtrar(todos, parsed);

        // 4. Resolver IDs de ingredientes a excluir
        List<IngredienteExcluirInfo> excluir = resolverIngredientesExcluir(parsed.getIngredientesExcluir());

        return BusquedaResponse.builder()
                .mensaje(parsed.getMensaje())
                .ingredientesExcluir(excluir.isEmpty() ? null : excluir)
                .platos(filtrados.stream().map(this::toResponse).toList())
                .build();
    }

    /**
     * Construye el prompt completo que se envía a Gemini.
     * Incluye el contexto del menú (características e ingredientes) para que el modelo
     * no invente valores que no existen en la BD.
     */
    private String buildPrompt(String userPrompt, List<String> caracteristicas, List<String> ingredientes) {
        return """
                Eres un asistente de restaurante. Tu tarea es interpretar el mensaje del cliente
                y extraer parámetros de búsqueda basándote SOLO en los datos disponibles del menú.

                DATOS DEL MENÚ:
                - Categorías válidas: ENTRADA, BEBIDA, PLATO_FUERTE, POSTRE
                - Características disponibles: %s
                - Ingredientes disponibles: %s

                INSTRUCCIONES:
                1. "busqueda": el plato o ingrediente principal que el cliente QUIERE comer. Ej: si dice "quiero carne" → "Carne de res". Si dice "quiero hamburguesa" → "hamburguesa".
                2. "caracteristicas": usá únicamente nombres de la lista disponible. Ej: si dice "picante" → ["Picante"].
                3. "categoria": usá únicamente los valores válidos o null si no se menciona.
                4. "precioMaximo": precio máximo en números si el cliente menciona un límite de precio.
                5. "ingredientesExcluir": SOLO ingredientes que el cliente NO quiere o mencionó como alergia. Ej: "sin cebolla", "alérgico al gluten". NUNCA pongas aquí ingredientes que el cliente quiere comer.
                6. "mensaje": mensaje amigable explicando qué encontraste.

                EJEMPLOS:
                - "Quiero comer algo con carne" → busqueda: "Carne de res", ingredientesExcluir: []
                - "Quiero una hamburguesa sin cebolla" → busqueda: "hamburguesa", ingredientesExcluir: ["Cebolla"]
                - "Algo picante que cueste menos de 20000" → caracteristicas: ["Picante"], precioMaximo: 20000

                Respondé ÚNICAMENTE con este JSON (sin texto adicional):
                {
                  "mensaje": "string",
                  "busqueda": "string o null",
                  "caracteristicas": [],
                  "categoria": "string o null",
                  "precioMaximo": number o null,
                  "ingredientesExcluir": []
                }

                MENSAJE DEL CLIENTE: %s
                """.formatted(
                String.join(", ", caracteristicas),
                String.join(", ", ingredientes),
                userPrompt
        );
    }

    /** Llama a Groq y parsea la respuesta JSON. Lanza 503 si algo falla. */
    private LlmParseResult llamarGroq(String prompt) {
        try {
            String json = groqClient.enviar(prompt);
            return objectMapper.readValue(json, LlmParseResult.class);
        } catch (Exception e) {
            // log.error("Error al llamar a Groq: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de búsqueda inteligente no está disponible en este momento");
        }
    }

    /** Aplica los filtros extraídos por Gemini en secuencia. */
    private List<Plato> filtrar(List<Plato> platos, LlmParseResult parsed) {
        return platos.stream()
                .filter(p -> filtrarPorCategoria(p, parsed.getCategoria()))
                .filter(p -> filtrarPorCaracteristicas(p, parsed.getCaracteristicas()))
                .filter(p -> filtrarPorBusqueda(p, parsed.getBusqueda()))
                .filter(p -> filtrarPorPrecio(p, parsed.getPrecioMaximo()))
                .toList();
    }

    private boolean filtrarPorCategoria(Plato plato, String categoria) {
        if (categoria == null || categoria.isBlank()) return true;
        try {
            return plato.getCategoria() == Categoria.valueOf(categoria.toUpperCase());
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Filtra platos cuyo ingrediente tenga al menos una de las características indicadas.
     * Requiere @Transactional para acceder a la colección lazy Ingrediente.caracteristicas.
     */
    private boolean filtrarPorCaracteristicas(Plato plato, List<String> caracteristicas) {
        if (caracteristicas == null || caracteristicas.isEmpty()) return true;
        List<String> buscadas = caracteristicas.stream().map(String::toLowerCase).toList();
        return plato.getIngredientes().stream()
                .flatMap(pi -> pi.getIngrediente().getCaracteristicas().stream())
                .map(c -> c.getNombre().toLowerCase())
                .anyMatch(buscadas::contains);
    }

    /** Filtra por nombre, descripción o ingredientes del plato (case-insensitive). */
    private boolean filtrarPorBusqueda(Plato plato, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) return true;
        String term = busqueda.toLowerCase();
        return plato.getNombre().toLowerCase().contains(term)
                || (plato.getDescripcion() != null && plato.getDescripcion().toLowerCase().contains(term))
                || plato.getIngredientes().stream()
                        .anyMatch(pi -> pi.getIngrediente().getNombre().toLowerCase().contains(term));
    }

    private boolean filtrarPorPrecio(Plato plato, BigDecimal precioMaximo) {
        if (precioMaximo == null) return true;
        return plato.getPrecio().compareTo(precioMaximo) <= 0;
    }

    /** Busca en la BD los IDs de los ingredientes mencionados por el cliente para excluir. */
    private List<IngredienteExcluirInfo> resolverIngredientesExcluir(List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) return List.of();
        return ingredienteRepository.findAll().stream()
                .filter(i -> nombres.stream()
                        .anyMatch(n -> n.equalsIgnoreCase(i.getNombre())))
                .map(i -> IngredienteExcluirInfo.builder()
                        .id(i.getId())
                        .nombre(i.getNombre())
                        .build())
                .toList();
    }

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
