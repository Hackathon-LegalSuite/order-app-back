package com.restaurant.order_app.ingrediente;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio JPA para la entidad Ingrediente. */
public interface IngredienteRepository extends JpaRepository<Ingrediente, Long> {
}
