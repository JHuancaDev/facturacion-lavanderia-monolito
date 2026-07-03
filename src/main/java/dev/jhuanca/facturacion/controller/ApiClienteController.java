package dev.jhuanca.facturacion.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.jhuanca.facturacion.entity.Cliente;
import dev.jhuanca.facturacion.repository.ClienteRepository;

@RestController
@RequestMapping("/api/clientes")
public class ApiClienteController {
    
    @Autowired
    private ClienteRepository clienteRepository;
    
        @GetMapping("/{dni}")
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable String dni) {
        return clienteRepository.findById(dni)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/guardar")
    public ResponseEntity<Cliente> guardarCliente(@RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteRepository.save(cliente));
    }
}