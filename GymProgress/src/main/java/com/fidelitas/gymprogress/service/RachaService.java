package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Racha;
import com.fidelitas.gymprogress.repository.RachaRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sistema de rachas de días consecutivos entrenando.
 */
@Service
public class RachaService {

    private final RachaRepository rachaRepository;

    public RachaService(RachaRepository rachaRepository) {
        this.rachaRepository = rachaRepository;
    }

    /**
     * Registra que el usuario entrenó hoy.
     * - Si ya entrenó hoy: no hace nada.
     * - Si entrenó ayer: incrementa la racha.
     * - Si pasó más de un día: reinicia la racha a 1.
     */
    @Transactional //porque este método escribe en la base
    public Racha registrarEntrenamiento(Long usuarioId) {
        //busca la racha, o crea una nueva si el usuario nunca entrenó
        Racha racha = rachaRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    Racha nueva = new Racha();
                    nueva.setUsuarioId(usuarioId);
                    return nueva;
                });
        /**
         * es orElseGet y no orElse: con orElse el objeto se crearía siempre,
         * incluso cuando la racha ya existe, para después descartarlo. con
         * orElseGet la lambda solo se ejecuta si la caja está vacía
         */
        //la racha nueva todavía no se guarda, se guarda al final junto con todo
        //lo demás, así queda un solo save para los dos casos

        LocalDate hoy = LocalDate.now();
        LocalDate ultimo = racha.getUltimoEntrenamiento();
        //se guardan en variables para llamar a now() una sola vez. si se llamara
        //en cada comparación y el método corriera justo a medianoche, dos
        //llamadas podrían devolver días distintos

        //primer caso: ya entrenó hoy, sale sin tocar nada
        if (hoy.equals(ultimo)) {
            // Ya se contó hoy
            return racha;
            //esto es lo que hace que entrenar dos veces el mismo día no sume 2.
            //importa porque SesionController llama a este método cada vez que
            //se finaliza un entrenamiento
        }

        //la condición se lee "el día siguiente al último entrenamiento, ¿es
        //hoy?", o sea, ¿entrenó ayer?
        //plusDays maneja solo los cambios de mes y año: 31 de agosto más 1 día
        //es 1 de setiembre, y 28 de febrero en año bisiesto es 29. hecho a mano
        //sumando números sería un bug seguro
        //el "ultimo != null" protege de llamar plusDays sobre null
        if (ultimo != null && ultimo.plusDays(1).equals(hoy)) {
            // Día consecutivo
            racha.setDiasActuales(racha.getDiasActuales() + 1);
        } else {
            // Racha rota o primera vez
            racha.setDiasActuales(1);
            //se pone en 1 y no en 0: hoy entrenaste, así que llevás un día
        }

        //si la racha actual superó el récord histórico, se actualiza el récord
        //diasMaximo solo sube y nunca baja, porque solo se toca dentro de este
        //if. cuando la racha se reinicia a 1, el máximo se queda como estaba
        if (racha.getDiasActuales() > racha.getDiasMaximo()) {
            racha.setDiasMaximo(racha.getDiasActuales());
        }

        racha.setUltimoEntrenamiento(hoy);
        //se marca hoy como último día entrenado, lo que hará que una segunda
        //llamada hoy salga por el primer caso
        return rachaRepository.save(racha);
        //un solo save al final, después de todos los cambios, así queda un
        //solo UPDATE en vez de varios
    }

    //Devuelve la racha actual del usuario (0 si no existe).
    @Transactional(readOnly = true) //solo lee, hibernate se ahorra trabajo de control
    public Racha obtener(Long usuarioId) {
        /**
         * devuelve Racha y no Optional<Racha>: si el usuario no tiene racha,
         * devuelve un objeto vacío pero válido, con los contadores en 0
         *
         * eso simplifica mucho el html, porque se puede escribir
         *   ${racha.diasActuales}
         * en vez de
         *   ${racha != null ? racha.diasActuales : 0}
         *
         * ese patrón se llama Null Object: en vez de devolver null y obligar a
         * todos a comprobarlo, devolvés un objeto que representa "nada" pero se
         * comporta como los demás
         */
        return rachaRepository.findByUsuarioId(usuarioId).orElseGet(() -> {
            Racha vacia = new Racha();
            vacia.setUsuarioId(usuarioId);
            return vacia;
        });
        //esa racha vacía no se guarda en la base, es solo para mostrar. la fila
        //se crea recién cuando el usuario entrena por primera vez
    }
}
