package dev.jhuanca.facturacion.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dev.jhuanca.facturacion.enums.EstadoPedido;
import dev.jhuanca.facturacion.repository.PedidoRepository;

@Controller
public class DashboardController {
    @Autowired
    private PedidoRepository pedidoRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalPedidos", pedidoRepository.count());
        model.addAttribute("pedidosRegistrados", pedidoRepository.findByEstado(EstadoPedido.REGISTRADO).size());
        model.addAttribute("pedidosProceso", pedidoRepository.findByEstado(EstadoPedido.EN_PROCESO).size());
        model.addAttribute("pedidosFinalizados", pedidoRepository.findByEstado(EstadoPedido.FINALIZADO).size());
        model.addAttribute("template", "dashboard/index");
        return "layout/base";
    }
}
