package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cambio_ejercicio")
public class CambioEjercicio {

    public static final String OMITIDO = "OMITIDO";
    public static final String SUSTITUIDO = "SUSTITUIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sesion_id", nullable = false)
    private Long sesionId;

    @Column(name = "ejercicio_original_id")
    private Long ejercicioOriginalId;

    @Column(name = "ejercicio_original_nombre", nullable = false, length = 120)
    private String ejercicioOriginalNombre;

    @Column(name = "ejercicio_nuevo_id")
    private Long ejercicioNuevoId;

    @Column(name = "ejercicio_nuevo_nombre", length = 120)
    private String ejercicioNuevoNombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "series_afectadas")
    private Integer seriesAfectadas;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn = LocalDateTime.now();

    public CambioEjercicio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSesionId() {
        return sesionId;
    }

    public void setSesionId(Long sesionId) {
        this.sesionId = sesionId;
    }

    public Long getEjercicioOriginalId() {
        return ejercicioOriginalId;
    }

    public void setEjercicioOriginalId(Long ejercicioOriginalId) {
        this.ejercicioOriginalId = ejercicioOriginalId;
    }

    public String getEjercicioOriginalNombre() {
        return ejercicioOriginalNombre;
    }

    public void setEjercicioOriginalNombre(String ejercicioOriginalNombre) {
        this.ejercicioOriginalNombre = ejercicioOriginalNombre;
    }

    public Long getEjercicioNuevoId() {
        return ejercicioNuevoId;
    }

    public void setEjercicioNuevoId(Long ejercicioNuevoId) {
        this.ejercicioNuevoId = ejercicioNuevoId;
    }

    public String getEjercicioNuevoNombre() {
        return ejercicioNuevoNombre;
    }

    public void setEjercicioNuevoNombre(String ejercicioNuevoNombre) {
        this.ejercicioNuevoNombre = ejercicioNuevoNombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getSeriesAfectadas() {
        return seriesAfectadas;
    }

    public void setSeriesAfectadas(Integer seriesAfectadas) {
        this.seriesAfectadas = seriesAfectadas;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }

    public void setRegistradoEn(LocalDateTime registradoEn) {
        this.registradoEn = registradoEn;
    }

    @Transient
    public boolean esOmision() {
        return OMITIDO.equals(tipo);
    }
}
