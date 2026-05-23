package com.restaurant.order_app.item;

import com.restaurant.order_app.pedido.Pedido;
import com.restaurant.order_app.plato.Plato;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/** Ítem de un pedido: un plato con ingredientes excluidos opcionales y un estado de preparación. */
@Entity
@Table(name = "items_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "plato_id", nullable = false)
    private Plato plato;

    /** IDs de ingredientes que el cliente solicita excluir. Solo pueden ser ingredientes con obligatorio=false. */
    @ElementCollection
    @CollectionTable(name = "item_ingredientes_excluidos", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "ingrediente_id")
    private List<Long> ingredientesExcluidos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoItem estado;
}
