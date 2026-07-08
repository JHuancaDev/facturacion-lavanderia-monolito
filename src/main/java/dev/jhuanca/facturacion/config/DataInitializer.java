package dev.jhuanca.facturacion.config;

import dev.jhuanca.facturacion.entity.User;
import dev.jhuanca.facturacion.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("developer"));
            admin.setNombre("Administrador");
            admin.setEmail("javier9hc@gmail.com");
            admin.setRol("ADMIN");
            admin.setActivo(true);
            admin.setFechaCreacion(LocalDateTime.now());
            userRepository.save(admin);
        }
        
        if (userRepository.findByUsername("trabajador").isEmpty()) {
            User worker = new User();
            worker.setUsername("trabajador");
            worker.setPassword(passwordEncoder.encode("trabajador123"));
            worker.setNombre("Trabajador");
            worker.setEmail("trabajador@lavanderia.com");
            worker.setRol("TRABAJADOR");
            worker.setActivo(true);
            worker.setFechaCreacion(LocalDateTime.now());
            userRepository.save(worker);
        }
    }
}