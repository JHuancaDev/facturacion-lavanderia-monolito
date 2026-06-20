// src/main/java/dev/jhuanca/facturacion/entity/Pedido.java
package dev.jhuanca.facturacion.entity;

import dev.jhuanca.facturacion.enums.EstadoPedido;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_dni")
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    private Double montoTotal = 0.0;

    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaEntrega;
    private String observacionesGenerales;

    @OneToOne(mappedBy = "pedido")
    private Boleta boleta;

    @OneToOne(mappedBy = "pedido")
    private UbicacionRopa ubicacion;
    
    public void calcularTotal() {
        this.montoTotal = detalles.stream()
            .mapToDouble(detalle -> detalle.getSubtotal() != null ? detalle.getSubtotal() : 0.0)
            .sum();
    }
}