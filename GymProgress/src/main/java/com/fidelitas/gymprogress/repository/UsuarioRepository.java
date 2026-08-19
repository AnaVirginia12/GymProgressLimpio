package com.fidelitas.gymprogress.repository;

import com.fidelitas.gymprogress.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Los repositorios son interfaces, no clases. No tienen ni una línea escrita
 * adentro y aún así funcionan.
 *
 * Cuando la aplicación arranca, Spring Data genera la clase que implementa
 * esta interfaz al vuelo: lee el nombre de cada método y arma el SQL solo.
 * Por eso no llevan @Repository, los detecta por heredar de JpaRepository
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    //los dos tipos entre <> son: la entidad que maneja, y el tipo de su @Id

    /**
     * con solo extender JpaRepository ya vienen gratis save(), findById(),
     * findAll(), deleteById(), count() y existsById()
     *
     * save() sirve para crear y para actualizar: hibernate decide mirando si
     * el objeto trae id o no
     */

    //findByCorreo se traduce a: SELECT * FROM usuario WHERE correo = ?
    Optional<Usuario> findByCorreo(String correo);
    //devuelve Optional porque puede no existir, y ese "puede no existir" es
    //justo el caso de "correo o contraseña incorrectos" en el login
    /**
     * es el método más importante del proyecto, de él dependen las dos cosas
     * fundamentales: el login busca el usuario por correo y compara la
     * contraseña, y el registro comprueba que ese correo no exista ya
     */

    //igual pero buscando por el token de sesión
    Optional<Usuario> findByTokenSesion(String tokenSesion);
    //está declarado pero nadie lo llama, corresponde al campo 'tokenSesion' de
    //Usuario que quedó preparado para sincronizar entre dispositivos
}
