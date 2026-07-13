package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.entity.Boleta;
import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.repository.BoletaRepository;
import dev.jhuanca.facturacion.repository.PedidoRepository;
import dev.jhuanca.facturacion.service.BoletaPdfService;
import dev.jhuanca.facturacion.service.SunatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/boletas")
public class BoletaController {

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private BoletaPdfService boletaPdfService;

    @Autowired
    private SunatService sunatService;

    @GetMapping
    public String listarBoletas(Model model) {
        List<Boleta> boletas = boletaRepository.findAll();
        model.addAttribute("boletas", boletas);
        model.addAttribute("template", "boleta/list");
        return "layout/base";
    }

    @GetMapping("/{id}/descargar-boleta")
    public ResponseEntity<byte[]> descargarBoleta(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null || pedido.getBoleta() == null) {
                return ResponseEntity.notFound().build();
            }
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

            if (boletaRepository.findByPedidoId(id).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Este pedido ya tiene boleta emitida");
                return "redirect:/pedidos/detalle/" + id;
            }

            String numeroBoleta = sunatService.generarNumeroBoleta();
            System.out.println("Número de Boleta: " + numeroBoleta);

            String ticket = sunatService.generarYEnviarBoleta(pedido, numeroBoleta);
            System.out.println("Ticket recibido: " + ticket);

            Boleta boleta = new Boleta();
            boleta.setPedido(pedido);
            boleta.setNumeroBoleta(numeroBoleta);
            boleta.setTotal(pedido.getMontoTotal());
            boleta.setSerie("B001");

            String[] partes = numeroBoleta.split("-");
            if (partes.length == 2) {
                boleta.setCorrelativo(Integer.parseInt(partes[1]));
            }

            boleta.setRucEmisor("10771318199");
            boleta.setRazonSocialEmisor("LAVANDERIA S.A.C.");

            boleta.setClienteTipoDoc("DNI");
            boleta.setClienteNumeroDoc(pedido.getCliente().getDni());
            boleta.setClienteNombre(pedido.getCliente().getNombres() + " " + pedido.getCliente().getApellidoPaterno());

            boleta.setEnviadoSunat(true);
            boleta.setFechaEmision(LocalDateTime.now());
            boleta.setFechaEnvioSunat(LocalDateTime.now());
            boleta.setTicket(ticket);
            boleta.setSunatRespuesta("{\"success\": true, \"ticket\": \"" + ticket + "\"}");

            boletaRepository.save(boleta);

            pedido.setBoleta(boleta);
            pedidoRepository.save(pedido);

            redirectAttributes.addFlashAttribute("success",
                    "Boleta " + numeroBoleta + " emitida correctamente. Ticket: " + ticket);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error al emitir boleta: " + e.getMessage());
        }

        return "redirect:/pedidos/detalle/" + id;
    }
    @GetMapping("/{id}/descargar-pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null || pedido.getBoleta() == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = boletaPdfService.generarPdfBoleta(pedido);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "boleta_" + pedido.getBoleta().getNumeroBoleta() + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/descargar-xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null || pedido.getBoleta() == null) {
                return ResponseEntity.notFound().build();
            }

            String xml = sunatService.generarXmlBoleta(pedido, pedido.getBoleta().getNumeroBoleta());

            byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment",
                    "boleta_" + pedido.getBoleta().getNumeroBoleta() + ".xml");

            return new ResponseEntity<>(xmlBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/{id}/emitir-nota-credito")
    public String emitirNotaCredito(
            @PathVariable Long id,
            @RequestParam(required = false) String codigoMotivo,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes) {

        try {
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pedido no encontrado"));

            if (pedido.getBoleta() == null) {
                redirectAttributes.addFlashAttribute("error",
                        "El pedido no tiene boleta emitida. No se puede generar nota de crédito.");
                return "redirect:/pedidos/detalle/" + id;
            }

            if (pedido.getBoleta().isAnulada() == false && pedido.getBoleta().isAnulada()) {
                redirectAttributes.addFlashAttribute("error",
                        "Esta boleta ya fue anulada con una nota de crédito.");
                return "redirect:/pedidos/detalle/" + id;
            }

            String motivoCodigo = codigoMotivo != null ? codigoMotivo : "01";
            String motivoDescripcion = motivo != null ? motivo : "ANULACION DE LA OPERACION";

            String ticket = sunatService.generarYEnviarNotaCredito(
                    pedido,
                    motivoCodigo,
                    motivoDescripcion);

            redirectAttributes.addFlashAttribute("success",
                    "Nota de Crédito emitida correctamente.\n" +
                            "📄 Número: " + pedido.getBoleta().getNumeroBoleta() + "\n" +
                            "🎫 Ticket: " + ticket);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al emitir nota de crédito: " + e.getMessage());
        }

        return "redirect:/pedidos/detalle/" + id;
    }
}