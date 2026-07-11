package dev.jhuanca.facturacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "factura")
@Data
public class Factura {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String numeroFactura;
    
    @OneToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
    
    private Double total;
    private String serie;
    private Integer correlativo;
    private LocalDateTime fechaEmision;
    
    private String rucEmisor;
    private String razonSocialEmisor;
    
    private String clienteTipoDoc;
    private String clienteNumeroDoc;
    private String clienteNombre;
    private String clienteRazonSocial;
    
    private String sunatRespuesta;
    private String sunatCodigo;
    private String sunatDescripcion;
    private String ticket;
    
    private Boolean enviadoSunat = false;
    private LocalDateTime fechaEnvioSunat;

    @OneToOne(mappedBy = "factura")
    private NotaCredito notaCredito;  // ← AGREGAR
    
    @Column(nullable = false)
    private boolean anulada = false;
}