package dev.jhuanca.facturacion.controller;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dev.jhuanca.facturacion.entity.Servicios;
import dev.jhuanca.facturacion.repository.ServiciosRepository;

@Controller
@RequestMapping("/servicios")
public class ServiciosController {
    
    @Autowired
    private ServiciosRepository serviciosRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicios", serviciosRepository.findAll());
        model.addAttribute("template", "services/list");
        return "layout/base";
    }

    
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Servicios servicio, RedirectAttributes redirectAttributes) {
        try {
            serviciosRepository.save(servicio);
            redirectAttributes.addFlashAttribute("success", "Servicio guardado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/servicios";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        serviciosRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Servicio eliminado");
        return "redirect:/servicios";
    }


}
