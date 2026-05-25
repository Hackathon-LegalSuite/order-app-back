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
        List<Plato> todos = platoRepository.findAllConCaracteristicas();
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
     * Construye el prompt completo que se envía al LLM.
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
                1. "busqueda": nombre del plato que el cliente busca. Solo para búsqueda por nombre de plato, no por ingrediente. Ej: "quiero hamburguesa" → busqueda: "hamburguesa".
                2. "ingredientesRequeridos": ingredientes que el plato DEBE tener TODOS (AND). Usá cuando el cliente dice "con X y con Y". Ej: "con pollo y arroz" → ["Pollo", "Arroz"].
                3. "ingredientesCualquiera": ingredientes donde el plato debe tener AL MENOS UNO (OR). Usá cuando el cliente dice "con X o Y". Ej: "con arepa o pan" → ["Arepa", "Pan"].
                4. "caracteristicas": usá únicamente nombres de la lista disponible. Ej: "picante" → ["Picante"].
                5. "categoria": usá únicamente los valores válidos o null si no se menciona.
                6. "precioMaximo": precio máximo en números si el cliente menciona un límite de precio.
                7. "ingredientesExcluir": SOLO ingredientes que el cliente NO quiere o mencionó como alergia. NUNCA pongas aquí ingredientes que el cliente quiere comer.
                8. "mensaje": mensaje amigable explicando qué encontraste.

                EJEMPLOS:
                - "Quiero algo con pollo y arroz" → ingredientesRequeridos: ["Pollo", "Arroz"]
                - "Quiero algo con arepa o pan" → ingredientesCualquiera: ["Arepa", "Pan"]
                - "Quiero una hamburguesa sin cebolla" → busqueda: "hamburguesa", ingredientesExcluir: ["Cebolla"]
                - "Algo picante que cueste menos de 20000" → caracteristicas: ["Picante"], precioMaximo: 20000

                Respondé ÚNICAMENTE con este JSON (sin texto adicional):
                {
                  "mensaje": "string",
                  "busqueda": "string o null",
                  "ingredientesRequeridos": [],
                  "ingredientesCualquiera": [],
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
            log.error("Error al llamar a Groq: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de búsqueda inteligente no está disponible en este momento");
        }
    }

    /** Aplica en secuencia los filtros extraídos por el LLM sobre la lista de platos. */
    private List<Plato> filtrar(List<Plato> platos, LlmParseResult parsed) {
        return platos.stream()
                .filter(p -> filtrarPorCategoria(p, parsed.getCategoria()))
                .filter(p -> filtrarPorCaracteristicas(p, parsed.getCaracteristicas()))
                .filter(p -> filtrarPorBusqueda(p, parsed.getBusqueda()))
                .filter(p -> filtrarPorIngredientesRequeridos(p, parsed.getIngredientesRequeridos()))
                .filter(p -> filtrarPorIngredientesCualquiera(p, parsed.getIngredientesCualquiera()))
                .filter(p -> filtrarPorIngredientesExcluir(p, parsed.getIngredientesExcluir()))
                .filter(p -> filtrarPorPrecio(p, parsed.getPrecioMaximo()))
                .toList();
    }

    /** Filtra por categoría del plato. Si la categoría es nula o inválida, no descarta el plato. */
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
     * Usa contains en ambas direcciones para tolerar nombres parciales (ej: "pescado" encuentra "con pescado").
     * Requiere @Transactional para acceder a la colección lazy Ingrediente.caracteristicas.
     */
    private boolean filtrarPorCaracteristicas(Plato plato, List<String> caracteristicas) {
        if (caracteristicas == null || caracteristicas.isEmpty()) return true;
        List<String> buscadas = caracteristicas.stream().map(String::toLowerCase).toList();
        return plato.getIngredientes().stream()
                .flatMap(pi -> pi.getIngrediente().getCaracteristicas().stream())
                .map(c -> c.getNombre().toLowerCase())
                .anyMatch(c -> buscadas.stream().anyMatch(b -> c.contains(b) || b.contains(c)));
    }

    /**
     * Filtra platos que contienen TODOS los ingredientes indicados (operador AND).
     * Busca en nombre del ingrediente y en sus características, para que "pescado"
     * encuentre ingredientes como "Mojarra" que tienen la característica "con pescado".
     */
    private boolean filtrarPorIngredientesRequeridos(Plato plato, List<String> requeridos) {
        if (requeridos == null || requeridos.isEmpty()) return true;
        return requeridos.stream()
                .map(String::toLowerCase)
                .allMatch(r -> plato.getIngredientes().stream()
                        .anyMatch(pi -> pi.getIngrediente().getNombre().toLowerCase().contains(r)
                                || pi.getIngrediente().getCaracteristicas().stream()
                                        .anyMatch(c -> c.getNombre().toLowerCase().contains(r))));
    }

    /**
     * Filtra platos que contienen AL MENOS UNO de los ingredientes indicados (operador OR).
     * Busca en nombre del ingrediente y en sus características, para que "pescado"
     * encuentre ingredientes como "Mojarra" que tienen la característica "con pescado".
     */
    private boolean filtrarPorIngredientesCualquiera(Plato plato, List<String> cualquiera) {
        if (cualquiera == null || cualquiera.isEmpty()) return true;
        return cualquiera.stream()
                .map(String::toLowerCase)
                .anyMatch(c -> plato.getIngredientes().stream()
                        .anyMatch(pi -> pi.getIngrediente().getNombre().toLowerCase().contains(c)
                                || pi.getIngrediente().getCaracteristicas().stream()
                                        .anyMatch(car -> car.getNombre().toLowerCase().contains(c))));
    }

    /**
     * Filtra por nombre, descripción, ingredientes o características del plato (case-insensitive).
     * Incluir características permite que "pescado" encuentre platos con ingredientes con característica "con pescado".
     */
    private boolean filtrarPorBusqueda(Plato plato, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) return true;
        String term = busqueda.toLowerCase();
        return plato.getNombre().toLowerCase().contains(term)
                || (plato.getDescripcion() != null && plato.getDescripcion().toLowerCase().contains(term))
                || plato.getIngredientes().stream()
                        .anyMatch(pi -> pi.getIngrediente().getNombre().toLowerCase().contains(term))
                || plato.getIngredientes().stream()
                        .flatMap(pi -> pi.getIngrediente().getCaracteristicas().stream())
                        .anyMatch(c -> c.getNombre().toLowerCase().contains(term));
    }

    /** Filtra platos cuyo precio sea menor o igual al máximo indicado. */
    private boolean filtrarPorPrecio(Plato plato, BigDecimal precioMaximo) {
        if (precioMaximo == null) return true;
        return plato.getPrecio().compareTo(precioMaximo) <= 0;
    }

    /**
     * Excluye platos donde el ingrediente no deseado es OBLIGATORIO.
     * Si el ingrediente es opcional, el plato se mantiene — puede pedirse sin él.
     */
    private boolean filtrarPorIngredientesExcluir(Plato plato, List<String> excluir) {
        if (excluir == null || excluir.isEmpty()) return true;
        return excluir.stream()
                .map(String::toLowerCase)
                .noneMatch(e -> plato.getIngredientes().stream()
                        .anyMatch(pi -> pi.getIngrediente().getNombre().toLowerCase().contains(e)
                                && pi.getObligatorio()));
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

    /** Convierte una entidad Plato al DTO de respuesta con sus ingredientes. */
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
