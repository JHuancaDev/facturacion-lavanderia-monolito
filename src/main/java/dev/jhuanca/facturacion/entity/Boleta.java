/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dev.jhuanca.facturacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author javier
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "boleta")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroBoleta;   // formato B001-000001

    @OneToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    private Double total;
    private String serie;
    private Integer correlativo;
    private LocalDateTime fechaEmision;

    // Datos adicionales para SUNAT
    private String rucLavanderia;
    private String razonSocial;
}
