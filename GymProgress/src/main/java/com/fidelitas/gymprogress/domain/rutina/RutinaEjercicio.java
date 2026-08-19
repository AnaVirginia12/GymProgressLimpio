package com.fidelitas.gymprogress.domain.rutina;

import jakarta.persistence.*;


/**
 * Representa un ejercicio específico dentro de una rutina, con sus
 * series, repeticiones, peso sugerido, descanso y tempo de ejecución.
 * Una Rutina tiene varios RutinaEjercicio (uno por cada ejercicio
 * que la compone).
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "rutina_ejercicio") //fija el nombre exacto de la tabla
public class RutinaEjercicio {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

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

    //constructor vacío
    public RutinaEjercicio() {
    }

    //setters n getters
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
    /**
     * 'RutinaEjercicio' es una entidad de asociación: no basta con relacionar rutina y ejercicio, hay que guardar
     * los parámetros de ese ejercicio en esa rutina (series, repeticiones,
     * descanso). El campo 'activa' de 'Rutina' es el que identifica cuál es la
     * rutina en uso, y 'programa' determina los valores de 'repsObjetivo' y
     * 'descansoSeg' de todos sus ejercicios
     */
}