package com.example.demo.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Plantilla;

public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {
    List<Plantilla> findByNombreBaseAndExtensionOrderByVersionDesc(String nombreBase, String extension);
	int countByNombreBaseAndExtension(String nombreBase, String extension);
	List<Plantilla> findByNombreBase(String nombreBase);
    Optional<Plantilla> findTopByNombreBaseAndExtensionOrderByVersionDesc(String nombreBase, String extension);
    Optional<Plantilla> findByNombreArchivo(String nombreArchivo);
    void deleteByNombreArchivo(String nombreArchivo);

}
