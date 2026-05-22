package com.restaurant.order_app.mesa;

import com.restaurant.order_app.usuario.Usuario;
import jakarta.persistence.*;
import lombok.*;

/** Representa una mesa del restaurante. Cada mesa tiene un código QR único y un mesero asignado. */
@Entity
@Table(name = "mesas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, unique = true)
    private String codigoQr;

    @ManyToOne
    @JoinColumn(name = "mesero_id")
    private Usuario mesero;
}
