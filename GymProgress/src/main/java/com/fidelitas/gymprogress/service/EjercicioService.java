package com.fidelitas.gymprogress.service;

import com.fidelitas.gymprogress.domain.Ejercicio;
import com.fidelitas.gymprogress.repository.EjercicioRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EjercicioService {

    private final EjercicioRepository ejercicioRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    //todo el catálogo, ordenado alfabéticamente
    public List<Ejercicio> listar() {
        return ejercicioRepository.findAllByOrderByNombreAsc();
    }

    public List<Ejercicio> buscar(String texto, String tipo) {
        String textoFiltro = (texto == null || texto.isBlank()) ? null : texto.trim();
        String tipoFiltro = (tipo == null || tipo.isBlank()) ? null : tipo;
        /**
         * parece que no hace nada pero es imprescindible.
         *
         * la consulta trata el null como "sin filtro", pero un formulario html
         * nunca manda null: si el usuario no escribe nada manda "", texto vacío
         *
         * isBlank devuelve true si está vacío o si solo tiene espacios, que es
         * distinto de isEmpty:
         *   "".isEmpty()     da true
         *   "   ".isEmpty()  da false, y ahí está el problema
         *   "   ".isBlank()  da true, que es lo que queremos
         *
         * y el trim saca los espacios de los extremos: si escribe "  press  "
         * busca "press"
         */
        //'tipo' no lleva trim porque viene de un select con valores fijos
        return ejercicioRepository.buscar(textoFiltro, tipoFiltro);
    }

    public Optional<Ejercicio> obtenerPorId(Long id) {
        return ejercicioRepository.findById(id);
    }

    public Ejercicio crear(Ejercicio ejercicio) {
        validar(ejercicio); //si falta algo obligatorio, corta acá

        ejercicio.setId(null);
        /**
         * esta línea es una medida de seguridad. el objeto viene armado por
         * spring desde el formulario, y si alguien agregara a mano un
         *   <input type="hidden" name="id" value="7">
         * ese id llegaría puesto y el save haría un UPDATE del ejercicio 7 en
         * vez de crear uno nuevo, o sea que crear un ejercicio pisaría otro
         *
         * forzarlo a null garantiza que el save haga un INSERT
         */
        ejercicio.setFavorito(false);
        //un ejercicio nace no favorito, el usuario lo marca después con
        //alternarFavorito(). así el formulario no puede crear uno ya marcado
        return ejercicioRepository.save(ejercicio);
    }

    public Ejercicio actualizar(Long id, Ejercicio datos) {
        Ejercicio existente = ejercicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado."));

        validar(datos);

        /**
         * acá se pisan todos los campos, a diferencia de UsuarioService que
         * solo pisa los que no son null
         *
         * es correcto porque el formulario de editar viene precargado con los
         * valores actuales: el usuario ve todo y modifica lo que quiere, así
         * que todos los campos viajan
         */
        existente.setNombre(datos.getNombre());
        existente.setGrupoMuscular(datos.getGrupoMuscular());
        existente.setTipoEntrenamiento(datos.getTipoEntrenamiento());
        existente.setDescripcion(datos.getDescripcion());
        existente.setMediaUrl(datos.getMediaUrl());
        existente.setRequiereEquipo(datos.getRequiereEquipo());
        existente.setTempoExcentrico(datos.getTempoExcentrico());
        existente.setTempoPausaAbajo(datos.getTempoPausaAbajo());
        existente.setTempoConcentrico(datos.getTempoConcentrico());
        existente.setTempoPausaArriba(datos.getTempoPausaArriba());
        // favorito se preserva; solo cambia vía alternarFavorito()
        //este comentario explica una ausencia, que es cuando más vale la pena:
        //si 'favorito' estuviera en la lista, editar un ejercicio te lo
        //desmarcaría, porque el formulario de edición no incluye esa casilla

        return ejercicioRepository.save(existente);
    }

    public void eliminar(Long id) {
        ejercicioRepository.deleteById(id);
        /**
         * simple, pero con un riesgo: puede haber 'RutinaEjercicio' y
         * 'SerieRegistrada' que guarden ese ejercicioId. como esas referencias
         * son Long sueltos y no relaciones jpa, hibernate no se entera y no
         * impide el borrado, así que quedan filas apuntando a algo que ya no existe
         *
         * el historial no se rompe, porque para eso se copió el nombre en
         * 'SerieRegistrada', pero en la rutina el nombre aparecería en blanco
         */
    }

    public Ejercicio alternarFavorito(Long id) {
        Ejercicio ejercicio = ejercicioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ejercicio no encontrado."));
        ejercicio.setFavorito(!Boolean.TRUE.equals(ejercicio.getFavorito()));
        /**
         * esta línea es un pequeño rompecabezas. lo intuitivo sería escribir
         *   !ejercicio.getFavorito()
         * pero eso explota si el valor es null, porque java intenta convertir
         * el Boolean a boolean para poder negarlo
         *
         * ¿cuándo sería null si el campo tiene = false por defecto? cuando la
         * fila vino de un INSERT hecho a mano en sql sin esa columna, que es
         * justo el caso del script de datos del proyecto
         *
         *   si vale true   la comparación da true    y con el ! queda false
         *   si vale false  la comparación da false   y con el ! queda true
         *   si vale null   la comparación da false   y con el ! queda true
         */
        return ejercicioRepository.save(ejercicio);
    }

    /*
     * Alternativas sin equipamiento para los grupos musculares de un
     * ejercicio de gimnasio.
     *
     * grupoMuscular puede traer varios grupos separados por coma, así que se
     * busca por cada uno y se juntan los resultados sin repetir.
     */
    public List<Ejercicio> alternativasEnCasa(Ejercicio original) {
        return buscarAlternativas(original, true);
        //el true quiere decir "solo sin equipo", para "hoy no voy al gimnasio"
    }

    /*
     * HU31 — Alternativas del mismo grupo muscular para sustituir un
     * ejercicio durante el entrenamiento. Aquí sí se admite equipo, porque
     * el usuario puede estar en el gimnasio y solo querer cambiarlo.
     */
    //son dos métodos públicos con nombre claro que llaman al mismo método
    //privado. si 'buscarAlternativas' fuera público, en los controladores se
    //leería buscarAlternativas(ejercicio, true) y nadie sabría qué es ese true
    public List<Ejercicio> alternativasMismoGrupo(Ejercicio original) {
        return buscarAlternativas(original, false);
        //acá el false permite ejercicios con equipo, porque estás en el gimnasio
    }

    private List<Ejercicio> buscarAlternativas(Ejercicio original, boolean soloSinEquipo) {
        if (original == null || original.getGrupoMuscular() == null) {
            return List.of();
            //List.of() crea una lista vacía inmutable. se devuelve una lista
            //vacía y no null, así quien llama puede hacer un for sin comprobar
        }

        Map<Long, Ejercicio> encontrados = new LinkedHashMap<>();
        /**
         * el mapa es la clave del método. como las claves no se repiten, esto
         * elimina los duplicados solo, porque un mismo ejercicio puede trabajar
         * pecho y tríceps y aparecería dos veces
         *
         * y es LinkedHashMap y no HashMap por el orden:
         *   HashMap       lo recorre en orden impredecible
         *   LinkedHashMap lo recorre en el orden en que se insertaron
         *
         * importa porque la consulta ordena por favorito y después alfabético,
         * y con un HashMap ese orden se perdería
         */

        //split parte el texto por las comas:
        //  "Pecho, Tríceps" queda como ["Pecho", " Tríceps"]
        //fijarse en el espacio al principio del segundo
        for (String grupo : original.getGrupoMuscular().split(",")) {
            String limpio = grupo.trim();
            //el trim saca ese espacio, si no se buscaría " Tríceps" con espacio
            //y el LIKE podría no coincidir
            if (limpio.isEmpty()) {
                continue;
                //protege de comas mal puestas como "Pecho,,Tríceps" o "Pecho,".
                //continue salta a la siguiente vuelta del bucle, si no se
                //buscaría con el grupo vacío y devolvería el catálogo entero
            }

            List<Ejercicio> parciales = soloSinEquipo
                    ? ejercicioRepository.alternativasSinEquipo(limpio, original.getId())
                    : ejercicioRepository.alternativasMismoGrupo(limpio, original.getId());

            for (Ejercicio e : parciales) {
                encontrados.putIfAbsent(e.getId(), e);
                /**
                 * putIfAbsent guarda solo si esa clave no estaba, y si ya
                 * estaba no hace nada:
                 *   put(id, ej)          pisa lo que hubiera
                 *   putIfAbsent(id, ej)  respeta lo que hubiera
                 *
                 * importa por el orden: si "Flexiones" aparece en la búsqueda de
                 * "Pecho" y también en la de "Tríceps", con putIfAbsent conserva
                 * la posición de la primera vez, y con put se movería al final
                 */
            }
        }

        return new ArrayList<>(encontrados.values());
        //values() devuelve los ejercicios sin las claves, en el orden del mapa.
        //se copian a un ArrayList nuevo porque lo que devuelve values() es una
        //vista conectada al mapa original
    }

    /*
     * Tempo recomendado según el tipo de ejercicio y el programa.
     *
     * Si el ejercicio ya trae su propio tempo se respeta; si no, se sugiere
     * uno según el programa: en fuerza la fase excéntrica es más corta y con
     * pausa abajo, en hipertrofia se alarga la excéntrica, y en resistencia
     * se busca un ritmo continuo.
     */
    public int[] tempoRecomendado(Ejercicio ejercicio, String programa) {
        //devuelve un arreglo de 4 posiciones, en el orden de las fases:
        //excéntrico, pausa abajo, concéntrico y pausa arriba

        //primero: si el ejercicio ya tiene su propio tempo, se respeta
        if (ejercicio != null && ejercicio.tieneTempo()) {
            return new int[] {
                valor(ejercicio.getTempoExcentrico()),
                valor(ejercicio.getTempoPausaAbajo()),
                valor(ejercicio.getTempoConcentrico()),
                valor(ejercicio.getTempoPausaArriba())
            };
        }

        //cada valor pasa por valor() porque tieneTempo() devuelve true si al
        //menos uno está definido, así que los otros tres pueden ser null

        String tipo = ejercicio == null ? null : ejercicio.getTipoEntrenamiento();
        //el ternario evita el NullPointerException si no hay ejercicio

        //segundo: si es cardio, ritmo continuo sin pausas
        if ("Cardio".equalsIgnoreCase(tipo)) {
            return new int[] {1, 0, 1, 0};
            //en una carrera no tiene sentido "pausar abajo"
        }

        //tercero: si no hay programa, un tempo neutro
        if (programa == null) {
            return new int[] {2, 1, 1, 0};
        }

        return switch (programa) {
            //y por último, según el programa
            case "Fuerza" -> new int[] {2, 1, 1, 1}; //pausa arriba para recuperar
            case "Hipertrofia" -> new int[] {3, 1, 1, 0}; //la excéntrica larga de
            //3 segundos es la que más estimula el crecimiento muscular, porque
            //es la fase de estiramiento bajo tensión
            case "Resistencia" -> new int[] {2, 0, 1, 0}; //sin pausas, continuo
            default -> new int[] {2, 1, 1, 0};
        };
    }

    private int valor(Integer v) {
        return v == null ? 0 : v;
        //convierte Integer a int con red de seguridad, usando el ternario
    }

    //comprueba los tres campos obligatorios. se llama desde crear() y desde
    //actualizar(), o sea un solo lugar con las reglas y dos usos
    private void validar(Ejercicio ejercicio) {
        if (ejercicio.getNombre() == null || ejercicio.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del ejercicio es obligatorio.");
        }
        if (ejercicio.getGrupoMuscular() == null || ejercicio.getGrupoMuscular().isBlank()) {
            throw new IllegalArgumentException("El grupo muscular es obligatorio.");
        }
        if (ejercicio.getTipoEntrenamiento() == null || ejercicio.getTipoEntrenamiento().isBlank()) {
            throw new IllegalArgumentException("El tipo de entrenamiento es obligatorio.");
        }
        /**
         * el orden "== null || isBlank()" es obligatorio, al revés isBlank
         * sobre null explotaría. java evalúa de izquierda a derecha y corta
         * apenas puede decidir, así que si es null nunca llega a llamar isBlank
         *
         * ¿por qué validar acá si el html ya tiene required? porque el required
         * del navegador se salta desactivando javascript o mandando la petición
         * con curl. la validación del cliente es comodidad, la del servidor es
         * la de verdad
         */
    }
}
