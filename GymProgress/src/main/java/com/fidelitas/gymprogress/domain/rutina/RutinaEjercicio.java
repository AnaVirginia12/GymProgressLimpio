package com.fidelitas.gymprogress.domain.rutina;

import jakarta.persistence.*;


/**
 * Representa un ejercicio específico dentro de una rutina, con sus
 * series, repeticiones, peso sugerido, descanso y tempo de ejecución.
 * Una Rutina tiene varios RutinaEjercicio (uno por cada ejercicio
 * que la compone).
 */
@Entity
@Table(name = "rutina_ejercicio")
public class RutinaEjercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;

    @Column(name = "ejercicio_id", nullable = false)
    private Long ejercicioId;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Integer series;

    @Column(name = "reps_objetivo", nullable = false)
    private Integer repsObjetivo;

    @Column(name = "peso_sugerido_kg")
    private Double pesoSugeridoKg;

    @Column(name = "tempo_bajada")
    private Double tempoBajada;

    @Column(name = "tempo_fondo")
    private Double tempoFondo;

    @Column(name = "tempo_subida")
    private Double tempoSubida;

    @Column(name = "tempo_tope")
    private Double tempoTope;

    @Column(name = "descanso_seg", nullable = false)
    private Integer descansoSeg;

    public RutinaEjercicio() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Rutina getRutina() {
        return rutina;
    }

    public void setRutina(Rutina rutina) {
        this.rutina = rutina;
    }

    public Long getEjercicioId() {
        return ejercicioId;
    }

    public void setEjercicioId(Long ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public Integer getRepsObjetivo() {
        return repsObjetivo;
    }

    public void setRepsObjetivo(Integer repsObjetivo) {
        this.repsObjetivo = repsObjetivo;
    }

    public Double getPesoSugeridoKg() {
        return pesoSugeridoKg;
    }

    public void setPesoSugeridoKg(Double pesoSugeridoKg) {
        this.pesoSugeridoKg = pesoSugeridoKg;
    }

    public Double getTempoBajada() {
        return tempoBajada;
    }

    public void setTempoBajada(Double tempoBajada) {
        this.tempoBajada = tempoBajada;
    }

    public Double getTempoFondo() {
        return tempoFondo;
    }

    public void setTempoFondo(Double tempoFondo) {
        this.tempoFondo = tempoFondo;
    }

    public Double getTempoSubida() {
        return tempoSubida;
    }

    public void setTempoSubida(Double tempoSubida) {
        this.tempoSubida = tempoSubida;
    }

    public Double getTempoTope() {
        return tempoTope;
    }

    public void setTempoTope(Double tempoTope) {
        this.tempoTope = tempoTope;
    }

    public Integer getDescansoSeg() {
        return descansoSeg;
    }

    public void setDescansoSeg(Integer descansoSeg) {
        this.descansoSeg = descansoSeg;
    }
}