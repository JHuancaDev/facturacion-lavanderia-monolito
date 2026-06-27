// src/main/java/dev/jhuanca/facturacion/controller/BoletaController.java
package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.entity.Boleta;
import dev.jhuanca.facturacion.repository.BoletaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/boletas")
public class BoletaController {

    @Autowired
    private BoletaRepository boletaRepository;

    @GetMapping
    public String listarBoletas(Model model) {
        List<Boleta> boletas = boletaRepository.findAll();
        model.addAttribute("boletas", boletas);
        model.addAttribute("template", "boleta/list");
        return "layout/base";
    }
}