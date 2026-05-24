package com.restaurant.order_app.pedido;

import com.restaurant.order_app.item.EstadoItem;
import com.restaurant.order_app.item.ItemPedido;
import com.restaurant.order_app.item.ItemRepository;
import com.restaurant.order_app.mesa.Mesa;
import com.restaurant.order_app.mesa.MesaRepository;
import com.restaurant.order_app.pedido.dto.ConsultaItemResponse;
import com.restaurant.order_app.pedido.dto.CrearPedidoRequest;
import com.restaurant.order_app.pedido.dto.ItemRequest;
import com.restaurant.order_app.pedido.dto.ItemResponse;
import com.restaurant.order_app.pedido.dto.PedidoResponse;
import com.restaurant.order_app.plato.Plato;
import com.restaurant.order_app.plato.PlatoRepository;
import com.restaurant.order_app.plato.dto.IngredienteEnPlatoResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Lógica de negocio para la creación y consulta de pedidos. */
@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final List<EstadoItem> SECUENCIA_ESTADOS = List.of(
            EstadoItem.EN_ESPERA, EstadoItem.EN_PROGRESO, EstadoItem.LISTO, EstadoItem.ENTREGADO
    );

    private final PedidoRepository pedidoRepository;
    private final ItemRepository itemRepository;
    private final MesaRepository mesaRepository;
    private final PlatoRepository platoRepository;

    /**
     * Crea un pedido extrayendo mesa y nombre del cliente del JWT.
     * Valida que los ingredientes excluidos sean solo opcionales.
     */
    public PedidoResponse crearPedido(CrearPedidoRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long mesaId = getMesaIdFromToken(auth);
        String clienteNombre = auth.getName();
        String clienteSessionId = (String) ((Claims) auth.getDetails()).get("clienteId");

        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada"));

        List<ItemPedido> items = request.getItems().stream()
                .map(itemReq -> buildItem(itemReq))
                .toList();

        Pedido pedido = Pedido.builder()
                .mesa(mesa)
                .clienteNombre(clienteNombre)
                .clienteSessionId(clienteSessionId)
                .creadoEn(LocalDateTime.now())
                .items(items)
                .build();

        items.forEach(item -> item.setPedido(pedido));
        return toResponse(pedidoRepository.save(pedido));
    }

    /**
     * Retorna ítems filtrados según el rol del token:
     * - CLIENTE: todos sus ítems (por sessionId), sin mesa ni mesero, sin filtro de estado.
     * - COCINERO: todos los ítems en EN_ESPERA o EN_PROGRESO de todas las mesas.
     * - MESERO: solo ítems en LISTO de las mesas que tiene asignadas.
     */
    public List<ConsultaItemResponse> listar() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims claims = (Claims) auth.getDetails();
        String rol = (String) claims.get("rol");

        Sort porFecha = Sort.by(Sort.Direction.DESC, "creadoEn");

        if ("CLIENTE".equals(rol)) {
            String sessionId = (String) claims.get("clienteId");
            return pedidoRepository.findByClienteSessionId(sessionId, porFecha).stream()
                    .flatMap(p -> p.getItems().stream())
                    .map(item -> toConsultaResponse(item, false))
                    .toList();
        }

        if ("COCINERO".equals(rol)) {
            return pedidoRepository.findAll(porFecha).stream()
                    .flatMap(p -> p.getItems().stream())
                    .filter(item -> item.getEstado() == EstadoItem.EN_ESPERA
                                 || item.getEstado() == EstadoItem.EN_PROGRESO)
                    .map(item -> toConsultaResponse(item, true))
                    .toList();
        }

        // MESERO: solo ítems LISTO de sus mesas asignadas
        String username = auth.getName();
        return pedidoRepository.findAll(porFecha).stream()
                .filter(p -> p.getMesa().getMesero().getUsername().equals(username))
                .flatMap(p -> p.getItems().stream())
                .filter(item -> item.getEstado() == EstadoItem.LISTO)
                .map(item -> toConsultaResponse(item, true))
                .toList();
    }

    /**
     * Elimina un ítem de un pedido. Si era el último ítem, elimina el pedido también.
     * Valida que el clienteSessionId del token coincida con el del pedido.
     */
    public String eliminarItem(Long pedidoId, Long itemId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Claims claims = (Claims) auth.getDetails();
        String sessionId = (String) claims.get("clienteId");

        if (sessionId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este endpoint es solo para clientes");
        }

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        if (!pedido.getClienteSessionId().equals(sessionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pedido no encontrado");
        }

        ItemPedido item = pedido.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El ítem no pertenece a este pedido"));

        if (item.getEstado() != EstadoItem.EN_ESPERA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede eliminar un ítem que ya está en estado " + item.getEstado());
        }

        pedido.getItems().removeIf(i -> i.getId().equals(itemId));

        if (pedido.getItems().isEmpty()) {
            pedidoRepository.delete(pedido);
            return "Ítem eliminado. El pedido fue eliminado por no tener más ítems";
        }

        pedidoRepository.save(pedido);
        return "Ítem eliminado correctamente";
    }

    /** Avanza el estado del ítem al siguiente en la secuencia. Lanza 400 si ya está en ENTREGADO. */
    public String avanzarEstado(Long itemId) {
        ItemPedido item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem no encontrado"));

        int indiceActual = SECUENCIA_ESTADOS.indexOf(item.getEstado());

        if (indiceActual == SECUENCIA_ESTADOS.size() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ítem ya está en el estado final: ENTREGADO");
        }

        EstadoItem nuevoEstado = SECUENCIA_ESTADOS.get(indiceActual + 1);
        item.setEstado(nuevoEstado);
        itemRepository.save(item);

        return "Estado actualizado a " + nuevoEstado;
    }

    /** Construye un ItemPedido validando que los ingredientes excluidos sean opcionales en ese plato. */
    private ItemPedido buildItem(ItemRequest req) {
        Plato plato = platoRepository.findById(req.getPlatoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plato no encontrado: " + req.getPlatoId()));

        List<Long> excluidos = req.getIngredientesExcluidos() != null ? req.getIngredientesExcluidos() : List.of();

        if (!excluidos.isEmpty()) {
            Set<Long> optativos = plato.getIngredientes().stream()
                    .filter(pi -> !pi.getObligatorio())
                    .map(pi -> pi.getIngrediente().getId())
                    .collect(Collectors.toSet());

            excluidos.forEach(ingId -> {
                if (!optativos.contains(ingId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El ingrediente " + ingId + " es obligatorio o no pertenece al plato");
                }
            });
        }

        return ItemPedido.builder()
                .plato(plato)
                .ingredientesExcluidos(excluidos)
                .estado(EstadoItem.EN_ESPERA)
                .build();
    }

    private PedidoResponse toResponse(Pedido pedido) {
        List<ItemResponse> items = pedido.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return PedidoResponse.builder()
                .id(pedido.getId())
                .mesaId(pedido.getMesa().getId())
                .clienteNombre(pedido.getClienteNombre())
                .clienteSessionId(pedido.getClienteSessionId())
                .creadoEn(pedido.getCreadoEn())
                .items(items)
                .build();
    }

    private ItemResponse toItemResponse(ItemPedido item) {
        Map<Long, String> nombrePorId = item.getPlato().getIngredientes().stream()
                .collect(Collectors.toMap(
                        pi -> pi.getIngrediente().getId(),
                        pi -> pi.getIngrediente().getNombre()
                ));

        List<String> excluidos = item.getIngredientesExcluidos().stream()
                .map(id -> nombrePorId.getOrDefault(id, "Ingrediente " + id))
                .toList();

        return ItemResponse.builder()
                .id(item.getId())
                .platoId(item.getPlato().getId())
                .platoNombre(item.getPlato().getNombre())
                .ingredientesExcluidos(excluidos)
                .estado(item.getEstado())
                .build();
    }

    private ConsultaItemResponse toConsultaResponse(ItemPedido item, boolean incluirMesaYMesero) {
        Map<Long, String> nombrePorId = item.getPlato().getIngredientes().stream()
                .collect(Collectors.toMap(
                        pi -> pi.getIngrediente().getId(),
                        pi -> pi.getIngrediente().getNombre()
                ));

        List<String> excluidos = item.getIngredientesExcluidos().stream()
                .map(id -> nombrePorId.getOrDefault(id, "Ingrediente " + id))
                .toList();

        List<IngredienteEnPlatoResponse> ingredientes = item.getPlato().getIngredientes().stream()
                .map(pi -> IngredienteEnPlatoResponse.builder()
                        .id(pi.getIngrediente().getId())
                        .nombre(pi.getIngrediente().getNombre())
                        .obligatorio(pi.getObligatorio())
                        .build())
                .toList();

        ConsultaItemResponse.ConsultaItemResponseBuilder builder = ConsultaItemResponse.builder()
                .pedidoId(item.getPedido().getId())
                .itemId(item.getId())
                .platoId(item.getPlato().getId())
                .platoNombre(item.getPlato().getNombre())
                .precio(item.getPlato().getPrecio())
                .imagenUrl(item.getPlato().getImagenUrl())
                .ingredientes(ingredientes)
                .ingredientesExcluidos(excluidos)
                .estado(item.getEstado());

        if (incluirMesaYMesero) {
            builder.mesa(item.getPedido().getMesa().getNumero())
                   .mesero(item.getPedido().getMesa().getMesero().getNombre());
        }

        return builder.build();
    }

    /** Extrae el mesaId del JWT. Lanza 403 si el token no pertenece a un cliente (no tiene mesaId). */
    private Long getMesaIdFromToken(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Number mesaId = (Number) claims.get("mesaId");
        if (mesaId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este endpoint es solo para clientes");
        }
        // JJWT deserializa números como Integer si el valor cabe; usamos Number para compatibilidad.
        return mesaId.longValue();
    }
}
