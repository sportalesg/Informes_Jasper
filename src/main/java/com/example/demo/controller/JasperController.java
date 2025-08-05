package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.example.demo.model.Plantilla;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.example.demo.service.PlantillaService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;


@RestController
@RequestMapping("/")
public class JasperController {
	
	@GetMapping("/plantillasv1")
	public ResponseEntity<String> listarPlantillas() {
	    try {
	        RestTemplate restTemplate = new RestTemplate();
	        String nodeUrl = "http://localhost:3000/get-all-templates";

	        ResponseEntity<String> response = restTemplate.getForEntity(nodeUrl, String.class);
	        return ResponseEntity.ok(response.getBody());

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al obtener las plantillas");
	    }
	}
	
	@PostMapping("/plantillasv1/subir")
	public ResponseEntity<String> subirPlantilla(@RequestParam("template") MultipartFile file) {
	    try {
	        String nodeUrl = "http://localhost:3000/upload-template";

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

	        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
	        body.add("template", new MultipartInputStreamFileResource(
	                file.getInputStream(), file.getOriginalFilename(), file.getSize()
	        ));

	        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
	        RestTemplate restTemplate = new RestTemplate();

	        ResponseEntity<String> response = restTemplate.postForEntity(nodeUrl, request, String.class);
	        return ResponseEntity.ok(response.getBody());

	    } catch (HttpClientErrorException e) {
	    	// Devuelve solo el cuerpo del error tal cual (ya limpio)
	        String errorMessage = e.getResponseBodyAsString();

	        // Mostrar en consola opcional
	        System.err.println(errorMessage);

	        return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.TEXT_PLAIN).body(errorMessage);
	    }catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al subir la plantilla");
	    }
	}

	
	@PostMapping("/plantillasv1/eliminar")
	public ResponseEntity<String> eliminarPlantilla(@RequestBody Map<String, String> request) {
	    try {
	        RestTemplate restTemplate = new RestTemplate();
	        String nodeUrl = "http://localhost:3000/delete-template";

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);

	        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
	        ResponseEntity<String> response = restTemplate.postForEntity(nodeUrl, entity, String.class);

	        return ResponseEntity.ok(response.getBody());

	    }
	    catch (HttpClientErrorException e) {
	    	// Devuelve solo el cuerpo del error tal cual (ya limpio)
	        String errorMessage = e.getResponseBodyAsString();

	        // Mostrar en consola opcional
	        System.err.println(errorMessage);

	        return ResponseEntity
	        	    .status(e.getStatusCode())
	        	    .contentType(MediaType.TEXT_PLAIN)
	        	    .body(errorMessage);
	    }
	    catch (HttpServerErrorException e) {
	    	// Devuelve solo el cuerpo del error tal cual (ya limpio)
	        String errorMessage = e.getResponseBodyAsString();

	        // Mostrar en consola opcional
	        System.err.println(errorMessage);

	        return ResponseEntity
	        	    .status(e.getStatusCode())
	        	    .contentType(MediaType.TEXT_PLAIN)
	        	    .body(errorMessage);
	    }catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al eliminar la plantilla");
	    }
	}


	
	@PostMapping("/generar-informe")
	public ResponseEntity<?> generarInformeDesdeCarbone(@RequestBody Map<String, Object> datos) {
	    try {
	        RestTemplate restTemplate = new RestTemplate();

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);

	        HttpEntity<Map<String, Object>> request = new HttpEntity<>(datos, headers);

	        ResponseEntity<byte[]> response = restTemplate.postForEntity(
	            "http://localhost:3000/convert", request, byte[].class
	        );

	        HttpHeaders nodeHeaders = new HttpHeaders();
	        nodeHeaders.setContentType(response.getHeaders().getContentType());

	        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
	        if (contentDisposition != null) {
	            nodeHeaders.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
	        }

	        return new ResponseEntity<>(response.getBody(), nodeHeaders, HttpStatus.OK);

	    } catch (HttpServerErrorException e) {
	    	// Devuelve solo el cuerpo del error tal cual (ya limpio)
	        String errorMessage = e.getResponseBodyAsString();

	        // Mostrar en consola opcional
	        System.err.println(errorMessage);

	        return ResponseEntity
	        	    .status(e.getStatusCode())
	        	    .contentType(MediaType.TEXT_PLAIN)
	        	    .body(errorMessage);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body("Error interno en el backend Java: " + e.getMessage());
	    }
	}
	
	@PostMapping("/convert")
	public ResponseEntity<byte[]> generarDocumento(
	    @RequestParam(value = "nombrePlantilla", required = false) String nombrePlantilla,
	    @RequestParam(value = "data", required = false) String jsonData,
	    @RequestParam(value = "formato", required = false) String formato,
	    @RequestParam(value = "nombreExportar", required = false) String nombreExportar
	) {
	    try {
	        if (nombrePlantilla == null || jsonData == null || formato == null) {
	            String msg = "Faltan parámetros obligatorios: ";
	            if (nombrePlantilla == null) msg += "nombrePlantilla ";
	            if (jsonData == null) msg += "data ";
	            if (formato == null) msg += "formato ";
	            return ResponseEntity.badRequest()
	                .contentType(MediaType.TEXT_PLAIN)
	                .body(msg.getBytes(StandardCharsets.UTF_8));
	        }

	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Object> params = mapper.readValue(jsonData, Map.class);
	        JasperReport jasperReport;
	        Set<String> parametros = new HashSet<>();

	        String rutaJasper = "C:/Users/sportalesg/carbone/templates/jasper/";
	        String rutaJrxml  = "C:/Users/sportalesg/carbone/templates/jrxml/";
	        File plantillaFile;

	        if (nombrePlantilla.endsWith(".jasper")) {
	            plantillaFile = new File(rutaJasper + nombrePlantilla);
	            if (!plantillaFile.exists()) {
	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .contentType(MediaType.TEXT_PLAIN)
	                    .body(("La plantilla compilada " + nombrePlantilla + " no existe").getBytes(StandardCharsets.UTF_8));
	            }

	            jasperReport = (JasperReport) JRLoader.loadObject(plantillaFile);

	        } else if (nombrePlantilla.endsWith(".jrxml")) {
	            plantillaFile = new File(rutaJrxml + nombrePlantilla);
	            if (!plantillaFile.exists()) {
	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .contentType(MediaType.TEXT_PLAIN)
	                    .body(("La plantilla " + nombrePlantilla + " no existe").getBytes(StandardCharsets.UTF_8));
	            }

	            FileInputStream paramStream = new FileInputStream(plantillaFile);
	            parametros = extraerParametrosDesdeJrxml(paramStream);

	            FileInputStream compileStream = new FileInputStream(plantillaFile);
	            jasperReport = JasperCompileManager.compileReport(compileStream);

	        } else {
	            return ResponseEntity.badRequest()
	                .contentType(MediaType.TEXT_PLAIN)
	                .body(("Formato de plantilla no soportado: " + nombrePlantilla).getBytes(StandardCharsets.UTF_8));
	        }

	        Map<String, Object> datosCompletos = completarParametrosFaltantes(params, parametros);

	        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, datosCompletos, new JREmptyDataSource());

	        if (nombreExportar == null || nombreExportar.isBlank()) {
	            nombreExportar = "informe";
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        MediaType contentType;
	        String fileName;

	        switch (formato.toLowerCase()) {
	            case "pdf":
	                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
	                contentType = MediaType.APPLICATION_PDF;
	                fileName = nombreExportar + ".pdf";
	                break;
	            case "docx":
	                JRDocxExporter docxExporter = new JRDocxExporter();
	                docxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                docxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                docxExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".docx";
	                break;
	            case "xlsx":
	                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
	                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                xlsxExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".xlsx";
	                break;
	            case "odt":
	                JROdtExporter odtExporter = new JROdtExporter();
	                odtExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                odtExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                odtExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".odt";
	                break;
	            case "ods":
	                JROdsExporter odsExporter = new JROdsExporter();
	                odsExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                odsExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                odsExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".ods";
	                break;
	            default:
	                return ResponseEntity.badRequest()
	                    .contentType(MediaType.TEXT_PLAIN)
	                    .body(("Formato de exportación no soportado: " + formato).getBytes(StandardCharsets.UTF_8));
	        }

	        return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
	            .contentType(contentType)
	            .body(outputStream.toByteArray());

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .contentType(MediaType.TEXT_PLAIN)
	            .body(("Error al generar el informe: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
	    }
	}
	
	/*2 VERSION DEL CONVERT QUE UTILIZA LA BASE DE DATOS*/
	
	@Autowired
	private PlantillaService plantillaService;

	@PostMapping("/convert2")
	public ResponseEntity<byte[]> generarDocumento2(
	    @RequestParam(value = "nombrePlantilla", required = false) String nombrePlantilla,
	    @RequestParam(value = "data", required = false) String jsonData,
	    @RequestParam(value = "formato", required = false) String formato,
	    @RequestParam(value = "nombreExportar", required = false) String nombreExportar
	) {
	    try {
	        if (nombrePlantilla == null || jsonData == null || formato == null) {
	            String msg = "Faltan parámetros obligatorios: ";
	            if (nombrePlantilla == null) msg += "nombrePlantilla ";
	            if (jsonData == null) msg += "data ";
	            if (formato == null) msg += "formato ";
	            return ResponseEntity.badRequest()
	                .contentType(MediaType.TEXT_PLAIN)
	                .body(msg.getBytes(StandardCharsets.UTF_8));
	        }

	        ObjectMapper mapper = new ObjectMapper();
	        Map<String, Object> params = mapper.readValue(jsonData, Map.class);
	        JasperReport jasperReport;
	        Set<String> parametros = new HashSet<>();

	        Optional<Plantilla> plantillaOpt = plantillaService.buscarPorNombreArchivo(nombrePlantilla);
	        if (plantillaOpt.isEmpty()) {
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                .contentType(MediaType.TEXT_PLAIN)
	                .body(("No se encontró la plantilla " + nombrePlantilla).getBytes(StandardCharsets.UTF_8));
	        }

	        Plantilla plantilla = plantillaOpt.get();
	        byte[] contenido = plantilla.getContenido();

	        if (nombrePlantilla.endsWith(".jasper")) {
	            InputStream input = new ByteArrayInputStream(contenido);
	            jasperReport = (JasperReport) JRLoader.loadObject(input);

	        } else if (nombrePlantilla.endsWith(".jrxml")) {
	            InputStream paramStream = new ByteArrayInputStream(contenido);
	            parametros = extraerParametrosDesdeJrxml(paramStream);

	            InputStream compileStream = new ByteArrayInputStream(contenido);
	            jasperReport = JasperCompileManager.compileReport(compileStream);

	        } else {
	            return ResponseEntity.badRequest()
	                .contentType(MediaType.TEXT_PLAIN)
	                .body(("Formato de plantilla no soportado: " + nombrePlantilla).getBytes(StandardCharsets.UTF_8));
	        }

	        Map<String, Object> datosCompletos = completarParametrosFaltantes(params, parametros);
	        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, datosCompletos, new JREmptyDataSource());

	        if (nombreExportar == null || nombreExportar.isBlank()) {
	            nombreExportar = "informe";
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        MediaType contentType;
	        String fileName;

	        switch (formato.toLowerCase()) {
	            case "pdf":
	                JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
	                contentType = MediaType.APPLICATION_PDF;
	                fileName = nombreExportar + ".pdf";
	                break;
	            case "docx":
	                JRDocxExporter docxExporter = new JRDocxExporter();
	                docxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                docxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                docxExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".docx";
	                break;
	            case "xlsx":
	                JRXlsxExporter xlsxExporter = new JRXlsxExporter();
	                xlsxExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                xlsxExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                xlsxExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".xlsx";
	                break;
	            case "odt":
	                JROdtExporter odtExporter = new JROdtExporter();
	                odtExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                odtExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                odtExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".odt";
	                break;
	            case "ods":
	                JROdsExporter odsExporter = new JROdsExporter();
	                odsExporter.setExporterInput(new SimpleExporterInput(jasperPrint));
	                odsExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
	                odsExporter.exportReport();
	                contentType = MediaType.APPLICATION_OCTET_STREAM;
	                fileName = nombreExportar + ".ods";
	                break;
	            default:
	                return ResponseEntity.badRequest()
	                    .contentType(MediaType.TEXT_PLAIN)
	                    .body(("Formato de exportación no soportado: " + formato).getBytes(StandardCharsets.UTF_8));
	        }

	        return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
	            .contentType(contentType)
	            .body(outputStream.toByteArray());

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .contentType(MediaType.TEXT_PLAIN)
	            .body(("Error al generar el informe: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
	    }
	}



	
	public Set<String> extraerParametrosDesdeJrxml(InputStream jrxmlInputStream) throws Exception {
	    Set<String> parametros = new HashSet<>();

	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    Document doc = builder.parse(jrxmlInputStream);

	    NodeList paramNodes = doc.getElementsByTagName("parameter");

	    for (int i = 0; i < paramNodes.getLength(); i++) {
	        Element paramElement = (Element) paramNodes.item(i);
	        String nombre = paramElement.getAttribute("name");
	        // Excluir parámetros internos (si aplica)
	        if (!"REPORT_PARAMETERS_MAP".equals(nombre)) {
	            parametros.add(nombre);
	        }
	    }

	    return parametros;
	}
	
	public Map<String, Object> completarParametrosFaltantes(
		    Map<String, Object> datos, Set<String> parametrosEsperados
		) {
		    Map<String, Object> resultado = new HashMap<>(datos);

		    for (String param : parametrosEsperados) {
		        Object valor = resultado.get(param);

		        if (valor == null || (valor instanceof String && ((String) valor).isBlank())) {
		            // Usa una heurística si conoces algunos parámetros fijos
		            if (param.toLowerCase().contains("fecha")) {
		                
		            	resultado.put(param, null); // usar fecha actual
		            } else {
		                resultado.put(param, "No se ha proporcionado información de " + param);
		                System.out.println("No se ha proporcionado información de " + param);
		            }
		        }
		    }

		    return resultado;
		}
	
	//VERSION 1 PARA QUE COMPILE LOS .JRXML DESDE EL SISTEMA
	@PostConstruct
	public static void precargarJrxmls() {
		String rutaJrxml="C:/Users/sportalesg/carbone/templates/jrxml/";
	    System.out.println("🔄 Precargando y compilando archivos .jrxml desde: " + rutaJrxml);

	    File jrxmlDir = new File(rutaJrxml);
	    File jasperDir = new File("C:/Users/sportalesg/carbone/templates/jasper/");

	    if (!jrxmlDir.exists() || !jrxmlDir.isDirectory()) {
	        System.err.println("❌ La ruta de los .jrxml no es válida: " + rutaJrxml);
	        return;
	    }

	    // Crear carpeta de salida si no existe
	    if (!jasperDir.exists()) {
	        if (jasperDir.mkdirs()) {
	            System.out.println("📁 Carpeta 'jasper' creada: " + jasperDir.getAbsolutePath());
	        } else {
	            System.err.println("⚠️ No se pudo crear la carpeta 'jasper'");
	            return;
	        }
	    }

	    File[] archivos = jrxmlDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jrxml"));
	    if (archivos == null || archivos.length == 0) {
	        System.out.println("ℹ️ No se encontraron archivos .jrxml");
	        return;
	    }

	    for (File archivo : archivos) {
	        try {
	            String nombreSinExtension = archivo.getName().replaceAll("\\.jrxml$", "");
	            String rutaJasper = new File(jasperDir, nombreSinExtension + ".jasper").getAbsolutePath();

	            JasperCompileManager.compileReportToFile(archivo.getAbsolutePath(), rutaJasper);
	            System.out.println("✅ Compilado y guardado: " + rutaJasper);
	        } catch (JRException e) {
	            System.err.println("Error compilando " + archivo.getName() + ": " + e.getMessage());
	        }
	    }
	}
	
	@PreDestroy
	public void eliminarJasperCompilados() {
	    File jasperDir = new File("C:/Users/sportalesg/carbone/templates/jasper/");
	    if (jasperDir.exists() && jasperDir.isDirectory()) {
	        File[] jasperFiles = jasperDir.listFiles((dir, name) -> name.endsWith(".jasper"));
	        if (jasperFiles != null) {
	            for (File f : jasperFiles) {
	                if (f.delete()) {
	                    System.out.println("🗑️ Eliminado: " + f.getName());
	                } else {
	                    System.err.println("⚠️ No se pudo eliminar: " + f.getName());
	                }
	            }
	        }
	    }
	}

	//VERSION 2 QUE PREGARGA LOS .JRXML EN .JASPER Y USA LOS QUE HAYA EN LA BASE DE DATOS
	// Cache en memoria: nombreArchivo -> JasperReport compilado
    private final Map<String, JasperReport> cacheReportes = new ConcurrentHashMap<>();

    @PostConstruct
    public void inicializarCache() {
        System.out.println("🔄 Precargando y compilando plantillas JRXML desde base de datos...");

        // Buscar todas las plantillas .jrxml
        List<Plantilla> jrxmls = plantillaService.listarTodas().stream()
            .filter(p -> p.getExtension().equalsIgnoreCase("jrxml"))
            .toList();

        for (Plantilla plantilla : jrxmls) {
            try (InputStream is = new ByteArrayInputStream(plantilla.getContenido())) {
                JasperReport jasperReport = JasperCompileManager.compileReport(is);
                cacheReportes.put(plantilla.getNombreArchivo(), jasperReport);
                System.out.println("✅ Compilado y cacheado: " + plantilla.getNombreArchivo());
            } catch (Exception e) {
                System.err.println("❌ Error compilando " + plantilla.getNombreArchivo() + ": " + e.getMessage());
            }
        }
    }

    @PreDestroy
    public void limpiarCache() {
        System.out.println("🧹 Limpiando cache de reportes compilados...");
        cacheReportes.clear();
    }



}
