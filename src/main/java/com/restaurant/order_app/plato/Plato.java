package com.restaurant.order_app.plato;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Plato del menú. Tiene una categoría, precio y lista de ingredientes con su carácter obligatorio. */
@Entity
@Table(name = "platos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    /** Solo aplica para BEBIDA: true = elaborada en cocina, false = embotellada. */
    private Boolean preparada;

    /** URL de la imagen del plato para mostrar en el menú. */
    private String imagenUrl;

    @OneToMany(mappedBy = "plato", fetch = FetchType.EAGER)
    private List<PlatoIngrediente> ingredientes;
}
