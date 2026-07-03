package dev.jhuanca.facturacion.service;

import dev.jhuanca.facturacion.dto.UserDTO;
import dev.jhuanca.facturacion.entity.User;
import dev.jhuanca.facturacion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNombre(user.getNombre());
        dto.setEmail(user.getEmail());
        dto.setRol(user.getRol());
        dto.setActivo(user.getActivo());
        return dto;
    }
    
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id)
            .map(this::convertToDTO);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Transactional
    public UserDTO createUser(UserDTO userDTO) throws Exception {
        
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new Exception("El nombre de usuario '" + userDTO.getUsername() + "' ya existe");
        }
        
        
        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty()) {
            if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
                throw new Exception("El email '" + userDTO.getEmail() + "' ya está registrado");
            }
        }
   
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setNombre(userDTO.getNombre());
        user.setEmail(userDTO.getEmail());
        user.setRol(userDTO.getRol() != null ? userDTO.getRol() : "TRABAJADOR");
        user.setActivo(userDTO.getActivo() != null ? userDTO.getActivo() : true);
        user.setFechaCreacion(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        logger.info("Usuario creado: {} con rol {}", savedUser.getUsername(), savedUser.getRol());
        
        return convertToDTO(savedUser);
    }
    
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) throws Exception {
        User existingUser = userRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado con ID: " + id));
        
        if (!existingUser.getUsername().equals(userDTO.getUsername())) {
            if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
                throw new Exception("El nombre de usuario '" + userDTO.getUsername() + "' ya existe");
            }
            existingUser.setUsername(userDTO.getUsername());
        }
        
        if (userDTO.getEmail() != null && !userDTO.getEmail().isEmpty()) {
            Optional<User> userWithEmail = userRepository.findByEmail(userDTO.getEmail());
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
                throw new Exception("El email '" + userDTO.getEmail() + "' ya está registrado");
            }
            existingUser.setEmail(userDTO.getEmail());
        }
        

        existingUser.setNombre(userDTO.getNombre());
        if (userDTO.getRol() != null) {
            existingUser.setRol(userDTO.getRol());
        }
        if (userDTO.getActivo() != null) {
            existingUser.setActivo(userDTO.getActivo());
        }
        
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        
        User updatedUser = userRepository.save(existingUser);
        logger.info("Usuario actualizado: {}", updatedUser.getUsername());
        
        return convertToDTO(updatedUser);
    }
    
    @Transactional
    public void deleteUser(Long id) throws Exception {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado con ID: " + id));
        
        user.setActivo(false);
        userRepository.save(user);
        logger.info("Usuario desactivado: {}", user.getUsername());
    }
    
    @Transactional
    public void hardDeleteUser(Long id) throws Exception {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado con ID: " + id));
        
        userRepository.delete(user);
        logger.info("Usuario eliminado permanentemente: {}", user.getUsername());
    }
    
    @Transactional
    public void activateUser(Long id) throws Exception {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado con ID: " + id));
        
        user.setActivo(true);
        userRepository.save(user);
        logger.info("Usuario activado: {}", user.getUsername());
    }
    
    @Transactional
    public void changeRole(Long id, String newRole) throws Exception {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new Exception("Usuario no encontrado con ID: " + id));
        
        user.setRol(newRole);
        userRepository.save(user);
        logger.info("Rol de usuario {} cambiado a {}", user.getUsername(), newRole);
    }
    
    public long countUsers() {
        return userRepository.count();
    }
}