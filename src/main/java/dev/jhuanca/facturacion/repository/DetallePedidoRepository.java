// src/main/java/dev/jhuanca/facturacion/repository/DetallePedidoRepository.java
package dev.jhuanca.facturacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import dev.jhuanca.facturacion.entity.DetallePedido;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
}