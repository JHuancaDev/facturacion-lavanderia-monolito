// src/main/java/dev/jhuanca/facturacion/controller/PedidoController.java
package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.entity.Boleta;
import dev.jhuanca.facturacion.entity.Cliente;
import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.entity.Servicios;
import dev.jhuanca.facturacion.entity.UbicacionRopa;
import dev.jhuanca.facturacion.enums.EstadoPedido;
import dev.jhuanca.facturacion.repository.BoletaRepository;
import dev.jhuanca.facturacion.repository.ClienteRepository;
import dev.jhuanca.facturacion.repository.DetallePedidoRepository;
import dev.jhuanca.facturacion.repository.PedidoRepository;
import dev.jhuanca.facturacion.repository.ServiciosRepository;
import dev.jhuanca.facturacion.repository.UbicacionRopaRepository;
import dev.jhuanca.facturacion.service.SunatService;
import dev.jhuanca.facturacion.service.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private SunatService sunatService;

    @GetMapping
    public String listarPedidos(Model model) {
        model.addAttribute("pedidos", pedidoRepository.findAll());
        model.addAttribute("template", "pedido/list");
        return "layout/base";
    }

    @GetMapping("/nuevo")
    public String nuevoPedido(Model model) {
        List<Servicios> servicios = serviciosRepository.findByActivoTrue();

        // ⚠️ LOG PARA DEPURAR
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

    // src/main/java/dev/jhuanca/facturacion/controller/PedidoController.java

    // src/main/java/dev/jhuanca/facturacion/controller/PedidoController.java

    @GetMapping("/{id}/descargar-boleta")
    public ResponseEntity<byte[]> descargarBoleta(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null || pedido.getBoleta() == null) {
                return ResponseEntity.notFound().build();
            }

            // Generar contenido de la boleta
            StringBuilder contenido = new StringBuilder();
            contenido.append("========================================\n");
            contenido.append("         BOLETA DE VENTA              \n");
            contenido.append("========================================\n");
            contenido.append("Número: ").append(pedido.getBoleta().getNumeroBoleta()).append("\n");
            contenido.append("Fecha: ").append(pedido.getBoleta().getFechaEmision()).append("\n");
            contenido.append("----------------------------------------\n");
            contenido.append("Cliente: ").append(pedido.getCliente().getNombres()).append(" ");
            contenido.append(pedido.getCliente().getApellidoPaterno()).append("\n");
            contenido.append("DNI: ").append(pedido.getCliente().getDni()).append("\n");
            contenido.append("Teléfono: ").append(pedido.getCliente().getTelefono()).append("\n");
            contenido.append("----------------------------------------\n");
            contenido.append("DETALLE:\n");

            for (DetallePedido detalle : pedido.getDetalles()) {
                contenido.append("  - ").append(detalle.getServicio().getNameService());
                contenido.append(" (").append(detalle.getPeso()).append(" kg)");
                contenido.append(" S/ ").append(detalle.getSubtotal()).append("\n");
            }

            contenido.append("----------------------------------------\n");
            contenido.append("TOTAL: S/ ").append(pedido.getMontoTotal()).append("\n");
            contenido.append("========================================\n");
            contenido.append("¡Gracias por su preferencia!\n");

            byte[] contenidoBytes = contenido.toString().getBytes("UTF-8");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment",
                    "boleta_" + pedido.getBoleta().getNumeroBoleta() + ".txt");

            return new ResponseEntity<>(contenidoBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/emitir-boleta")
public String emitirBoleta(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            redirectAttributes.addFlashAttribute("error", "Pedido no encontrado");
            return "redirect:/pedidos";
        }

        // Verificar si ya tiene boleta
        if (boletaRepository.findByPedidoId(id).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Este pedido ya tiene boleta emitida");
            return "redirect:/pedidos/detalle/" + id;
        }

        // 1. Generar número de boleta
        String numeroBoleta = sunatService.generarNumeroBoleta();
        System.out.println("📄 Número de Boleta: " + numeroBoleta);

        // 2. Generar XML, firmar y enviar a SUNAT (todo en uno)
        String ticket = sunatService.generarYEnviarBoleta(pedido, numeroBoleta);
        System.out.println("✅ Ticket recibido: " + ticket);

        // 3. Crear y guardar boleta en BD
        Boleta boleta = new Boleta();
        boleta.setPedido(pedido);
        boleta.setNumeroBoleta(numeroBoleta);
        boleta.setTotal(pedido.getMontoTotal());
        boleta.setSerie("B001");
        
        // Extraer correlativo del número
        String[] partes = numeroBoleta.split("-");
        if (partes.length == 2) {
            boleta.setCorrelativo(Integer.parseInt(partes[1]));
        }
        
        // Datos del emisor (coinciden con application.properties)
        boleta.setRucEmisor("10771318199");
        boleta.setRazonSocialEmisor("LAVANDERIA S.A.C.");
        
        // Datos del cliente
        boleta.setClienteTipoDoc("DNI");
        boleta.setClienteNumeroDoc(pedido.getCliente().getDni());
        boleta.setClienteNombre(pedido.getCliente().getNombres() + " " + pedido.getCliente().getApellidoPaterno());
        
        // Datos de SUNAT
        boleta.setEnviadoSunat(true);
        boleta.setFechaEmision(LocalDateTime.now());
        boleta.setFechaEnvioSunat(LocalDateTime.now());
        boleta.setTicket(ticket);
        boleta.setSunatRespuesta("{\"success\": true, \"ticket\": \"" + ticket + "\"}");

        // Guardar
        boletaRepository.save(boleta);
        
        // Actualizar pedido
        pedido.setBoleta(boleta);
        pedidoRepository.save(pedido);

        redirectAttributes.addFlashAttribute("success", "Boleta " + numeroBoleta + " emitida correctamente. Ticket: " + ticket);

    } catch (Exception e) {
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "Error al emitir boleta: " + e.getMessage());
    }

    return "redirect:/pedidos/detalle/" + id;
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

            // Validar que haya servicios
            if (servicioIds == null || servicioIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Debe agregar al menos un servicio");
                return "redirect:/pedidos/nuevo";
            }

            // Determinar el DNI
            String dni = (clienteDni != null && !clienteDni.isEmpty()) ? clienteDni : CLIENTE_DEFAULT_DNI;

            // Buscar o crear cliente
            Cliente cliente = clienteRepository.findById(dni)
                    .orElseGet(() -> {
                        Cliente nuevoCliente = new Cliente();
                        nuevoCliente.setDni(dni);

                        // Usar los valores proporcionados o valores por defecto
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

            // Si el cliente ya existe, actualizar los datos si se proporcionaron
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

            // Crear pedido
            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setFechaRegistro(LocalDateTime.now());
            pedido.setEstado(EstadoPedido.REGISTRADO);
            pedido.setObservacionesGenerales(observacionesGenerales);
            pedido.setMontoTotal(0.0);

            // Guardar pedido primero
            pedido = pedidoRepository.save(pedido);

            // Crear detalles
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

            // Guardar detalles
            detallePedidoRepository.saveAll(detalles);

            // Actualizar pedido
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

            pedido.setEstado(estado);

            if (estado == EstadoPedido.FINALIZADO) {
                pedido.setFechaEntrega(LocalDateTime.now());
                pedidoRepository.save(pedido);
                whatsAppService.enviarNotificacionPedidoListo(pedido);
            } else {
                pedidoRepository.save(pedido);
            }

            // Devolver respuesta JSON
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

    // src/main/java/dev/jhuanca/facturacion/controller/PedidoController.java

    // Agrega este método para generar el enlace de WhatsApp
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

            // Si no tiene teléfono, devolver error
            if (telefono == null || telefono.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "El cliente no tiene número de teléfono registrado");
                return ResponseEntity.badRequest().body(error);
            }

            // Construir mensaje personalizado
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

            // Codificar mensaje para URL
            String mensajeCodificado = java.net.URLEncoder.encode(mensaje, "UTF-8");

            // Generar enlace de WhatsApp
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

    // src/main/java/dev/jhuanca/facturacion/controller/PedidoController.java

    @GetMapping("/detalle/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        // Cargar el pedido con todas sus relaciones
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        // ⚠️ FORZAR LA CARGA DE LA RELACIÓN BOLETA
        // Esto asegura que Hibernate cargue la boleta asociada
        if (pedido.getBoleta() != null) {
            // La boleta ya está cargada
            System.out.println("Boleta encontrada: " + pedido.getBoleta().getNumeroBoleta());
        } else {
            System.out.println("No hay boleta para este pedido");
        }

        // También verificar que el estado sea FINALIZADO
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
                // Enviar notificación WhatsApp
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