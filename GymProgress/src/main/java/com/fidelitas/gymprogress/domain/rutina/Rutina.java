package com.fidelitas.gymprogress.domain.rutina;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "rutina") //fija el nombre exacto de la tabla
public class Rutina {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    @Column(name = "usuario_id", nullable = false) //sin un unique porque un usuario puede tener varias rutinas guardadas
    private Long usuarioId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo; //dónde entrenas (gimnasio, casa)

    @Column(nullable = false, length = 80)
    private String programa; //enfoque del entrenamiento (fuerza, hipertrofia, resistencia)
    /**
     * este es el campo que determina las repeticiones y los descansos de todos los ejercicios.
     * 'RutinaService.seleccionarPrograma()' lee este campo y reajusta la lista
     */

    @Column(nullable = false)
    private Boolean activa = true;
    //activa es la que decide cuál es la "rutina de hoy"
    //un usuario puede tener varias rutinas guardadas pero solo una está en un uso

    //fecha de creación
    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @OneToMany(
        mappedBy = "rutina", //el dueño es 'RutinaEjercicio.rutina', ahí está la columna 'rutina_id'
        cascade = CascadeType.ALL, //guardar la rutina guarda sus ejercicios
        orphanRemoval = true //sacar un ejercicio de la lista lo borra de la base
    )
    @OrderBy("orden ASC") //respeta el orden que el usuario le dio
    private List<RutinaEjercicio> ejercicios = new ArrayList<>();

    //constructor vacío
    public Rutina() {
    }

    //setters n getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getPrograma() {
        return programa;
    }

    public void setPrograma(String programa) {
        this.programa = programa;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(LocalDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public List<RutinaEjercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<RutinaEjercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }

    //Comodidad: agrega una serie manteniendo la relación bidireccional
    public void agregarEjercicio(RutinaEjercicio rutinaEjercicio) {
        ejercicios.add(rutinaEjercicio);
        rutinaEjercicio.setRutina(this); 
    }

    //el inverso
    public void eliminarEjercicio(RutinaEjercicio rutinaEjercicio) {
        ejercicios.remove(rutinaEjercicio);
        rutinaEjercicio.setRutina(null);
    }
}