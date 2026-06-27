// src/main/java/dev/jhuanca/facturacion/service/SunatFirmaService.java
package dev.jhuanca.facturacion.service;

import org.apache.xml.security.Init;
import org.apache.xml.security.keys.content.KeyInfoReference;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Service
public class SunatFirmaService {

    @Value("${sunat.certificado.ruta:certificados/sunat.pfx}")
    private String rutaCertificado;

    @Value("${sunat.certificado.password:}")
    private String passwordCertificado;

    static {
        Init.init();
    }

    /**
     * Firma el XML de la boleta con el certificado digital
     */
    public String firmarXML(String xmlBoleta) throws Exception {
        // 1. Cargar el certificado digital
        PrivateKey privateKey = getPrivateKey();
        X509Certificate certificate = getCertificate();

        // 2. Convertir XML a Document
        Document document = parseXML(xmlBoleta);

        // 3. Crear la firma
        XMLSignature signature = new XMLSignature(document, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);

        // 4. Configurar transformaciones
        Transforms transforms = new Transforms(document);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_OMIT_COMMENTS);

        // 5. Agregar el documento a firmar
        signature.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);

        // 6. Crear el KeyInfo con el certificado - CORREGIDO
        // Crear el elemento KeyInfo
        Element keyInfoElement = document.createElementNS(Constants.SignatureSpecNS, "ds:KeyInfo");
        
        // Agregar el nombre de la clave
        Element keyNameElement = document.createElementNS(Constants.SignatureSpecNS, "ds:KeyName");
        keyNameElement.appendChild(document.createTextNode("SUNAT"));
        keyInfoElement.appendChild(keyNameElement);
        
        // Agregar el certificado X509
        Element x509DataElement = document.createElementNS(Constants.SignatureSpecNS, "ds:X509Data");
        Element x509CertificateElement = document.createElementNS(Constants.SignatureSpecNS, "ds:X509Certificate");
        
        String certBase64 = Base64.getEncoder().encodeToString(certificate.getEncoded());
        // Formatear el certificado con saltos de línea cada 64 caracteres
        String certFormatted = formatCertificate(certBase64);
        x509CertificateElement.appendChild(document.createTextNode(certFormatted));
        x509DataElement.appendChild(x509CertificateElement);
        keyInfoElement.appendChild(x509DataElement);
        
        // CORRECCIÓN: Agregar el KeyInfo a la firma usando el método setKeyInfo
        signature.setElement(x509CertificateElement, certFormatted);

        // 7. Firmar el documento
        signature.sign(privateKey);

        // 8. Convertir Document a String
        return documentToString(document);
    }

    /**
     * Formatea el certificado en base64 con saltos de línea
     */
    private String formatCertificate(String base64Cert) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < base64Cert.length(); i += 64) {
            int end = Math.min(i + 64, base64Cert.length());
            formatted.append(base64Cert.substring(i, end));
            if (end < base64Cert.length()) {
                formatted.append("\n");
            }
        }
        return formatted.toString();
    }

    private PrivateKey getPrivateKey() throws Exception {
        ClassPathResource resource = new ClassPathResource(rutaCertificado);
        byte[] pfxBytes = Files.readAllBytes(Paths.get(resource.getURI()));
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(pfxBytes), passwordCertificado.toCharArray());
        
        String alias = keyStore.aliases().nextElement();
        PrivateKey key = (PrivateKey) keyStore.getKey(alias, passwordCertificado.toCharArray());
        
        if (key == null) {
            throw new RuntimeException("No se pudo cargar la clave privada del certificado");
        }
        return key;
    }

    private X509Certificate getCertificate() throws Exception {
        ClassPathResource resource = new ClassPathResource(rutaCertificado);
        byte[] pfxBytes = Files.readAllBytes(Paths.get(resource.getURI()));
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(pfxBytes), passwordCertificado.toCharArray());
        
        String alias = keyStore.aliases().nextElement();
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
        
        if (cert == null) {
            throw new RuntimeException("No se pudo cargar el certificado");
        }
        return cert;
    }

    private Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }

    private String documentToString(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }
}