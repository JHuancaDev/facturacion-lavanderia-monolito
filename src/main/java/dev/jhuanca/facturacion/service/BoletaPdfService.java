
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class BoletaPdfService {

        @Value("${sunat.razon-social}")
        private String razonSocial;

        @Value("${sunat.ruc}")
        private String ruc;

        @Value("${sunat.paguina}")
        private String paguina;

        @Value("${sunat.direccion}")
        private String direccion;

        @Value("${sunat.telefono}")
        private String telefono;

        @Value("${sunat.nombreComercial}")
        private String nombreComercial;

        private static PdfFont normalFont;
        private static PdfFont boldFont;
        private static final float TICKET_WIDTH = 55f;
        private static final float MARGIN = 5f;

        static {
                try {
                        normalFont = PdfFontFactory.createFont();
                        boldFont = PdfFontFactory.createFont();
                } catch (Exception e) {
                        normalFont = null;
                        boldFont = null;
                }
        }

        public byte[] generarPdfBoleta(Pedido pedido) throws Exception {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);

                PageSize ticketSize = new PageSize(TICKET_WIDTH * 2.83465f, 200f * 2.83465f); // Convertir mm a puntos
                Document document = new Document(pdfDoc, ticketSize);
                document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);


                document.add(new Paragraph(nombreComercial)
                                .setBold()
                                .setFontSize(14)
                                .setTextAlignment(TextAlignment.CENTER));

                document.add(new Paragraph(razonSocial )
                                .setBold()
                                .setFontSize(8)
                                .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("RUC: " + ruc)
                                .setFontSize(7)
                                .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph(direccion)
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("LIMA - LIMA")
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("Telf: (51) " + telefono)
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));
                document.add(new Paragraph("----------------------------------------")
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));

                Table headerTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                .setWidth(UnitValue.createPercentValue(100));

                headerTable.addCell(new Cell()
                                .add(new Paragraph("BOLETA ELECTRÓNICA")
                                                .setBold()
                                                .setFontSize(8)
                                                .setTextAlignment(TextAlignment.LEFT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));

                headerTable.addCell(new Cell()
                                .add(new Paragraph("N° " + pedido.getBoleta().getNumeroBoleta())
                                                .setBold()
                                                .setFontSize(8)
                                                .setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));
                document.add(headerTable);
                document.add(new Paragraph("----------------------------------------")
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));

                Table clienteTable = new Table(UnitValue.createPercentArray(new float[] { 1, 2 }))
                                .setWidth(UnitValue.createPercentValue(100));

                clienteTable.addCell(createLabelCell("Cliente:", 6));
                clienteTable.addCell(createValueCell(
                                pedido.getCliente().getNombres() + " " + pedido.getCliente().getApellidoPaterno(), 6));

                clienteTable.addCell(createLabelCell("DNI:", 6));
                clienteTable.addCell(createValueCell(pedido.getCliente().getDni(), 6));

                clienteTable.addCell(createLabelCell("Fecha:", 6));
                clienteTable.addCell(createValueCell(
                                pedido.getBoleta().getFechaEmision()
                                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                6));
                document.add(clienteTable);
                document.add(new Paragraph("----------------------------------------")
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));

                Table detailTable = new Table(UnitValue.createPercentArray(new float[] { 1, 3, 1 }))
                                .setWidth(UnitValue.createPercentValue(100));

                String[] headers = { "CANT", "DESCRIPCIÓN", "TOTAL" };
                for (String header : headers) {
                        Cell headerCell = new Cell()
                                        .add(new Paragraph(header).setBold().setFontSize(6))
                                        .setBackgroundColor(new DeviceRgb(230, 230, 230))
                                        .setTextAlignment(TextAlignment.CENTER)
                                        .setPadding(1);
                        detailTable.addCell(headerCell);
                }

                for (DetallePedido detalle : pedido.getDetalles()) {
                        // Descripción con peso incluido (más compacto)
                        String descripcion = detalle.getServicio().getNameService();
                        if (detalle.getPeso() != null && detalle.getPeso() > 0) {
                                descripcion += " " + detalle.getPeso() + "kg";
                        }

                        detailTable.addCell(createValueCell("1", TextAlignment.CENTER, 6));
                        detailTable.addCell(createValueCell(descripcion, TextAlignment.LEFT, 6));
                        detailTable.addCell(createValueCell("S/" + String.format("%.2f", detalle.getSubtotal()),
                                        TextAlignment.RIGHT, 6));
                }
                document.add(detailTable);
                document.add(new Paragraph("----------------------------------------")
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));

                Table totalTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                .setWidth(UnitValue.createPercentValue(100));

                totalTable.addCell(new Cell()
                                .add(new Paragraph("TOTAL:")
                                                .setBold()
                                                .setFontSize(8)
                                                .setTextAlignment(TextAlignment.LEFT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));
                totalTable.addCell(new Cell()
                                .add(new Paragraph("S/" + String.format("%.2f", pedido.getMontoTotal()))
                                                .setBold()
                                                .setFontSize(8)
                                                .setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));
                document.add(totalTable);

                Table pagoTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                                .setWidth(UnitValue.createPercentValue(100));

                pagoTable.addCell(new Cell()
                                .add(new Paragraph("PAGO:")
                                                .setFontSize(6)
                                                .setTextAlignment(TextAlignment.LEFT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));
                pagoTable.addCell(new Cell()
                                .add(new Paragraph("S/100.00 Efectivo")
                                                .setFontSize(6)
                                                .setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));

                pagoTable.addCell(new Cell()
                                .add(new Paragraph("VUELTO:")
                                                .setFontSize(6)
                                                .setTextAlignment(TextAlignment.LEFT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));
                pagoTable.addCell(new Cell()
                                .add(new Paragraph("S/56.20")
                                                .setFontSize(6)
                                                .setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0));
                document.add(pagoTable);
                document.add(new Paragraph("----------------------------------------")
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.CENTER));

                document.add(new Paragraph("¡Gracias por su preferencia!")
                                .setBold()
                                .setFontSize(7)
                                .setTextAlignment(TextAlignment.CENTER));

                document.add(new Paragraph("Comprobante Electrónico")
                                .setFontSize(5)
                                .setTextAlignment(TextAlignment.CENTER));

                document.add(new Paragraph("Validar en: " + paguina)
                                .setFontSize(5)
                                .setTextAlignment(TextAlignment.CENTER));

                String fechaImp = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                document.add(new Paragraph("Imp: " + nombreComercial + fechaImp)
                                .setFontSize(4)
                                .setTextAlignment(TextAlignment.CENTER));

                document.close();
                return baos.toByteArray();
        }

        private Cell createLabelCell(String text, float fontSize) {
                return new Cell()
                                .add(new Paragraph(text).setBold().setFontSize(fontSize))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0)
                                .setPaddingRight(2);
        }

        private Cell createValueCell(String text, float fontSize) {
                return new Cell()
                                .add(new Paragraph(text).setFontSize(fontSize))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0);
        }

        private Cell createValueCell(String text, TextAlignment alignment, float fontSize) {
                return new Cell()
                                .add(new Paragraph(text).setFontSize(fontSize).setTextAlignment(alignment))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(0);
        }

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