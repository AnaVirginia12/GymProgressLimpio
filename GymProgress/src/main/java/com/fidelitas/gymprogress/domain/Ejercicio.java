package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;

/**
 * un ejercicio del catálogo (press de banca, sentadilla, flexiones...) Cada
 * objeto es una fila de la tabla 'ejercicio'
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "ejercicio") //fija el nombre exacto de la tabla
public class Ejercicio {

    @Id //marca la llave primaria. toda @Entity necesita una
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    @Column(nullable = false, length = 120) //nombre es obligatorio
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    //Grupo muscular principal: Pecho, Espalda, Piernas, Hombros, Bíceps, etc
    @Column(name = "grupo_muscular", nullable = false, length = 80)
    private String grupoMuscular;

    @Column(name = "tipo_entrenamiento", nullable = false, length = 30)
    private String tipoEntrenamiento; // Calistenia, Cardio, Gimnasio

    @Column(name = "media_url", length = 300)
    private String mediaUrl; // URL de imagen o GIF demostrativo

    @Column(nullable = false) // campo obligatorio
    private Boolean favorito = false; //marcar como favorito

    @Column(name = "requiere_equipo", nullable = false) //campo obligatorio
    private Boolean requiereEquipo = true; //naturalmente todos los ejercicios están como sí requiere equipo, pero se puede modificar 

    @Column(name = "tempo_excentrico")
    private Integer tempoExcentrico;

    @Column(name = "tempo_pausa_abajo")
    private Integer tempoPausaAbajo;

    @Column(name = "tempo_concentrico")
    private Integer tempoConcentrico;

    @Column(name = "tempo_pausa_arriba")
    private Integer tempoPausaArriba;

    /**
     * ninguno lleva 'nullable = false'. Eso es a propósito, porque `null`
     * significa "este ejercicio no tiene tempo definido", que es distinto de un
     * tempo de 0 segundos
     */

    //constructor vacío
    public Ejercicio() {
    }

    //setters n getters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public String getTipoEntrenamiento() {
        return tipoEntrenamiento;
    }

    public void setTipoEntrenamiento(String tipoEntrenamiento) {
        this.tipoEntrenamiento = tipoEntrenamiento;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Boolean getFavorito() {
        return favorito;
    }

    public void setFavorito(Boolean favorito) {
        this.favorito = favorito;
    }

    public Boolean getRequiereEquipo() {
        return requiereEquipo;
    }

    public void setRequiereEquipo(Boolean requiereEquipo) {
        this.requiereEquipo = requiereEquipo;
    }

    public Integer getTempoExcentrico() {
        return tempoExcentrico;
    }

    public void setTempoExcentrico(Integer tempoExcentrico) {
        this.tempoExcentrico = tempoExcentrico;
    }

    public Integer getTempoPausaAbajo() {
        return tempoPausaAbajo;
    }

    public void setTempoPausaAbajo(Integer tempoPausaAbajo) {
        this.tempoPausaAbajo = tempoPausaAbajo;
    }

    public Integer getTempoConcentrico() {
        return tempoConcentrico;
    }

    public void setTempoConcentrico(Integer tempoConcentrico) {
        this.tempoConcentrico = tempoConcentrico;
    }

    public Integer getTempoPausaArriba() {
        return tempoPausaArriba;
    }

    public void setTempoPausaArriba(Integer tempoPausaArriba) {
        this.tempoPausaArriba = tempoPausaArriba;
    }

    @Transient
    /**
     * anotación @Transient: le dice a Hibernate: "ignorá esto, no le busques
     * una columna" marca lo que se calcula al vuelo en vezde guardarse
     */
    public String getTempoTexto() {
        if (!tieneTempo()) {
            return null; //si el ejercicio no tiene ningún tempo definido devuelve null, en el html es importante para que no aparezca el bloque de tempo, si no sería un tempo falso
        }
        return valor(tempoExcentrico) + "-" + valor(tempoPausaAbajo)
                + "-" + valor(tempoConcentrico) + "-" + valor(tempoPausaArriba);
    } //arma el texto pegando los 4 números con guiones, cada uno pasa por 'valor()' para que un 'null' se muestre como 0

    @Transient
    public boolean tieneTempo() {
        return tempoExcentrico != null || tempoPausaAbajo != null
                || tempoConcentrico != null || tempoPausaArriba != null;
    } //devuelve 'true' si al menos uno de los 4 está definido

    @Transient
    public int getSegundosPorRepeticion() {
        return valor(tempoExcentrico) + valor(tempoPausaAbajo)
                + valor(tempoConcentrico) + valor(tempoPausaArriba);
    } //suma las cuatro fases: cuánto dura una sola repetición.

    /**
     * Este método es el puente entre el tempo y la duración estimada de la
     * rutina. 'RutinaService.calcularDuracionEstimadaMinutos()' lo usa así:
     * duración ≈ series x repeticiones x segundosPorRepeticion + descansos o
     * sea que el tempo que se carga en un ejercicio termina afectando el tiempo
     * que la aplicación estima para el entrenamiento. Están conectados. Si el
     * ejercicio no tiene tempo, devuelve 0, y el cálculo de duración usa un
     * valor por defecto
     */
    private int valor(Integer v) {
        return v == null ? 0 : v;
    }
} //convierte 'Integer' a 'int' con el operador ternario: "si 'v' es null, 0; si no, 'v'"
//sin este método getSegundosPorRepeticion explotaría con un 'NullPointerException' cuando un ejercicio tuviese algún tempo sin definir

/**
 * Los tres métodos '@Transient' son datos
 * derivados: se calculan a partir de los campos que sí están guardados, así
 * que no ocupan espacio en la base y nunca pueden quedar desactualizados
 * respecto a los datos de los que salen.
 */
