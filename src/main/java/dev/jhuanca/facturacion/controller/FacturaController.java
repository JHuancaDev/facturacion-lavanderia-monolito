// src/main/java/dev/jhuanca/facturacion/controller/FacturaController.java
package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.entity.Factura;
import dev.jhuanca.facturacion.repository.FacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/facturas")
public class FacturaController {

    @Autowired
    private FacturaRepository facturaRepository;

    @GetMapping
    public String listarFacturas(Model model) {
        List<Factura> facturas = facturaRepository.findAll();
        model.addAttribute("facturas", facturas);
        model.addAttribute("template", "factura/list");
        return "layout/base";
    }
}