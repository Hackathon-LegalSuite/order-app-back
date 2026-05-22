package com.restaurant.order_app.plato;

import com.restaurant.order_app.ingrediente.Ingrediente;
import jakarta.persistence.*;
import lombok.*;

/**
 * Tabla intermedia entre Plato e Ingrediente.
 * Agrega el campo obligatorio: si es false, el cliente puede excluirlo al pedir.
 */
@Entity
@Table(name = "plato_ingredientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatoIngrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plato_id", nullable = false)
    private Plato plato;

    @ManyToOne
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    /** Si es true, el cliente no puede excluir este ingrediente al hacer el pedido. */
    @Column(nullable = false)
    private Boolean obligatorio;
}
