package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registra cuando un ejercicio de la rutina fue omitido o sustituido
 * durante una sesión de entrenamiento (por ejemplo, si el usuario no
 * tenía la máquina disponible y cambió el ejercicio por otro).
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "cambio_ejercicio") //fija el nombre exacto de la tabla
public class CambioEjercicio {

    public static final String OMITIDO = "OMITIDO";
    public static final String SUSTITUIDO = "SUSTITUIDO";

    @Id  //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    // A qué sesión de entrenamiento pertenece este cambio
    @Column(name = "sesion_id", nullable = false)
    private Long sesionId;

    // Ejercicio que se iba a hacer originalmente
    @Column(name = "ejercicio_original_id")
    private Long ejercicioOriginalId;

    @Column(name = "ejercicio_original_nombre", nullable = false, length = 120) //campo obligatorio
    private String ejercicioOriginalNombre;

    // Ejercicio nuevo con el que se sustituyó
    @Column(name = "ejercicio_nuevo_id")
    private Long ejercicioNuevoId;

    @Column(name = "ejercicio_nuevo_nombre", length = 120)
    private String ejercicioNuevoNombre;
    /**
     * Se guarda dos veces (id y nombre copiado)
     * por la misma razón, si se borra un ejercicio el historial
     * tiene que seguir diciendo que pasó ese día
     */

    //guarda omitido o sustituido 
    @Column(nullable = false, length = 20)
    private String tipo;

    //Cuántas series te perdiste al omitir el ejercicio
    @Column(name = "series_afectadas")
    private Integer seriesAfectadas;
    /**
     * es importante porque 'AjusteRutinaService.omitir()' lo usa para
     * calcular el impacto y decirte algo tipo: "omitiste 4 series, un 20%
     * del volumen de hoy". sin este dato no podría poner números
     */

    //cuando pasó
    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn = LocalDateTime.now();

    //constructor vacío
    public CambioEjercicio() {
    }

    //setters n getters
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

    /**
     * Dice si este cambio fue una omisión (el ejercicio simplemente
     * se saltó) y no una sustitución por otro ejercicio.
     */    
    @Transient
    public boolean esOmision() {
        return OMITIDO.equals(tipo);
    }
    
    /**
     * Guarda los nombres desnormalizados a propósito, para que el historial de un
     * entrenamiento pasado no cambie si después se edita o se borra el ejercicio
     * del catálogo. El campo 'tipo' distingue omisión de sustitución, y en las
     * omisiones los campos del ejercicio nuevo quedan en 'null'.
     */
}
