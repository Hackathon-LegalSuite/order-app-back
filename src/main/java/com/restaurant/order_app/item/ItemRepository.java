package com.restaurant.order_app.item;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio JPA para la entidad ItemPedido. */
public interface ItemRepository extends JpaRepository<ItemPedido, Long> {
}
