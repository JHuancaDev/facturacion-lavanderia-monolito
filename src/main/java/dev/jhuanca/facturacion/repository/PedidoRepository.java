package dev.jhuanca.facturacion.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.enums.EstadoPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByEstado(EstadoPedido estado);

    List<Pedido> findAllByOrderByIdDesc();

    List<Pedido> findByClienteDni(String dni);

    List<Pedido> findByFechaRegistroBetween(LocalDateTime inicio, LocalDateTime fin);
}
