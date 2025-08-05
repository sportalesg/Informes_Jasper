package com.example.demo.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.model.Plantilla;
import com.example.demo.repository.PlantillaRepository;

import jakarta.transaction.Transactional;

@Service
public class PlantillaService {

    @Autowired
    private PlantillaRepository repo;
    
    public List<Plantilla> listarTodas() {
        return repo.findAll();
    }

    @Transactional
    public boolean eliminarPorNombreArchivo(String nombreArchivo) {
        Optional<Plantilla> plantillaOpt = repo.findByNombreArchivo(nombreArchivo);
        if (plantillaOpt.isPresent()) {
        	repo.deleteByNombreArchivo(nombreArchivo);
            return true;
        }
        return false;
    }


    public Plantilla guardarPlantilla(String nombreOriginal, byte[] contenido) {
        String extension = FilenameUtils.getExtension(nombreOriginal);
        String nombreBase = FilenameUtils.getBaseName(nombreOriginal);

        int nuevaVersion = calcularSiguienteVersion(nombreBase, extension);
        String nombreFinal = nombreBase + "_v" + nuevaVersion + "." + extension;

        Plantilla plantilla = new Plantilla();
        plantilla.setNombreBase(nombreBase);
        plantilla.setExtension(extension);
        plantilla.setVersion(nuevaVersion);
        plantilla.setNombreArchivo(nombreFinal);
        plantilla.setContenido(contenido);

        return repo.save(plantilla);
    }

    public int calcularSiguienteVersion(String nombreBase, String extension) {
        return repo.countByNombreBaseAndExtension(nombreBase, extension) + 1;
    }
    public Optional<Plantilla> buscarPorNombreArchivo(String nombreArchivo) {
        return repo.findByNombreArchivo(nombreArchivo);
    }
    
    
    @Transactional
    public void guardarContenidoJasper(Long plantillaId, byte[] jasperBytes) {
        Optional<Plantilla> opt = repo.findById(plantillaId);
        if (opt.isPresent()) {
            Plantilla p = opt.get();
            p.setContenidoJasper(jasperBytes);
            repo.save(p);
        }
    }

    @Transactional
    public void eliminarContenidoJasperTemporales() {
        List<Plantilla> plantillasConJasper = repo.findAll().stream()
            .filter(p -> p.getContenidoJasper() != null)
            .toList();
        for (Plantilla p : plantillasConJasper) {
            p.setContenidoJasper(null);
            repo.save(p);
        }
    }


}
