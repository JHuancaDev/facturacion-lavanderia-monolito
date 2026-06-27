// src/main/java/dev/jhuanca/facturacion/repository/BoletaRepository.java
package dev.jhuanca.facturacion.repository;

import dev.jhuanca.facturacion.entity.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    
    Optional<Boleta> findByPedidoId(Long pedidoId);
    
    @Query("SELECT MAX(b.correlativo) FROM Boleta b")
    Long findMaxCorrelativo();
}