package dev.jhuanca.facturacion.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ubicacion_ropa")
public class UbicacionRopa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "cliente_dni")
    private Cliente cliente;

    private String codigoUbicacion;
    private String estante;
    private String balda;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaRetiro;
    private Boolean retirado = false;

    @Column(length = 500)
    private String observaciones;
}
