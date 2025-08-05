package com.example.demo.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Plantilla;
import com.example.demo.service.PlantillaService;

@RestController
@RequestMapping("/plantillas")
public class PlantillaController {

    @Autowired
    private PlantillaService plantillaService;

    @PostMapping("/subir")
    public ResponseEntity<String> subirArchivo(@RequestParam("archivo") MultipartFile archivo) {
        try {
            byte[] contenido = archivo.getBytes();
            String nombreOriginal = archivo.getOriginalFilename();

            Plantilla plantillaGuardada = plantillaService.guardarPlantilla(nombreOriginal, contenido);

            return ResponseEntity.ok("Archivo guardado como: " + plantillaGuardada.getNombreArchivo());

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al leer el archivo");
        }
    }
    @DeleteMapping("/eliminar")
    public ResponseEntity<String> eliminarPorNombreArchivo(@RequestParam String nombreArchivo) {
        boolean eliminado = plantillaService.eliminarPorNombreArchivo(nombreArchivo);
        return eliminado ?
                ResponseEntity.ok("Plantilla eliminada: " + nombreArchivo) :
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró la plantilla: " + nombreArchivo);
    }
    @GetMapping("/listar")
    public ResponseEntity<List<Plantilla>> listar() {
        return ResponseEntity.ok(plantillaService.listarTodas());
    }
    
    
}