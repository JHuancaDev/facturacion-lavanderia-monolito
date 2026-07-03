package dev.jhuanca.facturacion.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.jhuanca.facturacion.service.PasswordResetService;

@Controller
public class PasswordResetController {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
    
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        logger.info("Accediendo a página de recuperación de contraseña");
        return "auth/forgot-password";
    }
    
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, 
                                       RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.sendResetPasswordEmail(email);
            redirectAttributes.addFlashAttribute("message", 
                "Se ha enviado un enlace de recuperación a tu correo electrónico.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Error al enviar el correo de recuperación.");
        }
        return "redirect:/forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, 
                                       Model model) {
        if (!passwordResetService.validateResetToken(token)) {
            model.addAttribute("error", "El enlace de recuperación ha expirado o es inválido.");
            return "error/404";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }
    
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
        
        try {
            passwordResetService.updatePassword(token, password);
            model.addAttribute("message", "Contraseña actualizada correctamente.");
            return "auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar la contraseña.");
            return "auth/reset-password";
        }
    }
}