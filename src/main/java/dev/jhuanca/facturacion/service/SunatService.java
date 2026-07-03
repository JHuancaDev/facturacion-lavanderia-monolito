// src/main/java/dev/jhuanca/facturacion/service/SunatService.java
package dev.jhuanca.facturacion.service;

import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.repository.BoletaRepository;
import io.github.project.openubl.xbuilder.content.catalogs.Catalog6;
import io.github.project.openubl.xbuilder.content.models.common.Proveedor;
import io.github.project.openubl.xbuilder.content.models.standard.general.DocumentoVentaDetalle;
import io.github.project.openubl.xbuilder.content.models.standard.general.Invoice;
import io.github.project.openubl.xbuilder.enricher.ContentEnricher;
import io.github.project.openubl.xbuilder.enricher.config.DateProvider;
import io.github.project.openubl.xbuilder.enricher.config.Defaults;
import io.github.project.openubl.xbuilder.renderer.TemplateProducer;
import io.github.project.openubl.xbuilder.signature.CertificateDetails;
import io.github.project.openubl.xbuilder.signature.CertificateDetailsFactory;
import io.github.project.openubl.xbuilder.signature.XMLSigner;
import io.github.project.openubl.xbuilder.signature.XmlSignatureHelper;
import io.github.project.openubl.xsender.Constants;
import io.github.project.openubl.xsender.camel.utils.CamelData;
import io.github.project.openubl.xsender.camel.utils.CamelUtils;
import io.github.project.openubl.xsender.company.CompanyCredentials;
import io.github.project.openubl.xsender.company.CompanyURLs;
import io.github.project.openubl.xsender.files.BillServiceFileAnalyzer;
import io.github.project.openubl.xsender.files.BillServiceXMLFileAnalyzer;
import io.github.project.openubl.xsender.files.ZipFile;
import io.github.project.openubl.xsender.models.SunatResponse;
import io.github.project.openubl.xsender.sunat.BillServiceDestination;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;

@Service
public class SunatService {

        @Value("${sunat.ruc}")
        private String ruc;

        @Value("${sunat.razon-social}")
        private String razonSocial;

        @Value("${sunat.usuario}")
        private String usuario;

        @Value("${sunat.password}")
        private String password;

        @Value("${sunat.url-beta}")
        private String urlSunat;

        @Value("${sunat.serie-boleta}")
        private String serieBoleta;

        @Value("${sunat.certificado.ruta:certificados/sunat.pfx}")
        private String rutaCertificado;

        @Value("${sunat.certificado.password:}")
        private String passwordCertificado;

        @Autowired
        private BoletaRepository boletaRepository;

        @Autowired
        private CamelContext camelContext;

        private final Defaults defaults = Defaults.builder()
                        .igvTasa(new BigDecimal("0.18"))
                        .icbTasa(new BigDecimal("0.20"))
                        .build();

        private final DateProvider dateProvider = LocalDate::now;

        public synchronized String generarNumeroBoleta() {
                Long ultimoCorrelativo = boletaRepository.findMaxCorrelativo();
                if (ultimoCorrelativo == null) {
                        ultimoCorrelativo = 0L;
                }
                Integer nuevoCorrelativo = ultimoCorrelativo.intValue() + 1;
                String numeroFormateado = String.format("%06d", nuevoCorrelativo);
                return serieBoleta + "-" + numeroFormateado;
        }

        public String generarXmlBoleta(Pedido pedido, String numeroBoleta) throws Exception {

                String numero = numeroBoleta.split("-")[1];
                Invoice.InvoiceBuilder builder = Invoice.builder()
                                .serie(serieBoleta)
                                .numero(Integer.parseInt(numero))
                                .proveedor(Proveedor.builder()
                                                .ruc(ruc)
                                                .razonSocial(razonSocial)
                                                .build());
                if (pedido.getCliente() != null) {
                        String dni = pedido.getCliente().getDni();
                        if (dni == null || dni.isEmpty() || dni.equals("99999999")) {
                                dni = "99999999";
                        }
                        builder.cliente(io.github.project.openubl.xbuilder.content.models.common.Cliente.builder()
                                        .nombre(pedido.getCliente().getNombres() + " " +
                                                        (pedido.getCliente().getApellidoPaterno() != null
                                                                        ? pedido.getCliente().getApellidoPaterno()
                                                                        : ""))
                                        .numeroDocumentoIdentidad(dni)
                                        .tipoDocumentoIdentidad(Catalog6.DNI.getCode())
                                        .build());
                }

                for (DetallePedido detalle : pedido.getDetalles()) {
                        builder.detalle(DocumentoVentaDetalle.builder()
                                        .descripcion(detalle.getServicio().getNameService() +
                                                        " (" + detalle.getPeso() + " kg)" +
                                                        (detalle.getColor() != null && !detalle.getColor().isEmpty()
                                                                        ? " - Color: " + detalle.getColor()
                                                                        : ""))
                                        .cantidad(BigDecimal.ONE)
                                        .precio(BigDecimal.valueOf(detalle.getServicio().getPrecio()))
                                        .unidadMedida("NIU")
                                        .build());
                }

                Invoice invoice = builder.build();

                ContentEnricher enricher = new ContentEnricher(defaults, dateProvider);
                enricher.enrich(invoice);

                String xml = TemplateProducer.getInstance()
                                .getInvoice()
                                .data(invoice)
                                .render();

                System.out.println(" XML GENERADO CON XBUILDER");
                return xml;
        }

        public String firmarXML(String xml) throws Exception {
                System.out.println("FIRMANDO XML CON XBUILDER...");

                ClassPathResource resource = new ClassPathResource(rutaCertificado);
                try (InputStream ksInputStream = resource.getInputStream()) {
                        CertificateDetails certificate = CertificateDetailsFactory.create(
                                        ksInputStream,
                                        passwordCertificado);

                        X509Certificate x509Certificate = certificate.getX509Certificate();
                        PrivateKey privateKey = certificate.getPrivateKey();

                        String idFirma = "Signature_" + System.currentTimeMillis();
                        System.out.println("   ID de firma: " + idFirma);

                        Document signedXML = XMLSigner.signXML(
                                        xml,
                                        idFirma,
                                        x509Certificate,
                                        privateKey);

                        byte[] bytesFromDocument = XmlSignatureHelper.getBytesFromDocument(signedXML);
                        String xmlFirmado = new String(bytesFromDocument, StandardCharsets.ISO_8859_1);

                        System.out.println("✅ XML FIRMADO CORRECTAMENTE");
                        return xmlFirmado;
                }
        }

        public String enviarBoletaSunat(String xmlFirmado, String numeroBoleta) throws Exception {

                CompanyURLs companyURLs = CompanyURLs.builder()
                                .invoice(urlSunat)
                                .perceptionRetention(
                                                "https://e-beta.sunat.gob.pe/ol-ti-itemision-otroscpe-gem-beta/billService")
                                .despatch("https://api-cpe.sunat.gob.pe/v1/contribuyente/gem")
                                .build();

                CompanyCredentials credentials = CompanyCredentials.builder()
                                .username(usuario)
                                .password(password)
                                .build();

                byte[] xmlBytes = xmlFirmado.getBytes(StandardCharsets.UTF_8);
                BillServiceFileAnalyzer fileAnalyzer = new BillServiceXMLFileAnalyzer(xmlBytes, companyURLs);
                ZipFile zipFile = fileAnalyzer.getZipFile();
                BillServiceDestination fileDestination = fileAnalyzer.getSendFileDestination();

                CamelData camelData = CamelUtils.getBillServiceCamelData(zipFile, fileDestination, credentials);

                SunatResponse response = camelContext.createProducerTemplate()
                                .requestBodyAndHeaders(
                                                Constants.XSENDER_BILL_SERVICE_URI,
                                                camelData.getBody(),
                                                camelData.getHeaders(),
                                                SunatResponse.class);

                System.out.println("=== RESPUESTA DE SUNAT ===");
                System.out.println("Status: " + response.getStatus());
                System.out.println("Sunat: " + response.getSunat());
                if (response.getMetadata() != null) {
                        System.out.println("Metadata - Code: " + response.getMetadata().getResponseCode());
                        System.out.println("Metadata - Description: " + response.getMetadata().getDescription());
                }
                System.out.println("===========================");

                boolean esExitoso = false;
                String ticket = null;
                String mensaje = "Error desconocido";

                // 1. Verificar ticket
                if (response.isTicket() && response.getSunat() != null && response.getSunat().getTicket() != null) {
                        ticket = response.getSunat().getTicket();
                        esExitoso = true;
                }

                if (response.getMetadata() != null && response.getMetadata().getResponseCode() != null) {
                        int code = response.getMetadata().getResponseCode();
                        String description = response.getMetadata().getDescription();

                        if (code == 0) {
                                esExitoso = true;
                                mensaje = description;
                                if (ticket == null) {
                                        ticket = "TICKET-" + System.currentTimeMillis() + "-ACEPTADO";
                                }
                        } else {
                                mensaje = description;
                        }
                }

                if (esExitoso) {
                        return ticket;
                } else {
                        throw new Exception("Error al enviar boleta: " + mensaje);
                }
        }

        public String generarYEnviarBoleta(Pedido pedido, String numeroBoleta) throws Exception {
                
                String xml = generarXmlBoleta(pedido, numeroBoleta);

                String xmlFirmado = firmarXML(xml);

                String ticket = enviarBoletaSunat(xmlFirmado, numeroBoleta);

                return ticket;
        }
}