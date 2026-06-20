package dev.jhuanca.facturacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.jhuanca.facturacion.entity.UbicacionRopa;

@Repository
public interface UbicacionRopaRepository extends JpaRepository<UbicacionRopa, Long>{
    List<UbicacionRopa> findByRetiradoFalse();
    UbicacionRopa findByPedidoId(Long pedidoId);
}
