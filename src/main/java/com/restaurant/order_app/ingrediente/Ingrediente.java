package com.restaurant.order_app.ingrediente;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/** Ingrediente individual del restaurante. Puede pertenecer a varios platos. */
@Entity
@Table(name = "ingredientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    /** Características sensoriales del ingrediente. Relación ManyToMany — un ingrediente puede tener varias. */
    @ManyToMany
    @JoinTable(
            name = "ingrediente_caracteristicas",
            joinColumns = @JoinColumn(name = "ingrediente_id"),
            inverseJoinColumns = @JoinColumn(name = "caracteristica_id")
    )
    private List<Caracteristica> caracteristicas;
}
