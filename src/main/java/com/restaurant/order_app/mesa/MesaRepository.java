package com.restaurant.order_app.mesa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio JPA para la entidad Mesa. */
public interface MesaRepository extends JpaRepository<Mesa, Long> {
    /** Busca una mesa por su código QR. Usado en el login del cliente. */
    Optional<Mesa> findByCodigoQr(String codigoQr);
}
