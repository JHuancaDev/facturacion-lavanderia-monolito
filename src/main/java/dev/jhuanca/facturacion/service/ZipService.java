// src/main/java/dev/jhuanca/facturacion/service/ZipService.java
package dev.jhuanca.facturacion.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {

    // src/main/java/dev/jhuanca/facturacion/service/ZipService.java

    public byte[] comprimirXML(String xmlContent, String fileName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        try {
            // ✅ El nombre dentro del ZIP debe ser EXACTAMENTE el mismo que el del archivo
            ZipEntry entry = new ZipEntry(fileName);
            zos.putNextEntry(entry);

            byte[] xmlBytes = xmlContent.getBytes("UTF-8");
            zos.write(xmlBytes, 0, xmlBytes.length);
            zos.closeEntry();

            System.out.println("📦 Archivo dentro del ZIP: " + fileName);
            System.out.println("📦 Tamaño del XML: " + xmlBytes.length + " bytes");

        } finally {
            zos.close();
        }

        return baos.toByteArray();
    }

    public String comprimirXMLABase64(String xmlContent, String fileName) throws IOException {
        byte[] zipBytes = comprimirXML(xmlContent, fileName);
        return java.util.Base64.getEncoder().encodeToString(zipBytes);
    }
}