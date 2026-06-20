package dev.jhuanca.facturacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.jhuanca.facturacion.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, String>{
    
}
