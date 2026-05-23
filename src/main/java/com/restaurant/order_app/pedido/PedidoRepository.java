package com.restaurant.order_app.pedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositorio JPA para la entidad Pedido. */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Retorna todos los pedidos de una mesa ordenados del más reciente al más antiguo. */
    List<Pedido> findByMesaIdOrderByCreadoEnDesc(Long mesaId);
}
