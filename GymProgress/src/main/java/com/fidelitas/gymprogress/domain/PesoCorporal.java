package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

//esta clase es cada pesada del usuario. Muchas filas por usuario, una por día que pese

@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "peso_corporal") //fija el nombre exacto de la tabla
public class PesoCorporal {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    @Column(name = "usuario_id", nullable = false) //sin unique porque el usuario puede pesarse muchas veces
    private Long usuarioId;

    //Peso almacenado siempre en kilogramos
    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    //fecha que se pesó, sin hora
    @Column(nullable = false)
    private LocalDate fecha;

    //constructor vacío
    public PesoCorporal() {
    }

    //setters n getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    
    // Conversión entre kilogramos y libras
    public static final double FACTOR_LB = 2.20462;
    //sirve para evitar bugs de si estuviese en lugares distintos del código o mal escrito. la constante hace que esté definidido en un lugar y con nombre

    //Devuelve el peso en libras redondeado a 2 decimales
    public Double getPesoLb() {
        if (pesoKg == null) { 
            return null; //si no hay peso devuelve 'null'
        }
        return Math.round(pesoKg * FACTOR_LB * 100.0) / 100.0;
    }

    //Establece el peso a partir de libras convirtiéndolo a kg internamente
    public void setPesoLb(Double pesoLb) {
        if (pesoLb == null) {
            this.pesoKg = null;
        } else {
            this.pesoKg = Math.round((pesoLb / FACTOR_LB) * 100.0) / 100.0;
        }
    }
    /**
     * Este método es el que hace que la regla "todo se guarda en kg" se
     * cumpla sola. Quien lo llama ni se entera de la conversión: manda libras
     * y adentro quedan kilos
     */

    //Devuelve el peso en la unidad preferida del usuario.
    public Double getPesoEnUnidad(String unidad) {
        return "lb".equalsIgnoreCase(unidad) ? getPesoLb() : pesoKg;
    }
    
    /**
     * 'Racha' es la única entidad con
     * 'unique' sobre 'usuario_id', porque la relación con el usuario es de uno a
     * uno. 'PesoCorporal' aplica el principio de guardar siempre en una unidad
     * canónica (kg) y convertir solo en la capa de presentación, así los
     * cálculos y comparaciones nunca mezclan unidades
     */
}
