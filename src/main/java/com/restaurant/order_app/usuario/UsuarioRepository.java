package com.restaurant.order_app.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio JPA para la entidad Usuario. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    /** Busca un usuario por su nombre de usuario. Usado en el login del staff. */
    Optional<Usuario> findByUsername(String username);
}
