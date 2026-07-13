package dev.jhuanca.facturacion.controller;

import dev.jhuanca.facturacion.entity.Cliente;
import dev.jhuanca.facturacion.entity.Factura;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.repository.FacturaRepository;
import dev.jhuanca.facturacion.repository.PedidoRepository;
import dev.jhuanca.facturacion.service.FacturaPdfService;
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
import java.util.List;

@Controller
@RequestMapping("/facturas")
public class FacturaController {

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private SunatService sunatService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private FacturaPdfService facturaPdfService;    

    @GetMapping
    public String listarFacturas(Model model) {
        List<Factura> facturas = facturaRepository.findAll();
        model.addAttribute("facturas", facturas);
        model.addAttribute("template", "factura/list");
        return "layout/base";
    }
    @GetMapping("/{id}/emitir-factura")
    public String emitirFactura(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pedido no encontrado"));

            Cliente cliente = pedido.getCliente();
            if (cliente.getDni() == null || cliente.getDni().length() != 11) {
                redirectAttributes.addFlashAttribute("error",
                        "Para emitir factura, el cliente debe tener RUC (11 dígitos)");
                return "redirect:/pedidos/detalle/" + id;
            }

            String numeroFactura = sunatService.generarNumeroFactura();
            System.out.println("Número de Factura: " + numeroFactura);

            String ticket = sunatService.generarYEnviarFactura(pedido, numeroFactura);

            redirectAttributes.addFlashAttribute("success",
                    "Factura " + numeroFactura + " emitida correctamente. Ticket: " + ticket);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al emitir factura: " + e.getMessage());
        }

        return "redirect:/pedidos/detalle/" + id;
    }

    @GetMapping("/{id}/emitir-nota-credito-factura")
    public String emitirNotaCreditoFactura(
            @PathVariable Long id,
            @RequestParam(required = false) String codigoMotivo,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes) {

        try {
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new Exception("Pedido no encontrado"));

            if (pedido.getFactura() == null) {
                redirectAttributes.addFlashAttribute("error",
                        "El pedido no tiene factura emitida");
                return "redirect:/pedidos/detalle/" + id;
            }

            if (pedido.getFactura().isAnulada()) {
                redirectAttributes.addFlashAttribute("error",
                        "Esta factura ya fue anulada");
                return "redirect:/pedidos/detalle/" + id;
            }

            String motivoCodigo = codigoMotivo != null ? codigoMotivo : "01";
            String motivoDescripcion = motivo != null ? motivo : "ANULACION DE LA OPERACION";

            String ticket = sunatService.generarYEnviarNotaCreditoFactura(
                    pedido,
                    motivoCodigo,
                    motivoDescripcion);

            redirectAttributes.addFlashAttribute("success",
                    "Nota de Crédito de Factura emitida correctamente. Ticket: " + ticket);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error al emitir nota de crédito: " + e.getMessage());
        }

        return "redirect:/pedidos/detalle/" + id;
    }

    
    @GetMapping("/{id}/descargar-pdf-factura")
    public ResponseEntity<byte[]> descargarPdfFactura(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null || pedido.getFactura() == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = facturaPdfService.generarPdfFactura(pedido);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "factura_" + pedido.getFactura().getNumeroFactura() + ".pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/descargar-xml-factura")
    public ResponseEntity<byte[]> descargarXmlFactura(@PathVariable Long id) {
        try {
            Pedido pedido = pedidoRepository.findById(id).orElse(null);
            if (pedido == null || pedido.getFactura() == null) {
                return ResponseEntity.notFound().build();
            }

            String xml = sunatService.generarXmlFactura(pedido, pedido.getFactura().getNumeroFactura());

            byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setContentDispositionFormData("attachment",
                    "factura_" + pedido.getFactura().getNumeroFactura() + ".xml");

            return new ResponseEntity<>(xmlBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}