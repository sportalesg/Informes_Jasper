package com.example.demo.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Plantilla {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreBase;     // Sin versión, sin extensión
    private String extension;      // docx, jrxml, etc.
    private int version;           // v1, v2, ...
    private String nombreArchivo;  // nombreBase_v2.extension

    @Lob
    @JsonIgnore
    @Column(columnDefinition = "BLOB")
    private byte[] contenido;

    @Lob
    @Column(name = "contenido_jasper")
    private byte[] contenidoJasper;

    
    
    public byte[] getContenidoJasper() {
		return contenidoJasper;
	}

	public void setContenidoJasper(byte[] contenidoJasper) {
		this.contenidoJasper = contenidoJasper;
	}

	// Getters y setters...
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombreBase() {
		return nombreBase;
	}

	public void setNombreBase(String nombreBase) {
		this.nombreBase = nombreBase;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public void setNombreArchivo(String nombreArchivo) {
		this.nombreArchivo = nombreArchivo;
	}

	public byte[] getContenido() {
		return contenido;
	}

	public void setContenido(byte[] contenido) {
		this.contenido = contenido;
	}


    
}

