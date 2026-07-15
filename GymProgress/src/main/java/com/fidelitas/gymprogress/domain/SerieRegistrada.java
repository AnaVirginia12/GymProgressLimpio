package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Una serie concreta ejecutada durante una sesión.
 * Almacena ejercicio, peso usado, repeticiones completadas,
 * fecha/hora exacta y si se llegó a fallo muscular.
 */
@Entity
@Table(name = "serie_registrada")
public class SerieRegistrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private Sesion sesion;

    /** Id del ejercicio al que pertenece esta serie. */
    @Column(name = "ejercicio_id", nullable = false)
    private Long ejercicioId;

    /** Nombre del ejercicio (desnormalizado para facilitar historial). */
    @Column(name = "ejercicio_nombre", length = 120)
    private String ejercicioNombre;

    /** Número de serie dentro del ejercicio (1, 2, 3…). */
    @Column(name = "numero_serie", nullable = false)
    private Integer numeroSerie;

    /** Peso levantado en kg (siempre almacenado en kg). */
    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    /** Repeticiones completadas. */
    @Column(nullable = false)
    private Integer repeticiones;

    /**
     * HU — Marcar serie como fallo muscular:
     * true si el usuario agotó completamente el músculo en esta serie.
     */
    @Column(name = "fallo_muscular", nullable = false)
    private Boolean falloMuscular = false;

    @Column(name = "registrada_en", nullable = false)
    private LocalDateTime registradaEn = LocalDateTime.now();

    public SerieRegistrada() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Sesion getSesion() { return sesion; }
    public void setSesion(Sesion sesion) { this.sesion = sesion; }

    public Long getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(Long ejercicioId) { this.ejercicioId = ejercicioId; }

    public String getEjercicioNombre() { return ejercicioNombre; }
    public void setEjercicioNombre(String ejercicioNombre) { this.ejercicioNombre = ejercicioNombre; }

    public Integer getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(Integer numeroSerie) { this.numeroSerie = numeroSerie; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public Integer getRepeticiones() { return repeticiones; }
    public void setRepeticiones(Integer repeticiones) { this.repeticiones = repeticiones; }

    public Boolean getFalloMuscular() { return falloMuscular; }
    public void setFalloMuscular(Boolean falloMuscular) { this.falloMuscular = falloMuscular; }

    public LocalDateTime getRegistradaEn() { return registradaEn; }
    public void setRegistradaEn(LocalDateTime registradaEn) { this.registradaEn = registradaEn; }

    /** Volumen = peso × repeticiones (en kg). */
    public Double getVolumen() {
        if (pesoKg == null || repeticiones == null) return 0.0;
        return pesoKg * repeticiones;
    }
}
