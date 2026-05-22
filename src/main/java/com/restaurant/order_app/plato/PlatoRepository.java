package com.restaurant.order_app.plato;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Repositorio JPA para la entidad Plato. */
public interface PlatoRepository extends JpaRepository<Plato, Long> {

    /** Retorna todos los platos que pertenezcan a la categoría indicada. */
    List<Plato> findByCategoria(Categoria categoria);
}
