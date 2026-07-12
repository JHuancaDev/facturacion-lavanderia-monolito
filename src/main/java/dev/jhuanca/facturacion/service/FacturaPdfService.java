package dev.jhuanca.facturacion.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import dev.jhuanca.facturacion.entity.DetallePedido;
import dev.jhuanca.facturacion.entity.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FacturaPdfService {

    @Value("${sunat.razon-social}")
    private String razonSocial;

    @Value("${sunat.ruc}")
    private String ruc;

    @Value("${sunat.direccion}")
    private String direccion;

    @Value("${sunat.telefono}")
    private String telefono;

    @Value("${sunat.nombreComercial}")
    private String nombreComercial;

    @Value("${sunat.paguina}")
    private String paginaWeb;

    private static final float MARGIN = 10f;

    public byte[] generarPdfFactura(Pedido pedido) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);

        // Tamaño A4
        PageSize pageSize = PageSize.A4;
        Document document = new Document(pdfDoc, pageSize);
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        // Fuentes
        PdfFont normalFont = PdfFontFactory.createFont();
        PdfFont boldFont = PdfFontFactory.createFont();
        PdfFont smallFont = PdfFontFactory.createFont();

        // ===== ENCABEZADO =====
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);

        // Logo y nombre comercial (izquierda)
        Cell logoCell = new Cell()
                .add(new Paragraph(nombreComercial)
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setTextAlignment(TextAlignment.LEFT))
                .add(new Paragraph(razonSocial)
                        .setFont(normalFont)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.LEFT))
                .add(new Paragraph("RUC: " + ruc)
                        .setFont(normalFont)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.LEFT))
                .setBorder(Border.NO_BORDER)
                .setPadding(0);

        // Documento y número (derecha)
        Cell docCell = new Cell()
                .add(new Paragraph("FACTURA ELECTRÓNICA")
                        .setFont(boldFont)
                        .setFontSize(16)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .setFontColor(new DeviceRgb(0, 102, 204)))
                .add(new Paragraph("N° " + pedido.getFactura().getNumeroFactura())
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph("Serie: " + pedido.getFactura().getSerie())
                        .setFont(normalFont)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER)
                .setPadding(0);

        headerTable.addCell(logoCell);
        headerTable.addCell(docCell);
        document.add(headerTable);

        // ===== DIRECCIÓN Y DATOS DE CONTACTO =====
        Table contactTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(5);

        contactTable.addCell(createContactCell("📍 Dirección", direccion, normalFont, 9));
        contactTable.addCell(createContactCell("📞 Teléfono", "(51) " + telefono, normalFont, 9));
        contactTable.addCell(createContactCell("🌐 Web", paginaWeb, normalFont, 9));

        document.add(contactTable);

        // ===== LÍNEA SEPARADORA =====
        document.add(new Paragraph("───────────────────────────────────────────────────────────────")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

        // ===== DATOS DEL CLIENTE =====
        Table clienteTable = new Table(UnitValue.createPercentArray(new float[]{1, 3}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5)
                .setMarginBottom(5);

        String tipoDoc = pedido.getCliente().getDni().length() == 11 ? "RUC" : "DNI";

        clienteTable.addCell(createLabelCell("Cliente:", boldFont, 10));
        clienteTable.addCell(createValueCell(pedido.getCliente().getNombres() + " " +
                pedido.getCliente().getApellidoPaterno(), normalFont, 10));

        clienteTable.addCell(createLabelCell(tipoDoc + ":", boldFont, 10));
        clienteTable.addCell(createValueCell(pedido.getCliente().getDni(), normalFont, 10));

        clienteTable.addCell(createLabelCell("Teléfono:", boldFont, 10));
        clienteTable.addCell(createValueCell(pedido.getCliente().getTelefono() != null ?
                pedido.getCliente().getTelefono() : "-", normalFont, 10));

        clienteTable.addCell(createLabelCell("Fecha Emisión:", boldFont, 10));
        clienteTable.addCell(createValueCell(
                pedido.getFactura().getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                normalFont, 10));

        clienteTable.addCell(createLabelCell("Estado:", boldFont, 10));
        String estado = pedido.getFactura().isAnulada() ? "ANULADA" : "VÁLIDA";
        clienteTable.addCell(createValueCell(estado, normalFont, 10));

        document.add(clienteTable);

        // ===== LÍNEA SEPARADORA =====
        document.add(new Paragraph("───────────────────────────────────────────────────────────────")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

        // ===== DETALLE DE SERVICIOS =====
        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 1f, 1f, 1.5f}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5)
                .setMarginBottom(5);

        // Encabezados de la tabla
        String[] headers = {"#", "DESCRIPCIÓN", "PESO", "P/U", "SUBTOTAL"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header)
                            .setFont(boldFont)
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceRgb(230, 230, 230))
                    .setPadding(4);
            detailTable.addCell(headerCell);
        }

        // Filas de detalle
        int index = 1;
        for (DetallePedido detalle : pedido.getDetalles()) {
            String descripcion = detalle.getServicio().getNameService();
            if (detalle.getColor() != null && !detalle.getColor().isEmpty()) {
                descripcion += " (Color: " + detalle.getColor() + ")";
            }

            detailTable.addCell(createValueCell(String.valueOf(index++), TextAlignment.CENTER, normalFont, 9));
            detailTable.addCell(createValueCell(descripcion, TextAlignment.LEFT, normalFont, 9));
            detailTable.addCell(createValueCell(String.format("%.1f kg", detalle.getPeso()), TextAlignment.CENTER, normalFont, 9));
            detailTable.addCell(createValueCell("S/ " + String.format("%.2f", detalle.getServicio().getPrecio()), TextAlignment.RIGHT, normalFont, 9));
            detailTable.addCell(createValueCell("S/ " + String.format("%.2f", detalle.getSubtotal()), TextAlignment.RIGHT, normalFont, 9));
        }

        document.add(detailTable);

        // ===== RESUMEN Y TOTALES =====
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5);

        // Espacio en blanco a la izquierda
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(2));

        // Totales a la derecha
        Cell totalCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(2);

        // Subtotal
        totalCell.add(new Paragraph("Subtotal: S/ " + String.format("%.2f", pedido.getMontoTotal()))
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT));

        // IGV (18%)
        double igv = pedido.getMontoTotal() * 0.18;
        totalCell.add(new Paragraph("IGV (18%): S/ " + String.format("%.2f", igv))
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT));

        // Total
        totalCell.add(new Paragraph("TOTAL: S/ " + String.format("%.2f", pedido.getMontoTotal()))
                .setFont(boldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(new DeviceRgb(0, 102, 204)));

        // Son: (monto en letras)
        totalCell.add(new Paragraph("SON: " + numeroALetras(pedido.getMontoTotal().intValue()) + " CON " +
                String.format("%02d", (int) ((pedido.getMontoTotal() % 1) * 100)) + "/100 SOLES")
                .setFont(normalFont)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT));

        totalTable.addCell(totalCell);
        document.add(totalTable);

        // ===== LÍNEA SEPARADORA =====
        document.add(new Paragraph("───────────────────────────────────────────────────────────────")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

        // ===== PIE DE PÁGINA =====
        Table footerTable = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(5);

        Cell footerCell = new Cell()
                .add(new Paragraph("¡Gracias por su preferencia!")
                        .setFont(boldFont)
                        .setFontSize(11)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Este comprobante es una representación impresa de la Factura Electrónica.")
                        .setFont(normalFont)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Verificar en: " + paginaWeb)
                        .setFont(normalFont)
                        .setFontSize(8)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Fecha de impresión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        .setFont(normalFont)
                        .setFontSize(7)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPadding(2);

        footerTable.addCell(footerCell);
        document.add(footerTable);

        document.close();
        return baos.toByteArray();
    }

    // ===== MÉTODOS AUXILIARES =====

    private Cell createLabelCell(String text, PdfFont font, float fontSize) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setTextAlignment(TextAlignment.LEFT))
                .setBorder(Border.NO_BORDER)
                .setPadding(2);
    }

    private Cell createValueCell(String text, PdfFont font, float fontSize) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-")
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setTextAlignment(TextAlignment.LEFT))
                .setBorder(Border.NO_BORDER)
                .setPadding(2);
    }

    private Cell createValueCell(String text, TextAlignment alignment, PdfFont font, float fontSize) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-")
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setTextAlignment(alignment))
                .setBorder(Border.NO_BORDER)
                .setPadding(2);
    }

    private Cell createContactCell(String label, String value, PdfFont font, float fontSize) {
        return new Cell()
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph(value != null ? value : "-")
                        .setFont(font)
                        .setFontSize(fontSize - 1)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBorder(Border.NO_BORDER)
                .setPadding(2);
    }

    // ===== CONVERSIÓN DE NÚMEROS A LETRAS =====
    private String numeroALetras(int numero) {
        if (numero == 0) return "CERO";

        String[] unidades = {"", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE"};
        String[] decenas = {"", "DIEZ", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
        String[] centenas = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
                "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

        if (numero >= 1000) {
            int miles = numero / 1000;
            int resto = numero % 1000;
            String resultado = (miles == 1 ? "MIL" : numeroALetras(miles) + " MIL");
            if (resto > 0) {
                resultado += " " + numeroALetras(resto);
            }
            return resultado;
        }

        if (numero >= 100) {
            int centena = numero / 100;
            int resto = numero % 100;
            String resultado = centenas[centena];
            if (centena == 1 && resto == 0) return "CIEN";
            if (resto > 0) {
                resultado += " " + numeroALetras(resto);
            }
            return resultado;
        }

        if (numero >= 10) {
            int decena = numero / 10;
            int unidad = numero % 10;
            if (decena == 1 && unidad == 0) return "DIEZ";
            if (decena == 1) {
                String[] especiales = {"", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
                        "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"};
                return especiales[unidad];
            }
            if (decena == 2 && unidad == 0) return "VEINTE";
            if (decena == 2) return "VEINTI" + unidades[unidad].toLowerCase();
            if (unidad == 0) return decenas[decena];
            return decenas[decena] + " Y " + unidades[unidad].toLowerCase();
        }

        return unidades[numero];
    }
}