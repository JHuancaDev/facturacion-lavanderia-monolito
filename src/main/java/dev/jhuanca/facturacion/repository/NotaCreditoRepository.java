package dev.jhuanca.facturacion.repository;

import dev.jhuanca.facturacion.entity.NotaCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Long> {
    Optional<NotaCredito> findByBoletaId(Long boletaId);

    @Query("SELECT MAX(n.correlativo) FROM NotaCredito n WHERE n.serie = :serie")
    Long findMaxCorrelativoBySerie(String serie);
}