package com.restaurant.order_app.ingrediente;

import jakarta.persistence.*;
import lombok.*;

/** Característica sensorial de un ingrediente (ej: picante, dulce). Se gestiona desde BD, no desde código. */
@Entity
@Table(name = "caracteristicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;
}
