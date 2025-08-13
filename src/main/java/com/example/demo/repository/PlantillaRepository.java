package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.interfaces.PlantillaResumen;
import com.example.demo.models.Plantilla;

public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {
    Optional<Plantilla> findByNombre(String nombre);
    List<PlantillaResumen> findAllBy();
    boolean existsByNombre(String nombre);
}
