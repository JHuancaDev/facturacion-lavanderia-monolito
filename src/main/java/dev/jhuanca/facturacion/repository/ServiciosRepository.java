package dev.jhuanca.facturacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.jhuanca.facturacion.entity.Servicios;

@Repository
public interface ServiciosRepository extends JpaRepository<Servicios, Long> {
        List<Servicios> findByActivoTrue();
}
