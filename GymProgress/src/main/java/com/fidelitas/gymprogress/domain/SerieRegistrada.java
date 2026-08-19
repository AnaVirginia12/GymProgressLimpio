package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Una serie concreta ejecutada durante una sesión.
 * Almacena ejercicio, peso usado, repeticiones completadas,
 * fecha/hora exacta y si se llegó a fallo muscular.
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "serie_registrada") //fija el nombre exacto de la tabla
public class SerieRegistrada {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    //@ManyToOne "muchas series pertenencen a esta sesión". Este es el lado dueño de la relación, acá está la llave foránea
    @ManyToOne(fetch = FetchType.LAZY, optional = false) //cuando se trae la serie de la base, no se trae la sesión, deja un objeto vacío en su lugar, y se busca solo si alguien llama a 'getSesion()'
    //optional = false: toda serie debe pertenecer a alguna sesión, información para hibernate
    @JoinColumn(name = "sesion_id", nullable = false) //nombra la columna que trae la llave foránea. se llama 'sesion_id' y no acepta nulos
    //"optional" es para hibernate y "nullable" es para la base de datos
    private Sesion sesion;
    
    /**
     * el ejercicio se guarda 2 veces por una razón, el id del ejercicio y el nombre copiado
     * la idea es desnormalizar a propósito
     * si el ejercicio se borra o cambia de nombre debería mostrar el historial el nombre original o no romperse
     */

    //Id del ejercicio al que pertenece esta serie. 
    @Column(name = "ejercicio_id", nullable = false) //es obligatorio
    private Long ejercicioId;

    //Nombre del ejercicio
    @Column(name = "ejercicio_nombre", length = 120)
    private String ejercicioNombre;

    //Número de serie dentro del ejercicio (1, 2, 3…)
    @Column(name = "numero_serie", nullable = false)
    private Integer numeroSerie;
    //si se hacen 4 series de press banca, son 1, 2, 3, 4.

    //Peso levantado en kg (siempre almacenado en kg)
    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg; //double porque hay discos de 2.5kg, 1.25kg...
    //siempre se guarda en kg, aunque se configure con lb es solo para mostrar, dentro se guarda en kg
    //si la fila guardara su propia unidad, al sumar el volumen del entrenamiento no sería correcto

    //Repeticiones completadas
    @Column(nullable = false)
    private Integer repeticiones;

    /**
     * Marcar serie como fallo muscular:
     * true si el usuario agotó completamente el músculo en esta serie.
     */
    @Column(name = "fallo_muscular", nullable = false)
    private Boolean falloMuscular = false;
    //deloadService cuenta esto

    @Column(name = "registrada_en", nullable = false)
    private LocalDateTime registradaEn = LocalDateTime.now();
    //hora exacta del registro 

    //constructor vacío
    public SerieRegistrada() {
    }

    //setters n getters
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

    //Volumen = peso × repeticiones
    @Transient
     /**
     * anotación @Transient: le dice a Hibernate: "ignorá esto, no le busques
     * una columna" marca lo que se calcula al vuelo en vez de guardarse
     */
    public Double getVolumen() {
        if (pesoKg == null || repeticiones == null) return 0.0;
        return pesoKg * repeticiones;
    }
    //esto calcula el volumen, base del análisis del deload
    
    /**
     * la relación es bidireccional, el lado
     * dueño es 'SerieRegistrada' (ahí está la llave foránea 'sesion_id'), y
     * 'Sesion' es el lado inverso, marcado con 'mappedBy'. El método
     * 'agregarSerie()' mantiene las dos puntas sincronizadas, que es la forma
     * recomendada de manejar relaciones bidireccionales en JPA.
     */
}
