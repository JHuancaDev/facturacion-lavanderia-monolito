// src/main/java/dev/jhuanca/facturacion/service/SunatService.java
package dev.jhuanca.facturacion.service;

import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.repository.BoletaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class SunatService {

    @Value("${sunat.ruc}")
    private String ruc;

    @Value("${sunat.razon-social}")
    private String razonSocial;

    @Value("${sunat.serie-boleta}")
    private String serieBoleta;

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private SunatFirmaService firmaService;

    @Autowired
    private SunatSoapClient soapClient;

    /**
     * Genera el número de boleta correlativo
     */
    public synchronized String generarNumeroBoleta() {
        Long ultimoCorrelativo = boletaRepository.findMaxCorrelativo();
        if (ultimoCorrelativo == null) {
            ultimoCorrelativo = 0L;
        }

        Integer nuevoCorrelativo = ultimoCorrelativo.intValue() + 1;
        String numeroFormateado = String.format("%06d", nuevoCorrelativo);
        return serieBoleta + "-" + numeroFormateado;
    }

    public String enviarBoletaSunat(String xmlBoleta, String numeroBoleta) {
        try {
            System.out.println("🚀 INICIANDO ENVÍO A SUNAT BETA...");
            System.out.println("📄 Número de Boleta: " + numeroBoleta);

            // PASO 1: Firmar el XML
            System.out.println("🔐 FIRMANDO XML...");
            String xmlFirmado = firmaService.firmarXML(xmlBoleta);
            System.out.println("✅ XML FIRMADO CORRECTAMENTE");

            // PASO 2: Enviar a SUNAT Beta (ahora con ZIP)
            System.out.println("📤 ENVIANDO A SUNAT BETA...");
            String respuestaSunat = soapClient.enviarBoleta(xmlFirmado, numeroBoleta);

            // PASO 3: Procesar respuesta
            Map<String, String> resultado = procesarRespuestaSunat(respuestaSunat);
            System.out.println("📥 RESPUESTA PROCESADA:");
            System.out.println("   Éxito: " + resultado.get("success"));
            System.out.println("   Código: " + resultado.get("codigo"));
            System.out.println("   Mensaje: " + resultado.get("mensaje"));
            System.out.println("   Ticket: " + resultado.get("ticket"));

            return respuestaSunat;

        } catch (Exception e) {
            System.err.println("❌ ERROR EN ENVÍO A SUNAT: " + e.getMessage());
            e.printStackTrace();
            return "{\"success\": false, \"mensaje\": \"" + e.getMessage() + "\"}";
        }
    }

    // src/main/java/dev/jhuanca/facturacion/service/SunatService.java

    public Map<String, String> procesarRespuestaSunat(String respuesta) {
        Map<String, String> resultado = new HashMap<>();

        try {
            // Verificar si hay error
            if (respuesta.contains("<soap-env:Fault>") || respuesta.contains("<soap:Fault>")) {
                resultado.put("success", "false");

                // Extraer código y mensaje de error
                String faultCode = extraerEntre(respuesta, "<faultcode>", "</faultcode>");
                String faultString = extraerEntre(respuesta, "<faultstring>", "</faultstring>");

                resultado.put("codigo", faultCode != null ? faultCode : "ERROR");
                resultado.put("mensaje", faultString != null ? faultString : "Error en el envío");

                System.err.println("❌ ERROR SUNAT: " + faultCode);
                System.err.println("   Mensaje: " + faultString);

                // Si el error es de nombre de archivo, mostrar el formato esperado
                if (faultString != null && faultString.contains("nombre del archivo")) {
                    System.err.println("   💡 El formato correcto es: RUC-03-SERIE-NUMERO.xml");
                    System.err.println("   💡 Ejemplo: 10771318199-03-B001-000001.xml");
                }
            } else {
                // Respuesta exitosa
                resultado.put("success", "true");

                // Buscar ticket en la respuesta
                String ticket = extraerEntre(respuesta, "<ticket>", "</ticket>");
                if (ticket == null) {
                    ticket = extraerEntre(respuesta, "ticket:", "\n");
                }
                if (ticket == null) {
                    ticket = extraerEntre(respuesta, "ticket='", "'");
                }
                if (ticket == null) {
                    ticket = extraerEntre(respuesta, "ticket=\"", "\"");
                }

                if (ticket != null && !ticket.isEmpty()) {
                    resultado.put("ticket", ticket.trim());
                    System.out.println("✅ TICKET OBTENIDO: " + ticket.trim());
                } else {
                    resultado.put("ticket", "TICKET-" + System.currentTimeMillis());
                    System.out.println("⚠️ No se encontró ticket en la respuesta, se genera uno temporal");
                }
            }
        } catch (Exception e) {
            resultado.put("success", "false");
            resultado.put("mensaje", "Error al procesar respuesta: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }

    private String extraerEntre(String texto, String inicio, String fin) {
        try {
            int start = texto.indexOf(inicio);
            if (start == -1)
                return null;
            start += inicio.length();
            int end = texto.indexOf(fin, start);
            if (end == -1)
                return null;
            return texto.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Genera el XML de la boleta (sin firma)
     */
    public String generarXmlBoleta(Pedido pedido, String numeroBoleta) throws Exception {
        System.out.println("📝 GENERANDO XML DE BOLETA...");

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        Document doc = docFactory.newDocumentBuilder().newDocument();

        // Elemento raíz
        Element rootElement = doc.createElementNS(
                "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2",
                "Invoice");
        rootElement.setAttribute("xmlns:cac",
                "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
        rootElement.setAttribute("xmlns:cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
        rootElement.setAttribute("xmlns:ccts", "urn:un:unece:uncefact:documentation:2");
        rootElement.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
        rootElement.setAttribute("xmlns:ext",
                "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
        rootElement.setAttribute("xmlns:qdt", "urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2");
        rootElement.setAttribute("xmlns:udt",
                "urn:un:unece:uncefact:data:specification:UnqualifiedDataTypesSchemaModule:2");
        doc.appendChild(rootElement);

        // ID (número de boleta)
        Element idElement = doc.createElement("cbc:ID");
        idElement.appendChild(doc.createTextNode(numeroBoleta));
        rootElement.appendChild(idElement);

        // Fecha de emisión
        Element issueDate = doc.createElement("cbc:IssueDate");
        issueDate.appendChild(doc.createTextNode(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)));
        rootElement.appendChild(issueDate);

        // Fecha/hora de emisión
        Element issueTime = doc.createElement("cbc:IssueTime");
        issueTime.appendChild(doc.createTextNode(LocalDateTime.now().format(DateTimeFormatter.ISO_TIME)));
        rootElement.appendChild(issueTime);

        // Tipo de documento (03 = Boleta)
        Element invoiceTypeCode = doc.createElement("cbc:InvoiceTypeCode");
        invoiceTypeCode.setAttribute("listID", "0101");
        invoiceTypeCode.setAttribute("listAgencyName", "PE:SUNAT");
        invoiceTypeCode.setAttribute("listName", "Tipo de Documento");
        invoiceTypeCode.appendChild(doc.createTextNode("03"));
        rootElement.appendChild(invoiceTypeCode);

        // Moneda
        Element docCurrencyCode = doc.createElement("cbc:DocumentCurrencyCode");
        docCurrencyCode.appendChild(doc.createTextNode("PEN"));
        rootElement.appendChild(docCurrencyCode);

        // ============ DATOS DEL EMISOR ============
        Element accountingSupplierParty = doc.createElement("cac:AccountingSupplierParty");
        Element supplierParty = doc.createElement("cac:Party");

        // RUC
        Element supplierIdentification = doc.createElement("cac:PartyIdentification");
        Element supplierId = doc.createElement("cbc:ID");
        supplierId.setAttribute("schemeID", "6");
        supplierId.appendChild(doc.createTextNode(ruc));
        supplierIdentification.appendChild(supplierId);
        supplierParty.appendChild(supplierIdentification);

        // Razón Social
        Element supplierPartyName = doc.createElement("cac:PartyName");
        Element supplierName = doc.createElement("cbc:Name");
        supplierName.appendChild(doc.createTextNode(razonSocial));
        supplierPartyName.appendChild(supplierName);
        supplierParty.appendChild(supplierPartyName);

        accountingSupplierParty.appendChild(supplierParty);
        rootElement.appendChild(accountingSupplierParty);

        // ============ DATOS DEL CLIENTE ============
        Element accountingCustomerParty = doc.createElement("cac:AccountingCustomerParty");
        Element customerParty = doc.createElement("cac:Party");

        Element customerIdentification = doc.createElement("cac:PartyIdentification");
        Element customerId = doc.createElement("cbc:ID");
        customerId.setAttribute("schemeID", "1"); // 1 = DNI
        String dni = (pedido.getCliente().getDni() != null && !pedido.getCliente().getDni().equals("99999999"))
                ? pedido.getCliente().getDni()
                : "99999999";
        customerId.appendChild(doc.createTextNode(dni));
        customerIdentification.appendChild(customerId);
        customerParty.appendChild(customerIdentification);

        Element customerPartyName = doc.createElement("cac:PartyName");
        Element customerName = doc.createElement("cbc:Name");
        String nombreCliente = pedido.getCliente().getNombres() + " " +
                (pedido.getCliente().getApellidoPaterno() != null ? pedido.getCliente().getApellidoPaterno() : "");
        customerName.appendChild(doc.createTextNode(nombreCliente));
        customerPartyName.appendChild(customerName);
        customerParty.appendChild(customerPartyName);

        accountingCustomerParty.appendChild(customerParty);
        rootElement.appendChild(accountingCustomerParty);

        // ============ DETALLE ============
        int lineNumber = 1;
        for (DetallePedido detalle : pedido.getDetalles()) {
            Element invoiceLine = doc.createElement("cac:InvoiceLine");

            Element lineId = doc.createElement("cbc:ID");
            lineId.appendChild(doc.createTextNode(String.valueOf(lineNumber++)));
            invoiceLine.appendChild(lineId);

            // Cantidad (siempre 1 por ahora, el peso va en la descripción)
            Element quantity = doc.createElement("cbc:InvoicedQuantity");
            quantity.setAttribute("unitCode", "NIU");
            quantity.appendChild(doc.createTextNode("1"));
            invoiceLine.appendChild(quantity);

            // Precio unitario
            Element lineExtensionAmount = doc.createElement("cbc:LineExtensionAmount");
            lineExtensionAmount.setAttribute("currencyID", "PEN");
            lineExtensionAmount.appendChild(doc.createTextNode(detalle.getSubtotal().toString()));
            invoiceLine.appendChild(lineExtensionAmount);

            // Item
            Element item = doc.createElement("cac:Item");
            Element description = doc.createElement("cbc:Description");
            description.appendChild(doc.createTextNode(
                    detalle.getServicio().getNameService() +
                            " (" + detalle.getPeso() + " kg)" +
                            (detalle.getColor() != null && !detalle.getColor().isEmpty()
                                    ? " - Color: " + detalle.getColor()
                                    : "")));
            item.appendChild(description);
            invoiceLine.appendChild(item);

            // Precio
            Element price = doc.createElement("cac:Price");
            Element priceAmount = doc.createElement("cbc:PriceAmount");
            priceAmount.setAttribute("currencyID", "PEN");
            priceAmount.appendChild(doc.createTextNode(detalle.getServicio().getPrecio().toString()));
            price.appendChild(priceAmount);
            invoiceLine.appendChild(price);

            rootElement.appendChild(invoiceLine);
        }

        // ============ TOTAL ============
        Element legalMonetaryTotal = doc.createElement("cac:LegalMonetaryTotal");

        Element lineExtensionAmountTotal = doc.createElement("cbc:LineExtensionAmount");
        lineExtensionAmountTotal.setAttribute("currencyID", "PEN");
        lineExtensionAmountTotal.appendChild(doc.createTextNode(pedido.getMontoTotal().toString()));
        legalMonetaryTotal.appendChild(lineExtensionAmountTotal);

        Element payableAmount = doc.createElement("cbc:PayableAmount");
        payableAmount.setAttribute("currencyID", "PEN");
        payableAmount.appendChild(doc.createTextNode(pedido.getMontoTotal().toString()));
        legalMonetaryTotal.appendChild(payableAmount);

        rootElement.appendChild(legalMonetaryTotal);

        // Convertir a String
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        String xml = writer.toString();
        System.out.println("✅ XML GENERADO");
        return xml;
    }
}