package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.PesoCorporal;
import com.fidelitas.gymprogress.repository.PesoCorporalRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service //spring crea un objeto de esta clase al arrancar y lo guarda para quien lo necesite
public class PesoCorporalService {

    private final PesoCorporalRepository pesoCorporalRepository;
    //final quiere decir que se asigna una sola vez, en el constructor, y ya
    //nadie lo puede cambiar

    //la clase pide por el constructor lo que necesita y spring se lo pasa, eso
    //se llama inyección de dependencias. por eso en ningún lado del proyecto
    //hay un "new PesoCorporalService()"
    public PesoCorporalService(PesoCorporalRepository pesoCorporalRepository) {
        this.pesoCorporalRepository = pesoCorporalRepository;
    }

    /**
     * Guarda el registro de peso del día para el usuario. El registro
     * periódico es diario: si ya existía uno para hoy, se actualiza
     * en vez de duplicarlo.
     */
    public PesoCorporal guardar(Long usuarioId, Double valorPeso, String unidad) {
        LocalDate hoy = LocalDate.now();
        //la fecha la pone el servidor, no el formulario, así nadie puede
        //registrar un peso con fecha del año pasado manipulando la petición
        PesoCorporal registro = pesoCorporalRepository
                .findByUsuarioIdAndFecha(usuarioId, hoy)
                .orElseGet(PesoCorporal::new);
        /**
         * si ya existe un registro de hoy se reutiliza esa fila en vez de crear
         * otra, o sea que pesarse dos veces el mismo día actualiza el valor y
         * no agrega un segundo punto. así el gráfico queda limpio, un punto
         * por día
         */
        //el orElseGet crea uno nuevo solo si no había

        registro.setUsuarioId(usuarioId);
        registro.setFecha(hoy);

        /**
         * acá está la aplicación práctica de la regla "todo se guarda en kg"
         *
         * el usuario puede tener el perfil en libras, y si escribe 165 son
         * 165 libras, no 165 kilos. este if elige el setter correcto:
         *   setPesoLb(165) adentro convierte y guarda 74.84 kg
         *   setPesoKg(165) guarda 165 kg
         *
         * toda la conversión está en la entidad, no acá. este servicio no sabe
         * cuánto vale una libra, solo decide qué setter llamar
         */
        if ("lb".equalsIgnoreCase(unidad)) {
            //la constante va a la izquierda a propósito: si 'unidad' fuera
            //null, escrito al revés reventaría con NullPointerException
            registro.setPesoLb(valorPeso);
        } else {
            registro.setPesoKg(valorPeso);
            //el else cubre null, "kg" o cualquier cosa rara, por defecto kilos
        }

        return pesoCorporalRepository.save(registro);
    }

    //Devuelve el historial de peso del usuario ordenado por fecha descendente.
    //delegación pura, solo llama al repositorio
    public List<PesoCorporal> historial(Long usuarioId) {
        return pesoCorporalRepository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }

    //Elimina un registro de peso, siempre que pertenezca al usuario.
    public void eliminar(Long usuarioId, Long id) {
        pesoCorporalRepository.findByIdAndUsuarioId(id, usuarioId)
                .ifPresent(pesoCorporalRepository::delete);
        //la seguridad está en el nombre del método del repositorio, que busca
        //el registro comprobando que sea del usuario. si le pasan el id de otra
        //persona el Optional viene vacío y el ifPresent no hace nada
        //los :: son una referencia a método, equivale a
        //  .ifPresent(p -> pesoCorporalRepository.delete(p))
    }

    //Días transcurridos desde el último registro, o null si nunca ha registrado.
    public Long diasDesdeUltimoRegistro(Long usuarioId) {
        List<PesoCorporal> historial = historial(usuarioId);
        if (historial.isEmpty()) {
            return null;
            //null y no 0 porque son cosas distintas: 0 sería "se pesó hoy" y
            //null es "nunca se pesó". la vista los muestra diferente
        }
        return ChronoUnit.DAYS.between(historial.get(0).getFecha(), LocalDate.now());
        //el get(0) es el más reciente, porque el historial viene ordenado de
        //la fecha más nueva a la más vieja
    }
}
