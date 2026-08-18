package com.fidelitas.gymprogress.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa a un usuario de la aplicación: sus datos de acceso
 * (correo, contraseña) y su perfil de entrenamiento (objetivo,
 * nivel, días disponibles, etc.).
 */
@Entity //convierte esto de una clase de java común en una tabla
@Table(name = "usuario") //fija el nombre exacto de la tabla
public class Usuario {

    @Id //marca la llave primaria. toda @Entity necesita una 
    @GeneratedValue(strategy = GenerationType.IDENTITY) //IDENTITY significa "el número lo pone mysql, no java", cuando un usuario nuevo se hace, se manda en null el id, mysql le asigna el siguiente número libre y lo devuelve
    private Long id; //Long de objeto

    @Column(nullable = false, unique = true, length = 180) //nullable = false: No se puede guardar un usuario sin correo. unique = true: crea un índice único, dos personas no se pueden registrar con el mismo correo
    private String correo;
    /**
     * 'UsuarioService' igual comprueba a mano si el correo ya existe, antes de
     * intentar guardar. Eso es para poder mostrar un mensaje bonito ("Ese
     * correo ya está registrado") en vez de dejar que explote una excepción fea
     * de la base. Las dos defensas se complementan: la de Java da buen mensaje,
     * la de la base garantiza que no pase ni aunque dos personas se registren
     * en el mismo milisegundo
     */

    @Column(nullable = false, length = 255) //con 255 carecteres porque está pensada para que quepa una contraseña cifrada
    private String password; //contraseña

    @Column(nullable = false, length = 100)
    private String nombre; //nombre de usuario

    // URL o path relativo de la foto de perfil 
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    //Objetivo principal: Fuerza, Hipertrofia, Resistencia, etc. 
    @Column(length = 80)
    private String objetivo;

    // Nivel: Principiante, Intermedio, Avanzado 
    @Column(length = 50)
    private String nivel;

    //Días disponibles por semana (1-7) 
    @Column(name = "dias_semana")
    private Integer diasSemana;

    //Minutos disponibles por sesión
    @Column(name = "minutos_sesion")
    private Integer minutosSesion;

    //Equipamiento disponible: Gimnasio, Casa, Al aire libre 
    @Column(length = 100)
    private String equipamiento;

    //Lesiones o limitaciones físicas reportadas 
    @Column(length = 500)
    private String lesiones;

    //Preferencia de unidad: kg o lb 
    @Column(name = "unidad_peso", length = 2, nullable = false)
    private String unidadPeso = "kg";

    //Preferencia de apariencia: dark o light (valores de data-bs-theme)
    @Column(name = "tema", length = 10, nullable = false)
    private String tema = "dark";

    //Si el descanso entre series inicia solo al guardar una serie, o el usuario lo inicia manualmente
    @Column(name = "descanso_automatico", nullable = false)
    private Boolean descansoAutomatico = true;

    //Duración por defecto (segundos) del temporizador de descanso
    @Column(name = "descanso_por_defecto_seg", nullable = false)
    private Integer descansoPorDefectoSeg = 90;

    /**
     * Estos tres campos son de la pantalla de configuración. Los tres llevan
     * 'nullable = false' y a la vez un valor inicial ("dark", true, 90): el
     * valor lo pone Java al hacer 'new Usuario()', así que nunca llegan vacíos
     * a la base y por eso pueden ser obligatorios sin romper el registro
     */

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    //Token de sesión para sincronización entre dispositivos 
    @Column(name = "token_sesion", length = 64, unique = true)
    private String tokenSesion;

    //constructor vacío
    public Usuario() {
    }

    //setters n getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public Integer getDiasSemana() { return diasSemana; }
    public void setDiasSemana(Integer diasSemana) { this.diasSemana = diasSemana; }

    public Integer getMinutosSesion() { return minutosSesion; }
    public void setMinutosSesion(Integer minutosSesion) { this.minutosSesion = minutosSesion; }

    public String getEquipamiento() { return equipamiento; }
    public void setEquipamiento(String equipamiento) { this.equipamiento = equipamiento; }

    public String getLesiones() { return lesiones; }
    public void setLesiones(String lesiones) { this.lesiones = lesiones; }

    public String getUnidadPeso() { return unidadPeso; }
    public void setUnidadPeso(String unidadPeso) { this.unidadPeso = unidadPeso; }

    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }

    public Boolean getDescansoAutomatico() { return descansoAutomatico; }
    public void setDescansoAutomatico(Boolean descansoAutomatico) { this.descansoAutomatico = descansoAutomatico; }

    public Integer getDescansoPorDefectoSeg() { return descansoPorDefectoSeg; }
    public void setDescansoPorDefectoSeg(Integer descansoPorDefectoSeg) { this.descansoPorDefectoSeg = descansoPorDefectoSeg; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public String getTokenSesion() { return tokenSesion; }
    public void setTokenSesion(String tokenSesion) { this.tokenSesion = tokenSesion; }
}

/**
 * esta clase es una entidad, o sea el
 * espejo en Java de una tabla. No tiene lógica: solo datos y sus accesos.
 * Las reglas de negocio (validar el correo, comprobar la contraseña) están
 * en 'UsuarioService', no acá. Esa separación es a propósito
 */
