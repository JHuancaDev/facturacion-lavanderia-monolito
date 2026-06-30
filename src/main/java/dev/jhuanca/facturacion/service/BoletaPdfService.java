// src/main/java/dev/jhuanca/facturacion/service/BoletaPdfService.java
package dev.jhuanca.facturacion.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class BoletaPdfService {

    // Fuentes - creadas una sola vez para evitar problemas
    private static PdfFont normalFont;
    private static PdfFont boldFont;

    static {
        try {
            normalFont = PdfFontFactory.createFont();
            boldFont = PdfFontFactory.createFont();
        } catch (Exception e) {
            // Fallback en caso de error
            normalFont = null;
            boldFont = null;
        }
    }

    public byte[] generarPdfBoleta(Pedido pedido) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // ============ ENCABEZADO ============
        // Título
        Paragraph titulo = new Paragraph("BOLETA DE VENTA ELECTRÓNICA")
                .setFont(getBoldFont())
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(titulo);

        // Datos del emisor
        document.add(new Paragraph("LAVANDERÍA S.A.C.")
                .setFont(getBoldFont())
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("RUC: 10771318199")
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("")
                .setMarginBottom(10));

        // ============ DATOS DE LA BOLETA ============
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);

        // Número de boleta
        infoTable.addCell(createLabelCell("N° Boleta:"));
        infoTable.addCell(createValueCell(pedido.getBoleta().getNumeroBoleta()));

        // Fecha
        infoTable.addCell(createLabelCell("Fecha Emisión:"));
        infoTable.addCell(createValueCell(
                pedido.getBoleta().getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ));

        // Cliente
        infoTable.addCell(createLabelCell("Cliente:"));
        infoTable.addCell(createValueCell(pedido.getCliente().getNombres() + " " +
                pedido.getCliente().getApellidoPaterno()));

        // DNI
        infoTable.addCell(createLabelCell("DNI:"));
        infoTable.addCell(createValueCell(pedido.getCliente().getDni()));

        document.add(infoTable);

        // ============ TABLA DE DETALLE ============
        Table detailTable = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1, 1, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);

        // Encabezados
        String[] headers = {"#", "Descripción", "Peso", "Precio", "Subtotal"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setFont(getBoldFont()))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER);
            detailTable.addCell(headerCell);
        }

        // Filas de detalle
        int index = 1;
        for (DetallePedido detalle : pedido.getDetalles()) {
            detailTable.addCell(createValueCell(String.valueOf(index++), TextAlignment.CENTER));
            detailTable.addCell(createValueCell(detalle.getServicio().getNameService()));
            detailTable.addCell(createValueCell(detalle.getPeso() + " kg", TextAlignment.CENTER));
            detailTable.addCell(createValueCell("S/ " + String.format("%.2f", detalle.getServicio().getPrecio()), TextAlignment.RIGHT));
            detailTable.addCell(createValueCell("S/ " + String.format("%.2f", detalle.getSubtotal()), TextAlignment.RIGHT));
        }

        document.add(detailTable);

        // ============ TOTAL ============
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(10);

        totalTable.addCell(new Cell()
                .add(new Paragraph("TOTAL").setFont(getBoldFont()).setFontSize(14))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER));

        totalTable.addCell(new Cell()
                .add(new Paragraph("S/ " + String.format("%.2f", pedido.getMontoTotal()))
                        .setFont(getBoldFont())
                        .setFontSize(14))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER));

        document.add(totalTable);

        // ============ PIE DE PÁGINA ============
        document.add(new Paragraph("")
                .setMarginTop(20));

        document.add(new Paragraph("¡Gracias por su preferencia!")
                .setFont(getBoldFont())
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Este comprobante fue emitido electrónicamente")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER));

        // Cerrar documento
        document.close();

        return baos.toByteArray();
    }

    // ✅ CORREGIDO: Métodos con manejo de excepciones
    private Cell createLabelCell(String text) {
        try {
            return new Cell()
                    .add(new Paragraph(text).setFont(getNormalFont()))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2);
        } catch (Exception e) {
            // Fallback: sin fuente personalizada
            return new Cell()
                    .add(new Paragraph(text))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2);
        }
    }

    private Cell createValueCell(String text) {
        try {
            return new Cell()
                    .add(new Paragraph(text).setFont(getNormalFont()))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2);
        } catch (Exception e) {
            return new Cell()
                    .add(new Paragraph(text))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2);
        }
    }

    private Cell createValueCell(String text, TextAlignment alignment) {
        try {
            return new Cell()
                    .add(new Paragraph(text).setFont(getNormalFont()))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2)
                    .setTextAlignment(alignment);
        } catch (Exception e) {
            return new Cell()
                    .add(new Paragraph(text))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(2)
                    .setTextAlignment(alignment);
        }
    }

    // ✅ Métodos seguros para obtener las fuentes
    private PdfFont getNormalFont() throws Exception {
        if (normalFont == null) {
            normalFont = PdfFontFactory.createFont();
        }
        return normalFont;
    }

    private PdfFont getBoldFont() throws Exception {
        if (boldFont == null) {
            boldFont = PdfFontFactory.createFont();
        }
        return boldFont;
    }
}