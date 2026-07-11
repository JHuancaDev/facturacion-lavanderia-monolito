// src/main/java/dev/jhuanca/facturacion/service/SunatService.java
package dev.jhuanca.facturacion.service;

import dev.jhuanca.facturacion.entity.Boleta;
import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Factura;
import dev.jhuanca.facturacion.entity.NotaCredito;
import io.github.project.openubl.xbuilder.content.models.standard.general.CreditNote;
import dev.jhuanca.facturacion.entity.Pedido;
import dev.jhuanca.facturacion.repository.BoletaRepository;
import dev.jhuanca.facturacion.repository.FacturaRepository;
import dev.jhuanca.facturacion.repository.NotaCreditoRepository;
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
import jakarta.transaction.Transactional;

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
import java.time.LocalDateTime;

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

        @Value("${sunat.serie-factura:F001}")
        private String serieFactura;

        @Value("${sunat.certificado.ruta:certificados/sunat.pfx}")
        private String rutaCertificado;

        @Value("${sunat.certificado.password:}")
        private String passwordCertificado;

        @Autowired
        private BoletaRepository boletaRepository;

        @Autowired
        private CamelContext camelContext;

        @Autowired
        private NotaCreditoRepository notaCreditoRepository;

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

        public String generarXmlNotaCredito(
                        Pedido pedido,
                        String numeroNotaCredito,
                        String numeroBoleta,
                        String codigoMotivo,
                        String motivo) throws Exception {

                String[] partes = numeroNotaCredito.split("-");
                String serie = partes[0]; // "B001"
                String numero = partes[1]; // "1"

                CreditNote.CreditNoteBuilder builder = CreditNote.builder()
                                .serie(serie) // ← Usar "B001", no "NC01"
                                .numero(Integer.parseInt(numero))
                                .comprobanteAfectadoSerieNumero(numeroBoleta) // "B001-000009"
                                .sustentoDescripcion(motivo)
                                .proveedor(Proveedor.builder()
                                                .ruc(ruc)
                                                .razonSocial(razonSocial)
                                                .build());

                if (pedido.getCliente() != null) {

                        String dni = pedido.getCliente().getDni();

                        if (dni == null || dni.isBlank()) {
                                dni = "99999999";
                        }

                        builder.cliente(
                                        io.github.project.openubl.xbuilder.content.models.common.Cliente.builder()
                                                        .nombre(
                                                                        pedido.getCliente().getNombres() + " "
                                                                                        + pedido.getCliente()
                                                                                                        .getApellidoPaterno())
                                                        .numeroDocumentoIdentidad(dni)
                                                        .tipoDocumentoIdentidad(Catalog6.DNI.getCode())
                                                        .build());
                }

                for (DetallePedido detalle : pedido.getDetalles()) {

                        builder.detalle(
                                        DocumentoVentaDetalle.builder()
                                                        .descripcion(detalle.getServicio().getNameService())
                                                        .cantidad(BigDecimal.ONE)
                                                        .precio(BigDecimal.valueOf(detalle.getServicio().getPrecio()))
                                                        .unidadMedida("NIU")
                                                        .build());
                }

                CreditNote creditNote = builder.build();

                ContentEnricher enricher = new ContentEnricher(defaults, dateProvider);

                enricher.enrich(creditNote);

                return TemplateProducer.getInstance()
                                .getCreditNote()
                                .data(creditNote)
                                .render();
        }

        public String enviarNotaCreditoSunat(String xmlFirmado) throws Exception {

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

                BillServiceFileAnalyzer analyzer = new BillServiceXMLFileAnalyzer(xmlBytes, companyURLs);

                ZipFile zipFile = analyzer.getZipFile();

                BillServiceDestination destination = analyzer.getSendFileDestination();

                CamelData camelData = CamelUtils.getBillServiceCamelData(
                                zipFile,
                                destination,
                                credentials);

                SunatResponse response = camelContext
                                .createProducerTemplate()
                                .requestBodyAndHeaders(
                                                Constants.XSENDER_BILL_SERVICE_URI,
                                                camelData.getBody(),
                                                camelData.getHeaders(),
                                                SunatResponse.class);

                if (response.getMetadata() != null
                                && response.getMetadata().getResponseCode() == 0) {

                        if (response.getSunat() != null
                                        && response.getSunat().getTicket() != null) {

                                return response.getSunat().getTicket();
                        }

                        return "ACEPTADO";
                }

                throw new Exception(response.getMetadata().getDescription());
        }

        // En SunatService.java
        @Transactional
        public String generarYEnviarNotaCredito(
                        Pedido pedido,
                        String codigoMotivo,
                        String motivo) throws Exception {

                // 1. Validar que el pedido tenga boleta
                if (pedido.getBoleta() == null) {
                        throw new Exception("El pedido no tiene boleta asociada");
                }

                Boleta boleta = pedido.getBoleta();

                // 2. Verificar si ya existe nota de crédito para esta boleta
                if (notaCreditoRepository.findByBoletaId(boleta.getId()).isPresent()) {
                        throw new Exception("Ya existe una nota de crédito para esta boleta");
                }

                // 3. Generar número de nota de crédito
                String numeroNotaCredito = generarNumeroNotaCredito();
                System.out.println("📄 Número de Nota Crédito: " + numeroNotaCredito);

                // 4. Generar XML
                String xml = generarXmlNotaCredito(
                                pedido,
                                numeroNotaCredito,
                                boleta.getNumeroBoleta(),
                                codigoMotivo,
                                motivo);

                // 5. Firmar XML
                String xmlFirmado = firmarXML(xml);

                // 6. Enviar a SUNAT
                String ticket = enviarNotaCreditoSunat(xmlFirmado);
                System.out.println("✅ Ticket de NC: " + ticket);

                // 7. Guardar en base de datos
                NotaCredito notaCredito = new NotaCredito();
                notaCredito.setBoleta(boleta);
                notaCredito.setNumeroNotaCredito(numeroNotaCredito);
                notaCredito.setSerie(serieBoleta);

                String[] partes = numeroNotaCredito.split("-");
                if (partes.length == 2) {
                        notaCredito.setCorrelativo(Integer.parseInt(partes[1]));
                }

                notaCredito.setCodigoMotivo(codigoMotivo);
                notaCredito.setMotivo(motivo);
                notaCredito.setTotal(BigDecimal.valueOf(pedido.getMontoTotal()));
                notaCredito.setFechaEmision(LocalDateTime.now());
                notaCredito.setFechaEnvioSunat(LocalDateTime.now());
                notaCredito.setTicket(ticket);
                notaCredito.setEnviadoSunat(true);
                notaCredito.setSunatRespuesta("{\"success\": true, \"ticket\": \"" + ticket + "\"}");

                notaCreditoRepository.save(notaCredito);

                // 8. Actualizar estado del pedido o boleta
                // Opcional: Anular la boleta original
                boleta.setAnulada(true);
                boletaRepository.save(boleta);

                return ticket;
        }

        public synchronized String generarNumeroNotaCredito() {
                Long ultimoCorrelativo = notaCreditoRepository.findMaxCorrelativoBySerie(serieBoleta);
                if (ultimoCorrelativo == null) {
                        ultimoCorrelativo = 0L;
                }
                Integer nuevoCorrelativo = ultimoCorrelativo.intValue() + 1;
                String numeroFormateado = String.format("%06d", nuevoCorrelativo);
                // La serie debe ser la misma que la boleta original
                return serieBoleta + "-" + numeroFormateado;
        }

        // En SunatService.java - Agregar estos métodos

        @Autowired
        private FacturaRepository facturaRepository;

        // Generar número de factura
        public synchronized String generarNumeroFactura() {
                Long ultimoCorrelativo = facturaRepository.findMaxCorrelativoBySerie(serieFactura);
                if (ultimoCorrelativo == null) {
                        ultimoCorrelativo = 0L;
                }
                Integer nuevoCorrelativo = ultimoCorrelativo.intValue() + 1;
                String numeroFormateado = String.format("%06d", nuevoCorrelativo);
                return serieFactura + "-" + numeroFormateado;
        }

        // Generar XML de Factura
        public String generarXmlFactura(Pedido pedido, String numeroFactura) throws Exception {
                String[] partes = numeroFactura.split("-");
                String serie = partes[0];
                String numero = partes[1];

                Invoice.InvoiceBuilder builder = Invoice.builder()
                                .serie(serie)
                                .numero(Integer.parseInt(numero))
                                .proveedor(Proveedor.builder()
                                                .ruc(ruc)
                                                .razonSocial(razonSocial)
                                                .build());

                // Datos del cliente
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

                // Detalles
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

                return TemplateProducer.getInstance()
                                .getInvoice()
                                .data(invoice)
                                .render();
        }

        // Generar y enviar Factura
        @Transactional
        public String generarYEnviarFactura(Pedido pedido, String numeroFactura) throws Exception {
                // Validar que no tenga factura
                if (facturaRepository.findByPedidoId(pedido.getId()).isPresent()) {
                        throw new Exception("Este pedido ya tiene factura emitida");
                }

                // Generar XML
                String xml = generarXmlFactura(pedido, numeroFactura);

                // Firmar XML
                String xmlFirmado = firmarXML(xml);

                // Enviar a SUNAT
                String ticket = enviarFacturaSunat(xmlFirmado, numeroFactura);
                System.out.println("✅ Ticket de Factura: " + ticket);

                // Guardar en base de datos
                Factura factura = new Factura();
                factura.setPedido(pedido);
                factura.setNumeroFactura(numeroFactura);
                factura.setTotal(pedido.getMontoTotal());
                factura.setSerie(serieFactura);

                String[] partes = numeroFactura.split("-");
                if (partes.length == 2) {
                        factura.setCorrelativo(Integer.parseInt(partes[1]));
                }

                factura.setRucEmisor(ruc);
                factura.setRazonSocialEmisor(razonSocial);
                factura.setClienteTipoDoc("DNI");
                factura.setClienteNumeroDoc(pedido.getCliente().getDni());
                factura.setClienteNombre(pedido.getCliente().getNombres() + " " +
                                pedido.getCliente().getApellidoPaterno());

                factura.setEnviadoSunat(true);
                factura.setFechaEmision(LocalDateTime.now());
                factura.setFechaEnvioSunat(LocalDateTime.now());
                factura.setTicket(ticket);
                factura.setSunatRespuesta("{\"success\": true, \"ticket\": \"" + ticket + "\"}");

                facturaRepository.save(factura);

                return ticket;
        }

        // Enviar Factura a SUNAT
        public String enviarFacturaSunat(String xmlFirmado, String numeroFactura) throws Exception {
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

                if (response.getMetadata() != null && response.getMetadata().getResponseCode() == 0) {
                        if (response.getSunat() != null && response.getSunat().getTicket() != null) {
                                return response.getSunat().getTicket();
                        }
                        return "ACEPTADO";
                }

                throw new Exception(response.getMetadata().getDescription());
        }

        // Nota de Crédito para Factura
        // En SunatService.java - CORREGIR

        @Transactional
        public String generarYEnviarNotaCreditoFactura(
                        Pedido pedido,
                        String codigoMotivo,
                        String motivo) throws Exception {

                if (pedido.getFactura() == null) {
                        throw new Exception("El pedido no tiene factura asociada");
                }

                Factura factura = pedido.getFactura();

                if (factura.isAnulada()) {
                        throw new Exception("Esta factura ya fue anulada");
                }

                String numeroNotaCredito = generarNumeroNotaCreditoFactura();

                String xml = generarXmlNotaCreditoFactura(
                                pedido,
                                numeroNotaCredito,
                                factura.getNumeroFactura(),
                                codigoMotivo,
                                motivo);

                String xmlFirmado = firmarXML(xml);
                String ticket = enviarNotaCreditoSunat(xmlFirmado);

                // Guardar Nota de Crédito
                NotaCredito notaCredito = new NotaCredito();
                notaCredito.setFactura(factura);
                notaCredito.setNumeroNotaCredito(numeroNotaCredito);
                notaCredito.setSerie(serieFactura);
                notaCredito.setCorrelativo(Integer.parseInt(numeroNotaCredito.split("-")[1]));
                notaCredito.setCodigoMotivo(codigoMotivo);
                notaCredito.setMotivo(motivo);
                // ✅ CORREGIR: Usar BigDecimal en lugar de Double
                notaCredito.setTotal(BigDecimal.valueOf(pedido.getMontoTotal()));
                notaCredito.setFechaEmision(LocalDateTime.now());
                notaCredito.setFechaEnvioSunat(LocalDateTime.now());
                notaCredito.setTicket(ticket);
                notaCredito.setEnviadoSunat(true);
                notaCredito.setSunatRespuesta("{\"success\": true, \"ticket\": \"" + ticket + "\"}");

                notaCreditoRepository.save(notaCredito);

                factura.setAnulada(true);
                facturaRepository.save(factura);

                return ticket;
        }

        // En SunatService.java - AGREGAR ESTE MÉTODO

        public String generarXmlNotaCreditoFactura(
                        Pedido pedido,
                        String numeroNotaCredito,
                        String numeroFactura,
                        String codigoMotivo,
                        String motivo) throws Exception {

                String[] partes = numeroNotaCredito.split("-");
                String serie = partes[0]; // "F001"
                String numero = partes[1]; // "1"

                CreditNote.CreditNoteBuilder builder = CreditNote.builder()
                                .serie(serie)
                                .numero(Integer.parseInt(numero))
                                .comprobanteAfectadoSerieNumero(numeroFactura) // "F001-000001"
                                .sustentoDescripcion(motivo)
                                .proveedor(Proveedor.builder()
                                                .ruc(ruc)
                                                .razonSocial(razonSocial)
                                                .build());

                if (pedido.getCliente() != null) {
                        String dni = pedido.getCliente().getDni();
                        if (dni == null || dni.isBlank()) {
                                dni = "99999999";
                        }
                        builder.cliente(
                                        io.github.project.openubl.xbuilder.content.models.common.Cliente.builder()
                                                        .nombre(pedido.getCliente().getNombres() + " " +
                                                                        pedido.getCliente().getApellidoPaterno())
                                                        .numeroDocumentoIdentidad(dni)
                                                        .tipoDocumentoIdentidad(Catalog6.DNI.getCode())
                                                        .build());
                }

                for (DetallePedido detalle : pedido.getDetalles()) {
                        builder.detalle(
                                        DocumentoVentaDetalle.builder()
                                                        .descripcion(detalle.getServicio().getNameService() +
                                                                        " (" + detalle.getPeso() + " kg)")
                                                        .cantidad(BigDecimal.ONE)
                                                        .precio(BigDecimal.valueOf(detalle.getServicio().getPrecio()))
                                                        .unidadMedida("NIU")
                                                        .build());
                }

                CreditNote creditNote = builder.build();
                ContentEnricher enricher = new ContentEnricher(defaults, dateProvider);
                enricher.enrich(creditNote);

                return TemplateProducer.getInstance()
                                .getCreditNote()
                                .data(creditNote)
                                .render();
        }

        public synchronized String generarNumeroNotaCreditoFactura() {
                Long ultimoCorrelativo = notaCreditoRepository.findMaxCorrelativoBySerie(serieFactura);
                if (ultimoCorrelativo == null) {
                        ultimoCorrelativo = 0L;
                }
                Integer nuevoCorrelativo = ultimoCorrelativo.intValue() + 1;
                return serieFactura + "-" + String.valueOf(nuevoCorrelativo);
        }

}