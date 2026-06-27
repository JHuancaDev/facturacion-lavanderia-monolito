// src/main/java/dev/jhuanca/facturacion/service/SunatSoapClient.java
package dev.jhuanca.facturacion.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

@Service
public class SunatSoapClient {

    @Value("${sunat.url-beta}")
    private String urlSunat;

    @Value("${sunat.usuario}")
    private String usuario;

    @Value("${sunat.password}")
    private String password;

    @Value("${sunat.ruc}")
    private String ruc;

    @Autowired
    private ZipService zipService;

    // src/main/java/dev/jhuanca/facturacion/service/SunatSoapClient.java

    public String enviarBoleta(String xmlFirmado, String numeroBoleta) throws Exception {

        System.out.println("📤 CREANDO CLIENTE SOAP...");
        System.out.println("📄 Número de Boleta: " + numeroBoleta);

        // 🔥 FORMATO CORRECTO: RUC-03-SERIE-NUMERO.xml
        // Ejemplo: 10771318199-03-B001-000008.xml
        String nombreCompleto = ruc + "-03-" + numeroBoleta + ".xml";

        System.out.println("📄 Nombre del archivo: " + nombreCompleto);

        // Crear conexión SOAP
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();

        // Crear mensaje SOAP
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        SOAPMessage soapMessage = messageFactory.createMessage();

        // Crear el SOAP Envelope
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("ser", "http://service.sunat.gob.pe");
        envelope.addNamespaceDeclaration("wsse",
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");

        // Crear el Body
        SOAPBody body = envelope.getBody();
        SOAPElement sendBill = body.addChildElement("sendBill", "ser");

        // ✅ IMPORTANTE: El ZIP y el XML dentro deben tener el MISMO nombre
        String zipBase64 = zipService.comprimirXMLABase64(xmlFirmado, nombreCompleto);
        System.out.println("📦 ZIP generado, tamaño: " + zipBase64.length() + " caracteres");

        // ✅ El nombre del ZIP debe ser el mismo que el XML (con .xml)
        SOAPElement fileName = sendBill.addChildElement("fileName");
        fileName.addTextNode(nombreCompleto);

        SOAPElement contentFile = sendBill.addChildElement("contentFile");
        contentFile.addTextNode(zipBase64);

        // Agregar el header de autenticación (WS-Security)
        SOAPHeader header = envelope.getHeader();
        if (header == null) {
            header = envelope.addHeader();
        }

        SOAPElement security = header.addChildElement("Security", "wsse");
        security.addAttribute(new javax.xml.namespace.QName("xmlns:wsse"),
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");

        SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");
        SOAPElement username = usernameToken.addChildElement("Username", "wsse");
        username.addTextNode(usuario);

        SOAPElement passwordElement = usernameToken.addChildElement("Password", "wsse");
        passwordElement.addAttribute(new javax.xml.namespace.QName("Type"),
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
        passwordElement.addTextNode(password);

        // Guardar el mensaje antes de enviar
        soapMessage.saveChanges();

        // Mostrar el SOAP Request
        String soapRequest = soapMessageToString(soapMessage);
        System.out.println("=== SOAP REQUEST (primeros 500 caracteres) ===");
        System.out.println(soapRequest.substring(0, Math.min(500, soapRequest.length())) + "...");

        // Enviar a SUNAT
        System.out.println("📤 ENVIANDO A SUNAT... URL: " + urlSunat);
        SOAPMessage response = soapConnection.call(soapMessage, urlSunat);
        soapConnection.close();

        // Procesar respuesta
        String responseString = soapMessageToString(response);
        System.out.println("=== SOAP RESPONSE ===");
        System.out.println(responseString);

        return responseString;
    }

    private String soapMessageToString(SOAPMessage message) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(message.getSOAPPart()), new StreamResult(writer));
        return writer.toString();
    }
}