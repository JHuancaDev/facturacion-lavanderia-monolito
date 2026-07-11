// src/main/java/dev/jhuanca/facturacion/entity/NotaCredito.java
package dev.jhuanca.facturacion.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notas_credito")
@Data
public class NotaCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "boleta_id", nullable = false)
    private Boleta boleta;

    @OneToOne
    @JoinColumn(name = "factura_id")
    private Factura factura;
    
    @Column(nullable = false, unique = true)
    private String numeroNotaCredito;
    
    @Column(nullable = false)
    private String serie;
    
    @Column(nullable = false)
    private Integer correlativo;
    
    @Column(nullable = false)
    private String codigoMotivo;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;
    
    @Column(nullable = false)
    private BigDecimal total;
    
    @Column(nullable = false)
    private LocalDateTime fechaEmision;
    
    private LocalDateTime fechaEnvioSunat;
    
    private String ticket;
    
    @Column(columnDefinition = "TEXT")
    private String sunatRespuesta;
    
    @Column(nullable = false)
    private boolean enviadoSunat = false;
}