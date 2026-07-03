package dev.jhuanca.facturacion.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.jhuanca.facturacion.entity.User;
import dev.jhuanca.facturacion.repository.UserRepository;

@Service
public class PasswordResetService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.base.url}")
    private String baseUrl;
    
    public void sendResetPasswordEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new Exception("Usuario no encontrado"));
        
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        
        String resetLink = baseUrl + "/reset-password?token=" + token;
        
        // Enviar email
        sendEmail(user.getEmail(), "Recuperación de Contraseña", 
                 "Haz clic en el siguiente enlace para restablecer tu contraseña: " + resetLink);
    }
    
    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
    
    public boolean validateResetToken(String token) {
        User user = userRepository.findByResetToken(token);
        if (user == null) {
            return false;
        }
        return user.getResetTokenExpiry().isAfter(LocalDateTime.now());
    }
    
    public void updatePassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token);
        if (user != null && user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
        }
    }
}
