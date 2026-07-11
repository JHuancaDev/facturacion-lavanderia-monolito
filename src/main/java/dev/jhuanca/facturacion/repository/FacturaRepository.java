package dev.jhuanca.facturacion.repository;

import dev.jhuanca.facturacion.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Long> {
    Optional<Factura> findByPedidoId(Long pedidoId);
    
    @Query("SELECT MAX(f.correlativo) FROM Factura f WHERE f.serie = :serie")
    Long findMaxCorrelativoBySerie(String serie);
}
