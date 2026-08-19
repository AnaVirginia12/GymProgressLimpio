package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Usuario;
import com.fidelitas.gymprogress.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    //Autentica al usuario con correo y contraseña.
    public Optional<Usuario> iniciarSesion(String correo, String password) {
        return usuarioRepository.findByCorreo(correo)
                //findByCorreo devuelve un Optional<Usuario>, o sea la caja que
                //tiene el usuario adentro o está vacía
                .filter(u -> u.getPassword().equals(password))
                //filter aplica la condición si la caja tiene algo, y si la
                //condición no se cumple, vacía la caja
                //o sea: "si la contraseña no coincide, hacé de cuenta que no
                //encontré nada"
                .map(u -> {
                    //map transforma lo que hay adentro si todavía queda algo.
                    //acá se aprovecha para generar el token de sesión
                    u.setTokenSesion(UUID.randomUUID().toString().replace("-", ""));
                    //UUID.randomUUID genera algo como 3f2504e0-4f89-11d3-9a0c
                    //y el replace le saca los guiones, quedan 32 caracteres,
                    //que entran justo en el @Column(length = 64)
                    return usuarioRepository.save(u);
                    //devuelve el usuario ya guardado, que es la instancia que
                    //hibernate está gestionando
                });
        /**
         * los dos casos de error terminan igual, con la caja vacía: que el
         * correo no exista, y que exista pero la contraseña no coincida
         *
         * eso es a propósito. el controlador no puede distinguirlos, así que
         * muestra un único mensaje "correo o contraseña incorrectos". si dijera
         * "ese correo no existe" en un caso, cualquiera podría averiguar qué
         * correos están registrados probando de a uno
         */
        /**
         * punto flojo del proyecto: la comparación es con equals directo, o sea
         * que la contraseña está guardada en texto plano. lo correcto sería
         * guardar un hash y comparar con passwordEncoder.matches(), agregando
         * spring-boot-starter-security y BCryptPasswordEncoder
         */
    }

    //Registra un usuario nuevo con los datos de la encuesta de onboarding.
     
    public Usuario registrar(Usuario usuario) {
        //isPresent devuelve true si la caja tiene algo, o sea "si ya existe
        //alguien con ese correo..."
        if (usuarioRepository.findByCorreo(usuario.getCorreo()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado.");
            //throw corta el método de golpe, y el controlador lo atrapa con un
            //try/catch y lo muestra como mensaje rojo
        }
        /**
         * es una doble defensa a propósito: 'Usuario.correo' ya tiene
         * unique = true, así que la base también impediría el duplicado
         *
         * ¿por qué comprobarlo acá entonces? por el mensaje. sin esta línea el
         * duplicado igual fallaría, pero con una excepción fea de hibernate que
         * el usuario vería como pantalla de error 500
         *
         * la de java da buen mensaje y la de la base garantiza que no pase ni
         * aunque dos personas se registren en el mismo milisegundo
         */
        return usuarioRepository.save(usuario);
    }

    //Cierra la sesión del usuario limpiando su token.
     
    public void cerrarSesion(Long usuarioId) {
        //ifPresent quiere decir "si hay algo adentro hacé esto, y si no, no
        //hagas nada"
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            u.setTokenSesion(null);
            usuarioRepository.save(u);
        });
        //no toca la HttpSession, eso lo hace el controlador con
        //session.invalidate(). el servicio no sabe nada de HTTP
    }

    //Actualiza los datos editables del perfil (nombre, foto, objetivo,días, minutos, equipamiento, lesiones, unidadPeso).
     
    public Usuario actualizarPerfil(Long usuarioId, Usuario datosNuevos) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        //orElseThrow lanza el error si no existe. acá sí es un problema de
        //verdad, significa que la sesión apunta a alguien borrado
        //la lambda solo se ejecuta si la caja está vacía, así que la excepción
        //ni se crea cuando todo va bien

        /**
         * de acá para abajo es una actualización parcial, y la regla es la
         * misma en todos los bloques: si el campo nuevo no es null lo pisa, y
         * si es null deja el que estaba
         *
         * hace falta porque si el formulario manda solo la foto, todos los
         * demás campos llegan en null, y sin estos if guardar ese objeto
         * borraría el nombre, el objetivo, el nivel y todo lo demás
         */

        if (datosNuevos.getNombre() != null) {
            u.setNombre(datosNuevos.getNombre());
        }
        if (datosNuevos.getFotoUrl() != null) {
            u.setFotoUrl(datosNuevos.getFotoUrl());
        }
        if (datosNuevos.getObjetivo() != null) {
            u.setObjetivo(datosNuevos.getObjetivo());
        }
        if (datosNuevos.getNivel() != null) {
            u.setNivel(datosNuevos.getNivel());
        }
        if (datosNuevos.getDiasSemana() != null) {
            u.setDiasSemana(datosNuevos.getDiasSemana());
        }
        if (datosNuevos.getMinutosSesion() != null) {
            u.setMinutosSesion(datosNuevos.getMinutosSesion());
        }
        if (datosNuevos.getEquipamiento() != null) {
            u.setEquipamiento(datosNuevos.getEquipamiento());
        }
        if (datosNuevos.getLesiones() != null) {
            u.setLesiones(datosNuevos.getLesiones());
        }
        if (datosNuevos.getUnidadPeso() != null) {
            u.setUnidadPeso(datosNuevos.getUnidadPeso());
        }
        if (datosNuevos.getTema() != null) {
            u.setTema(datosNuevos.getTema());
        }
        if (datosNuevos.getDescansoAutomatico() != null) {
            u.setDescansoAutomatico(datosNuevos.getDescansoAutomatico());
        }
        if (datosNuevos.getDescansoPorDefectoSeg() != null) {
            u.setDescansoPorDefectoSeg(datosNuevos.getDescansoPorDefectoSeg());
        }

        /**
         * fijarse en qué campos no están en la lista: id, correo, password,
         * creadoEn y tokenSesion
         *
         * eso es deliberado y es seguridad. si 'correo' estuviera, alguien
         * podría cambiarse el correo al de otra persona, y si 'password'
         * estuviera se podría cambiar la contraseña sin conocer la anterior
         *
         * al dejarlos afuera, por más que alguien agregue campos al formulario
         * no pasa nada. ese ataque se llama mass assignment
         */
        return usuarioRepository.save(u);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
        //un método de una línea que solo llama al repositorio, eso se llama
        //delegación. existe para que el controlador hable solo con servicios y
        //nunca con repositorios, así no se rompe la separación en capas
    }
}
