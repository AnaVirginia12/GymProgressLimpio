package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "peso_corporal")
public class PesoCorporal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Peso almacenado siempre en kilogramos */
    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(nullable = false)
    private LocalDate fecha;

    public PesoCorporal() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    
    // HU3 — Conversión entre kilogramos y libras
   

    public static final double FACTOR_LB = 2.20462;

    /** Devuelve el peso en libras redondeado a 2 decimales. */
    public Double getPesoLb() {
        if (pesoKg == null) {
            return null;
        }
        return Math.round(pesoKg * FACTOR_LB * 100.0) / 100.0;
    }

    /** Establece el peso a partir de libras convirtiéndolo a kg internamente. */
    public void setPesoLb(Double pesoLb) {
        if (pesoLb == null) {
            this.pesoKg = null;
        } else {
            this.pesoKg = Math.round((pesoLb / FACTOR_LB) * 100.0) / 100.0;
        }
    }

    //HU3 — Devuelve el peso en la unidad preferida del usuario.
    
    public Double getPesoEnUnidad(String unidad) {
        return "lb".equalsIgnoreCase(unidad) ? getPesoLb() : pesoKg;
    }
}
