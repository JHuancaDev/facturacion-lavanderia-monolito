// src/main/java/dev/jhuanca/facturacion/controller/PedidoController.java
package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.entity.Cliente;
import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.entity.Servicios;
import dev.jhuanca.facturacion.entity.UbicacionRopa;
import dev.jhuanca.facturacion.enums.EstadoPedido;
import dev.jhuanca.facturacion.repository.ClienteRepository;
import dev.jhuanca.facturacion.repository.DetallePedidoRepository;
import dev.jhuanca.facturacion.repository.PedidoRepository;
import dev.jhuanca.facturacion.repository.ServiciosRepository;
import dev.jhuanca.facturacion.repository.UbicacionRopaRepository;
import dev.jhuanca.facturacion.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private static final String CLIENTE_DEFAULT_DNI = "99999999";

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ServiciosRepository serviciosRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    @Autowired
    private UbicacionRopaRepository ubicacionRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @GetMapping
    public String listarPedidos(Model model) {
        model.addAttribute("pedidos", pedidoRepository.findAllByOrderByIdDesc());
        model.addAttribute("template", "pedido/list");
        return "layout/base";
    }

    @GetMapping("/nuevo")
    public String nuevoPedido(Model model) {
        List<Servicios> servicios = serviciosRepository.findByActivoTrue();

        System.out.println("=== SERVICIOS ENCONTRADOS: " + servicios.size() + " ===");
        for (Servicios s : servicios) {
            System.out.println("ID: " + s.getId() + " - Nombre: " + s.getNameService() + " - Precio: " + s.getPrecio());
        }

        model.addAttribute("servicios", servicios);
        model.addAttribute("pedido", new Pedido());
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("clienteDefaultDni", CLIENTE_DEFAULT_DNI);
        model.addAttribute("template", "pedido/form");
        return "layout/base";
    }

    @PostMapping("/guardar")
    public String guardarPedido(
            @RequestParam(required = false) String clienteDni,
            @RequestParam(required = false) String clienteNombres,
            @RequestParam(required = false) String clienteApellidoPaterno,
            @RequestParam(required = false) String clienteApellidoMaterno,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) List<Long> servicioIds,
            @RequestParam(required = false) List<Double> pesos,
            @RequestParam(required = false) List<String> colores,
            @RequestParam(required = false) List<String> observaciones,
            @RequestParam(required = false) String observacionesGenerales,
            RedirectAttributes redirectAttributes) {

        try {
            System.out.println("=== DATOS RECIBIDOS ===");
            System.out.println("DNI: " + clienteDni);
            System.out.println("Nombres: " + clienteNombres);
            System.out.println("Apellido Paterno: " + clienteApellidoPaterno);
            System.out.println("Apellido Materno: " + clienteApellidoMaterno);
            System.out.println("Teléfono: " + telefono);
            System.out.println("Servicios: " + servicioIds);
            System.out.println("Pesos: " + pesos);

            if (servicioIds == null || servicioIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Debe agregar al menos un servicio");
                return "redirect:/pedidos/nuevo";
            }

            String dni = (clienteDni != null && !clienteDni.isEmpty()) ? clienteDni : CLIENTE_DEFAULT_DNI;

            Cliente cliente = clienteRepository.findById(dni)
                    .orElseGet(() -> {
                        Cliente nuevoCliente = new Cliente();
                        nuevoCliente.setDni(dni);

                        nuevoCliente.setNombres((clienteNombres != null && !clienteNombres.isEmpty())
                                ? clienteNombres
                                : "CLIENTE");
                        nuevoCliente.setApellidoPaterno(
                                (clienteApellidoPaterno != null && !clienteApellidoPaterno.isEmpty())
                                        ? clienteApellidoPaterno
                                        : "GENERAL");
                        nuevoCliente.setApellidoMaterno(
                                (clienteApellidoMaterno != null && !clienteApellidoMaterno.isEmpty())
                                        ? clienteApellidoMaterno
                                        : "");
                        nuevoCliente.setDireccion("Sin dirección");
                        nuevoCliente.setTelefono((telefono != null && !telefono.isEmpty())
                                ? telefono
                                : "999999999");

                        return clienteRepository.save(nuevoCliente);
                    });

            boolean clienteActualizado = false;
            if (clienteNombres != null && !clienteNombres.isEmpty()) {
                cliente.setNombres(clienteNombres);
                clienteActualizado = true;
            }
            if (clienteApellidoPaterno != null && !clienteApellidoPaterno.isEmpty()) {
                cliente.setApellidoPaterno(clienteApellidoPaterno);
                clienteActualizado = true;
            }
            if (clienteApellidoMaterno != null && !clienteApellidoMaterno.isEmpty()) {
                cliente.setApellidoMaterno(clienteApellidoMaterno);
                clienteActualizado = true;
            }
            if (telefono != null && !telefono.isEmpty()) {
                cliente.setTelefono(telefono);
                clienteActualizado = true;
            }

            if (clienteActualizado) {
                clienteRepository.save(cliente);
            }

            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setFechaRegistro(LocalDateTime.now());
            pedido.setEstado(EstadoPedido.REGISTRADO);
            pedido.setObservacionesGenerales(observacionesGenerales);
            pedido.setMontoTotal(0.0);

            pedido = pedidoRepository.save(pedido);

            List<DetallePedido> detalles = new ArrayList<>();
            int maxSize = servicioIds.size();

            for (int i = 0; i < maxSize; i++) {
                Long servicioId = servicioIds.get(i);
                Double peso = (pesos != null && i < pesos.size()) ? pesos.get(i) : null;

                if (servicioId != null && servicioId > 0 && peso != null && peso > 0) {
                    Servicios servicio = serviciosRepository.findById(servicioId).orElse(null);
                    if (servicio != null) {
                        DetallePedido detalle = new DetallePedido();
                        detalle.setPedido(pedido);
                        detalle.setServicio(servicio);
                        detalle.setPeso(peso);
                        detalle.setSubtotal(servicio.getPrecio() * peso);

                        if (colores != null && i < colores.size()) {
                            detalle.setColor(colores.get(i) != null ? colores.get(i) : "");
                        }
                        if (observaciones != null && i < observaciones.size()) {
                            detalle.setObservaciones(observaciones.get(i) != null ? observaciones.get(i) : "");
                        }

                        detalles.add(detalle);
                    }
                }
            }

            if (detalles.isEmpty()) {
                pedidoRepository.delete(pedido);
                redirectAttributes.addFlashAttribute("error", "Debe seleccionar al menos un servicio con peso válido");
                return "redirect:/pedidos/nuevo";
            }

            detallePedidoRepository.saveAll(detalles);

            pedido.getDetalles().clear();
            pedido.getDetalles().addAll(detalles);
            pedido.calcularTotal();
            pedidoRepository.save(pedido);

            redirectAttributes.addFlashAttribute("success", "Pedido registrado con " + detalles.size() + " servicios");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/pedidos";
    }

    @GetMapping("/cambiar-estado-json/{id}")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoJson(@PathVariable Long id,
            @RequestParam EstadoPedido estado) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null) {
                return ResponseEntity.badRequest().body("Pedido no encontrado");
            }
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "El pedido ya fue entregado y no puede modificarse.");
                return ResponseEntity.badRequest().body(response);
            }

            if (pedido.getEstado() == EstadoPedido.FINALIZADO
                    && estado != EstadoPedido.ENTREGADO) {

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("mensaje", "Un pedido finalizado solo puede pasar a ENTREGADO.");
                return ResponseEntity.badRequest().body(response);
            }

            pedido.setEstado(estado);

            if (estado == EstadoPedido.FINALIZADO) {
                pedido.setFechaEntrega(LocalDateTime.now());
                pedidoRepository.save(pedido);
            } else {
                pedidoRepository.save(pedido);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("estado", estado.toString());
            response.put("mensaje", "Estado actualizado a: " + estado);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("mensaje", "Error al actualizar: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/whatsapp-link")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generarWhatsAppLink(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null) {
                return ResponseEntity.badRequest().build();
            }

            Cliente cliente = pedido.getCliente();
            String telefono = cliente.getTelefono();

            if (telefono == null || telefono.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El cliente no tiene número de teléfono registrado");
                return ResponseEntity.badRequest().body(error);
            }

            String mensaje = String.format(
                    "🏪 *LAVANDERÍA* 🏪\n\n" +
                            "✅ *SU PEDIDO ESTÁ LISTO* ✅\n\n" +
                            "📋 *N° Pedido:* %d\n" +
                            "👤 *Cliente:* %s %s\n" +
                            "💰 *Total:* S/ %.2f\n" +
                            "📍 *Ubicación:* %s\n\n" +
                            "🎯 *Pase a recogerlo a nuestro local* 🎯\n\n" +
                            "¡Gracias por confiar en nosotros! 🙏",
                    pedido.getId(),
                    cliente.getNombres(),
                    cliente.getApellidoPaterno(),
                    pedido.getMontoTotal(),
                    pedido.getUbicacion() != null ? pedido.getUbicacion().getCodigoUbicacion() : "Pendiente");

            String mensajeCodificado = java.net.URLEncoder.encode(mensaje, "UTF-8");

            String whatsappLink = "https://wa.me/51" + telefono + "?text=" + mensajeCodificado;

            Map<String, String> response = new HashMap<>();
            response.put("link", whatsappLink);
            response.put("telefono", telefono);
            response.put("mensaje", mensaje);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al generar enlace: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getBoleta() != null) {
            System.out.println("Boleta encontrada: " + pedido.getBoleta().getNumeroBoleta());
        } else {
            System.out.println("No hay boleta para este pedido");
        }

        System.out.println("Estado del pedido: " + pedido.getEstado());

        model.addAttribute("pedido", pedido);
        model.addAttribute("template", "pedido/detalle");
        return "layout/base";
    }

    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id,
            @RequestParam EstadoPedido estado,
            RedirectAttributes redirectAttributes) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido != null) {
            pedido.setEstado(estado);

            if (estado == EstadoPedido.FINALIZADO) {
                pedido.setFechaEntrega(LocalDateTime.now());
                pedidoRepository.save(pedido);
                whatsAppService.enviarNotificacionPedidoListo(pedido);
                redirectAttributes.addFlashAttribute("success", "Pedido finalizado y notificación enviada");
            } else {
                pedidoRepository.save(pedido);
                redirectAttributes.addFlashAttribute("success", "Estado actualizado a: " + estado);
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
        }
        return "redirect:/pedidos";
    }

    @GetMapping("/asignar-ubicacion/{id}")
    public String asignarUbicacion(@PathVariable Long id, Model model) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            return "redirect:/pedidos";
        }
        model.addAttribute("pedido", pedido);
        model.addAttribute("template", "pedido/ubicacion");
        return "layout/base";
    }

    @PostMapping("/guardar-ubicacion")
    public String guardarUbicacion(@RequestParam Long pedidoId,
            @RequestParam String codigoUbicacion,
            @RequestParam String estante,
            @RequestParam String balda,
            RedirectAttributes redirectAttributes) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido != null) {
            UbicacionRopa ubicacion = new UbicacionRopa();
            ubicacion.setPedido(pedido);
            ubicacion.setCliente(pedido.getCliente());
            ubicacion.setCodigoUbicacion(codigoUbicacion);
            ubicacion.setEstante(estante);
            ubicacion.setBalda(balda);
            ubicacion.setFechaAsignacion(LocalDateTime.now());
            ubicacion.setRetirado(false);

            ubicacionRepository.save(ubicacion);
            redirectAttributes.addFlashAttribute("success", "Ubicación asignada: " + codigoUbicacion);
        } else {
            redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/{id}/whatsapp")
    public String enviarWhatsApp(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido != null && pedido.getEstado() == EstadoPedido.FINALIZADO) {
            whatsAppService.enviarNotificacionPedidoListo(pedido);
            redirectAttributes.addFlashAttribute("success", "Notificación WhatsApp enviada");
        } else {
            redirectAttributes.addFlashAttribute("error", "No se puede enviar notificación");
        }
        return "redirect:/pedidos";
    }

}