package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.dto.UserDTO;
import dev.jhuanca.facturacion.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    

    @GetMapping
    public String listUsers(Model model) {
        try {
            List<UserDTO> users = userService.findAll();
            model.addAttribute("users", users);
            model.addAttribute("totalUsers", users.size());
            model.addAttribute("template", "usuarios/list");
            return "layout/base";
        } catch (Exception e) {
            logger.error("Error al listar usuarios: {}", e.getMessage());
            model.addAttribute("error", "Error al cargar la lista de usuarios");
            model.addAttribute("template", "usuarios/list");
            return "layout/base";
        }
    }
    
    @GetMapping("/nuevo")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new UserDTO());
        model.addAttribute("roles", getRoles());
        model.addAttribute("template", "usuarios/form"); 
        return "layout/base";
    }
    
    @PostMapping("/crear")
    public String createUser(@ModelAttribute("user") UserDTO userDTO, 
                            RedirectAttributes redirectAttributes) {
        try {
            if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria");
                return "redirect:/usuarios/nuevo";
            }
            
            userService.createUser(userDTO);
            redirectAttributes.addFlashAttribute("success", 
                "Usuario '" + userDTO.getUsername() + "' creado exitosamente");
        } catch (Exception e) {
            logger.error("Error al crear usuario: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/nuevo";
        }
        return "redirect:/usuarios";
    }
    
    @GetMapping("/editar/{id}")
    public String showEditForm(@PathVariable Long id, Model model, 
                             RedirectAttributes redirectAttributes) {
        try {
            UserDTO user = userService.findById(id)
                .orElseThrow(() -> new Exception("Usuario no encontrado"));
            
            model.addAttribute("user", user);
            model.addAttribute("roles", getRoles());
            model.addAttribute("template", "usuarios/form");
            return "layout/base"; 
        } catch (Exception e) {
            logger.error("Error al cargar usuario para editar: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios";
        }
    }
    
    @PostMapping("/actualizar/{id}")
    public String updateUser(@PathVariable Long id, 
                            @ModelAttribute("user") UserDTO userDTO,
                            RedirectAttributes redirectAttributes) {
        try {
            if (userDTO.getPassword() != null && userDTO.getPassword().isEmpty()) {
                userDTO.setPassword(null);
            }
            
            userService.updateUser(id, userDTO);
            redirectAttributes.addFlashAttribute("success", 
                "Usuario '" + userDTO.getUsername() + "' actualizado exitosamente");
        } catch (Exception e) {
            logger.error("Error al actualizar usuario: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/usuarios/editar/" + id;
        }
        return "redirect:/usuarios";
    }
    
    @PostMapping("/desactivar/{id}")
    public String deactivateUser(@PathVariable Long id, 
                                RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", 
                "Usuario desactivado exitosamente");
        } catch (Exception e) {
            logger.error("Error al desactivar usuario: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
    
    @PostMapping("/activar/{id}")
    public String activateUser(@PathVariable Long id, 
                              RedirectAttributes redirectAttributes) {
        try {
            userService.activateUser(id);
            redirectAttributes.addFlashAttribute("success", 
                "Usuario activado exitosamente");
        } catch (Exception e) {
            logger.error("Error al activar usuario: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
    
    @PostMapping("/cambiar-rol/{id}")
    public String changeRole(@PathVariable Long id, 
                            @RequestParam("rol") String newRole,
                            RedirectAttributes redirectAttributes) {
        try {
            userService.changeRole(id, newRole);
            redirectAttributes.addFlashAttribute("success", 
                "Rol actualizado exitosamente");
        } catch (Exception e) {
            logger.error("Error al cambiar rol: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }
    
    private String[] getRoles() {
        return new String[]{"ADMIN", "TRABAJADOR"};
    }
}