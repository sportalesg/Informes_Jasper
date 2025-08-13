package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.interfaces.PlantillaResumen;
import com.example.demo.models.Plantilla;
import com.example.demo.repository.PlantillaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;

@RestController
@RequestMapping("/")
public class PlantillaController {

    @Autowired
    private PlantillaRepository plantillaRepository;

    // ==============================
    // SUBIR PLANTILLA (.jrxml o .jasper)
    // ==============================
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("template") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                return ResponseEntity.badRequest().body("Archivo no válido");
            }

            // Obtener extensión
            String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();

            // Solo aceptamos .jrxml o .jasper
            if (!ext.equals(".jrxml") && !ext.equals(".jasper")) {
                return ResponseEntity.badRequest().body("Solo se permiten archivos .jrxml o .jasper");
            }

            String baseName = filename.substring(0, filename.lastIndexOf("."));
            
            // Evitar duplicados cambiando el nombre si ya existe
            List<Plantilla> plantillas = plantillaRepository.findAll();
            String storedBase = baseName;
            int count = 2;
            while (true) {
                String finalStoredBase = storedBase;
                boolean exists = plantillas.stream()
                        .anyMatch(p -> p.getNombre().equalsIgnoreCase(finalStoredBase + ".jasper")
                                || p.getNombre().equalsIgnoreCase(finalStoredBase + ".jrxml"));
                if (!exists) break;
                storedBase = baseName + "_v" + count;
                count++;
            }

            String storedName = storedBase + ext;
            byte[] contenidoParaGuardar;

            // Si es .jrxml → compilar a .jasper
            if (ext.equals(".jrxml")) {
                JasperReport jasperReport = JasperCompileManager.compileReport(
                        new ByteArrayInputStream(file.getBytes())
                );

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JRSaver.saveObject(jasperReport, baos); // Guardar el compilado en memoria
                contenidoParaGuardar = baos.toByteArray();

                storedName = storedBase + ".jasper";
                ext = ".jasper";
            } else {
                // Si ya es .jasper lo guardamos tal cual
                contenidoParaGuardar = file.getBytes();
            }

            // Guardar en BD
            Plantilla plantilla = new Plantilla();
            plantilla.setNombre(storedName);
            plantilla.setExtension(ext.substring(1));
            plantilla.setContenido(contenidoParaGuardar);

            plantillaRepository.save(plantilla);

            return ResponseEntity.ok(Map.of(
                    "message", "Plantilla subida correctamente",
                    "nombre", storedName
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ==============================
    // LISTAR PLANTILLAS (solo resumen)
    // ==============================
    @GetMapping("/list")
    public List<PlantillaResumen> list() {
        return plantillaRepository.findAllBy();
    }

    // ==============================
    // ELIMINAR PLANTILLA POR ID
    // ==============================
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam Long id) {
        if (!plantillaRepository.existsById(id)) {
            return ResponseEntity.status(404).body("Plantilla no encontrada");
        }
        plantillaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of(
                "message", "Plantilla eliminada",
                "id", id
        ));
    }

    // ==============================
    // GENERAR REPORTE EN PDF, DOC, DOCX u ODT
    // ==============================
    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(
            @RequestParam("id") Long id,               // ID de la plantilla a usar
            @RequestParam("data") String jsonData,     // Datos en JSON para rellenar el reporte
            @RequestParam(value = "format", required = false) String format // Formato de salida opcional
    ) {
        try {
            // 1️⃣ Buscar plantilla en BD
            Plantilla plantilla = plantillaRepository.findById(id).orElse(null);
            if (plantilla == null) {
                return ResponseEntity.status(404).body("Plantilla no encontrada");
            }

            // 2️⃣ Validar que sea .jasper
            if (!"jasper".equalsIgnoreCase(plantilla.getExtension())) {
                return ResponseEntity.badRequest().body("Solo se pueden generar reportes desde plantillas .jasper");
            }

            // 3️⃣ Formato por defecto PDF
            if (format == null || format.isBlank()) {
                format = "pdf";
            }
            format = format.toLowerCase();

            // 4️⃣ Validar formatos permitidos
            if (!List.of("pdf", "doc", "docx", "odt").contains(format)) {
                return ResponseEntity.badRequest().body("Formato no válido. Use pdf, doc, docx o odt");
            }

            // 5️⃣ Convertir JSON a Map de parámetros
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> parameters = mapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {});

            // 6️⃣ Cargar plantilla Jasper desde memoria
            ByteArrayInputStream plantillaStream = new ByteArrayInputStream(plantilla.getContenido());

            // 7️⃣ Llenar el reporte usando parámetros
            JasperPrint jasperPrint = JasperFillManager.fillReport(plantillaStream, parameters, new JREmptyDataSource());

            byte[] reportBytes;
            String mimeType;
            String fileExtension;

            // 8️⃣ Exportar en el formato indicado
            switch (format) {
                case "pdf":
                    reportBytes = JasperExportManager.exportReportToPdf(jasperPrint);
                    mimeType = "application/pdf";
                    fileExtension = ".pdf";
                    break;

                case "docx":
                    ByteArrayOutputStream docxOut = new ByteArrayOutputStream();
                    JRDocxExporter exporterDocx = new JRDocxExporter();
                    exporterDocx.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporterDocx.setExporterOutput(new SimpleOutputStreamExporterOutput(docxOut));
                    exporterDocx.exportReport();
                    reportBytes = docxOut.toByteArray();
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    fileExtension = ".docx";
                    break;

                case "doc":
                    ByteArrayOutputStream docOut = new ByteArrayOutputStream();
                    JRRtfExporter exporterDoc = new JRRtfExporter();
                    exporterDoc.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporterDoc.setExporterOutput(new SimpleWriterExporterOutput(docOut));
                    exporterDoc.exportReport();
                    reportBytes = docOut.toByteArray();
                    mimeType = "application/msword";
                    fileExtension = ".doc";
                    break;

                case "odt":
                    ByteArrayOutputStream odtOut = new ByteArrayOutputStream();
                    JROdtExporter exporterOdt = new JROdtExporter();
                    exporterOdt.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporterOdt.setExporterOutput(new SimpleOutputStreamExporterOutput(odtOut));
                    exporterOdt.exportReport();
                    reportBytes = odtOut.toByteArray();
                    mimeType = "application/vnd.oasis.opendocument.text";
                    fileExtension = ".odt";
                    break;

                default:
                    return ResponseEntity.badRequest().body("Formato no soportado");
            }

            // 9️⃣ Devolver el archivo generado
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=reporte" + fileExtension)
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(reportBytes);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al generar reporte: " + e.getMessage());
        }
    }
}
