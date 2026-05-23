package com.restaurant.order_app.pedido;

import com.restaurant.order_app.item.ItemPedido;
import com.restaurant.order_app.mesa.Mesa;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/** Pedido realizado desde una mesa. Contiene uno o más ítems con sus estados de preparación. */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @Column(nullable = false)
    private String clienteNombre;

    /** UUID generado al momento del login del cliente. Identifica la sesión de forma única. */
    @Column(nullable = false)
    private String clienteSessionId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ItemPedido> items;

    @Column(nullable = false)
    private LocalDateTime creadoEn;
}
