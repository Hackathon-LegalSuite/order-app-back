package com.restaurant.order_app.plato;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Repositorio JPA para la entidad Plato. */
public interface PlatoRepository extends JpaRepository<Plato, Long> {

    /** Retorna todos los platos que pertenezcan a la categoría indicada. */
    List<Plato> findByCategoria(Categoria categoria);

    /**
     * Carga todos los platos con sus ingredientes en una sola query.
     * Las características de cada ingrediente se cargan en batch (@BatchSize en Ingrediente)
     * para evitar el N+1 sin caer en MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT p FROM Plato p " +
           "LEFT JOIN FETCH p.ingredientes pi " +
           "LEFT JOIN FETCH pi.ingrediente")
    List<Plato> findAllConCaracteristicas();
}
