// src/main/java/dev/jhuanca/facturacion/service/WhatsAppService.java
package dev.jhuanca.facturacion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import dev.jhuanca.facturacion.entity.Pedido;

@Service
public class WhatsAppService {
    @Value("${whatsapp.api.url:https://api.whatsapp.com/send}")
    private String apiUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public void enviarNotificacionPedidoListo(Pedido pedido) {
        if (pedido.getCliente() != null && pedido.getCliente().getTelefono() != null) {
            String telefono = pedido.getCliente().getTelefono();
            
            // Construir detalle de servicios
            StringBuilder detalleServicios = new StringBuilder();
            for (var detalle : pedido.getDetalles()) {
                detalleServicios.append("   • ")
                    .append(detalle.getServicio().getNameService())
                    .append(" - ")
                    .append(detalle.getPeso())
                    .append(" kg - S/ ")
                    .append(String.format("%.2f", detalle.getSubtotal()))
                    .append("\n");
            }
            
            String mensaje = String.format(
                "🏪 *LAVANDERÍA* 🏪\n\n" +
                "✅ *SU PEDIDO ESTÁ LISTO* ✅\n\n" +
                "📋 *N° Pedido:* %d\n" +
                "👤 *Cliente:* %s %s\n" +
                "📦 *Servicios:*\n%s" +
                "💰 *Total:* S/ %.2f\n" +
                "📍 *Ubicación:* %s\n\n" +
                "🎯 *Pase a recogerlo a nuestro local* 🎯\n\n" +
                "¡Gracias por confiar en nosotros! 🙏",
                pedido.getId(),
                pedido.getCliente().getNombres(),
                pedido.getCliente().getApellidoPaterno(),
                detalleServicios.toString(),
                pedido.getMontoTotal(),
                pedido.getUbicacion() != null ? pedido.getUbicacion().getCodigoUbicacion() : "Pendiente"
            );
            
            enviarMensaje(telefono, mensaje);
        }
    }
    
    private void enviarMensaje(String telefono, String mensaje) {
        try {
            String url = apiUrl + "?phone=51" + telefono + "&text=" + java.net.URLEncoder.encode(mensaje, "UTF-8");
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            System.out.println("Notificación enviada a: " + telefono);
        } catch (Exception e) {
            System.err.println("Error al enviar WhatsApp: " + e.getMessage());
        }
    }
}