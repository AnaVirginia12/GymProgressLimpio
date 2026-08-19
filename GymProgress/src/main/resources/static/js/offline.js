/*
 * HU2 — Modo sin conexión.
 *
 * Criterios que cubre este archivo:
 *   1. Registrar series sin conexión: el formulario de la sesión activa se
 *      intercepta y la serie se guarda en una cola local.
 *   2. Sincronizar al recuperar la conexión: la cola se envía sola cuando
 *      vuelve Internet.
 *   3. Indicar visualmente el modo sin conexión: una barra fija arriba.
 *   4. No perder datos: la cola vive en localStorage, así que sobrevive a
 *      recargas y a cerrar el navegador.
 */
(function () {
    /**
     * el (function(){ ... })(); del principio y del final se llama IIFE, o sea
     * una función que se ejecuta sola. todo lo que se declare adentro no existe
     * afuera, así no se ensucia el espacio global y ningún otro script puede
     * pisar estas variables
     */
    'use strict';
    //el modo estricto convierte en error cosas que si no pasarían en silencio,
    //sobre todo usar una variable sin declararla

    const CLAVE_COLA = 'gymprogress.colaSeries';
    //el nombre con el que se guarda la cola. el prefijo 'gymprogress.' evita
    //chocar con otras aplicaciones que usen el mismo navegador

    /* --- cola en localStorage ------------------------------------------ */

    /**
     * localStorage es lo que hace que no se pierdan los datos. es un almacén de
     * texto del navegador que sobrevive a recargar la página y a cerrarlo:
     *   una variable normal se pierde al recargar
     *   sessionStorage se pierde al cerrar la pestaña
     *   localStorage solo se pierde si el usuario lo borra
     *
     * y solo guarda texto, por eso los JSON.stringify y JSON.parse
     */
    function leerCola() {
        try {
            return JSON.parse(localStorage.getItem(CLAVE_COLA)) || [];
            //el || [] es porque si no hay nada guardado getItem devuelve null,
            //y JSON.parse(null) devuelve null. el || lo convierte en un arreglo
            //vacío para que el resto del código no tenga que comprobarlo
        } catch (e) {
            return [];
            //el try/catch protege del caso en que lo guardado esté corrupto.
            //perder una cola corrupta es mejor que dejar la app rota
        }
    }

    function guardarCola(cola) {
        localStorage.setItem(CLAVE_COLA, JSON.stringify(cola));
    }

    function encolar(entrada) {
        const cola = leerCola();
        cola.push(entrada);
        guardarCola(cola);
        pintarIndicador();
    }

    /* --- indicador visual ---------------------------------------------- */

    function elementoBarra() {
        let barra = document.getElementById('barraOffline');

        if (!barra) {
            barra = document.createElement('div');
            barra.id = 'barraOffline';
            barra.className = 'alert mb-0 text-center rounded-0 py-2';
            barra.setAttribute('role', 'status');
            //esto es accesibilidad: le dice a los lectores de pantalla que este
            //elemento anuncia cambios de estado, así una persona ciega se
            //entera de que se perdió la conexión
            barra.style.position = 'sticky';
            barra.style.top = '0';
            barra.style.zIndex = '1020';

            /*
             * Va dentro de la columna de contenido para no taparse con la
             * barra lateral, que está fija a la izquierda. En las pantallas
             * sin barra lateral (login, registro) se pone arriba del todo.
             */
            //va dentro de la columna de contenido y no en el body por un
            //problema de maquetación: la barra lateral está fija a la izquierda,
            //así que una barra puesta en el body quedaría por debajo
            const contenido = document.querySelector('.app-content');

            if (contenido) {
                contenido.prepend(barra);
            } else {
                document.body.prepend(barra);
            }
        }

        return barra;
    }

    function pintarIndicador() {
        const barra = elementoBarra();
        const pendientes = leerCola().length;
        const sinConexion = !navigator.onLine;
        /**
         * navigator.onLine es lo que el navegador cree sobre la conexión, pero
         * ojo que solo detecta si hay interfaz de red, no si hay internet de
         * verdad: conectada a un wifi sin salida, dice que sí
         *
         * igual el diseño lo cubre, porque si el envío falla el catch de
         * sincronizar devuelve la serie a la cola. la detección es una pista,
         * no la garantía
         */

        if (sinConexion) {
            barra.className = 'alert alert-warning mb-0 text-center rounded-0 py-2';
            //es textContent y no innerHTML: escribe texto plano, así que aunque
            //el contenido tuviera un <script> se mostraría como texto en vez de
            //ejecutarse. es la defensa contra xss
            barra.textContent = pendientes > 0
                ? 'Sin conexión — ' + pendientes + (pendientes === 1
                    ? ' serie guardada en este dispositivo'
                    : ' series guardadas en este dispositivo')
                : 'Sin conexión — puedes seguir registrando series';
            barra.hidden = false;
            return;
        }

        if (pendientes > 0) {
            barra.className = 'alert alert-info mb-0 text-center rounded-0 py-2';
            barra.textContent = 'Conexión recuperada — sincronizando '
                + pendientes + (pendientes === 1 ? ' serie…' : ' series…');
            barra.hidden = false;
            return;
        }

        barra.hidden = true;
    }

    /* --- sincronización ------------------------------------------------- */

    let sincronizando = false;
    //esta bandera evita un problema real: si el evento 'online' se disparara dos
    //veces seguidas, cosa que pasa con wifi inestable, habría dos
    //sincronizaciones a la vez leyendo la misma cola y las series se enviarían
    //duplicadas

    async function sincronizar() {
        if (sincronizando || !navigator.onLine) {
            return;
        }

        const cola = leerCola();
        if (cola.length === 0) {
            pintarIndicador();
            return;
        }

        sincronizando = true;
        pintarIndicador();

        const quedan = [];
        /**
         * este arreglo es la parte inteligente: en vez de vaciar la cola de
         * golpe, se va armando la lista de las que fallaron, y al final esa
         * lista reemplaza la cola
         *
         * así, si se sincronizan 3 de 5 series, las otras 2 siguen en la cola
         * para el siguiente intento. si se vaciara la cola de entrada, esas 2
         * se perderían, que es justo lo contrario de lo que se busca
         */

        for (const entrada of cola) {
            try {
                const respuesta = await fetch(entrada.url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: entrada.datos
                });

                // Si el servidor responde mal, se conserva para reintentar.
                if (!respuesta.ok) {
                    quedan.push(entrada);
                }
            } catch (e) {
                // Se cayó la conexión otra vez: se guarda todo lo que falta.
                quedan.push(entrada);
            }
        }

        guardarCola(quedan);
        sincronizando = false;

        const barra = elementoBarra();

        if (quedan.length === 0) {
            barra.className = 'alert alert-success mb-0 text-center rounded-0 py-2';
            barra.textContent = 'Series sincronizadas correctamente.';
            barra.hidden = false;
            // Recargar para que la tabla muestre lo que quedó guardado.
            setTimeout(function () { window.location.reload(); }, 900);
        } else {
            pintarIndicador();
        }
    }

    /* --- interceptar el formulario de series ---------------------------- */

    function prepararFormulario() {
        const formulario = document.getElementById('serieForm');
        if (!formulario) {
            return;
        }

        formulario.addEventListener('submit', function (evento) {
            if (navigator.onLine) {
                return; // con conexión se envía normal
                //con conexión este return no hace nada y el formulario se
                //envía como si el javascript no existiera. el modo sin conexión
                //solo se activa cuando hace falta
            }

            evento.preventDefault();
            //cancela el envío normal, es la línea que convierte un formulario
            //común en uno que se guarda localmente

            const datos = new URLSearchParams(new FormData(formulario)).toString();
            /**
             * convierte el formulario al mismo formato que usaría el navegador:
             *   ejercicioId=3&numeroSerie=1&pesoKg=80&repeticiones=10
             *
             * guardarlo así es la clave de todo, porque permite que la
             * sincronización lo mande tal cual con fetch, y que el controlador
             * de spring no note ninguna diferencia entre una serie enviada en
             * vivo y una sincronizada después. por eso el servidor no necesita
             * ningún endpoint especial
             */

            encolar({
                url: formulario.action,
                datos: datos,
                registradaEn: new Date().toISOString()
            });

            mostrarFilaLocal(formulario);
            formulario.reset();
        });
    }

    /*
     * Muestra la serie al momento, marcada como pendiente, para que el
     * usuario vea que quedó guardada aunque no haya conexión.
     *
     * Si ya hay tabla de series registradas se añade una fila ahí. Si es la
     * primera serie de la sesión la tabla todavía no existe, así que se
     * crea una lista propia encima del formulario.
     */
    /**
     * sin este método el usuario apretaría "guardar" y no pasaría nada visible,
     * pensaría que no funcionó y lo intentaría otra vez
     *
     * al pintar la serie al instante con la etiqueta "pendiente" en amarillo, ve
     * que quedó guardada y que todavía no se envió. las dos cosas a la vez, que
     * es lo honesto
     */
    function mostrarFilaLocal(formulario) {
        const select = formulario.querySelector('#ejercicioSelect');
        const nombre = select && select.selectedIndex > 0
            ? select.options[select.selectedIndex].text
            : 'Ejercicio';

        const valor = function (id) {
            const campo = formulario.querySelector(id);
            return campo ? campo.value : '';
        };

        const cuerpoTabla = document.querySelector('table tbody');

        if (cuerpoTabla) {
            const fila = document.createElement('tr');
            fila.className = 'table-warning serie-pendiente';
            fila.innerHTML =
                '<td></td><td></td><td></td><td></td>'
                + '<td><span class="badge rounded-pill bg-warning-subtle '
                + 'text-warning-emphasis">Pendiente</span></td><td>—</td>';

            const celdas = fila.querySelectorAll('td');
            celdas[0].textContent = nombre;
            celdas[1].textContent = valor('#numeroSerie');
            celdas[2].textContent = valor('#pesoKg');
            celdas[3].textContent = valor('#repeticiones');

            cuerpoTabla.appendChild(fila);
            return;
        }

        // Primera serie de la sesión: no hay tabla todavía.
        let lista = document.getElementById('seriesPendientes');

        if (!lista) {
            const caja = document.createElement('section');
            caja.className = 'card border-warning mt-4';
            caja.innerHTML =
                '<div class="card-body">'
                + '<h2 class="h5 mb-3">Series guardadas sin conexión</h2>'
                + '<div class="vstack gap-2" id="seriesPendientes"></div>'
                + '</div>';

            const formularioCaja = formulario.closest('section') || formulario;
            formularioCaja.parentNode.insertBefore(caja, formularioCaja);
            lista = caja.querySelector('#seriesPendientes');
        }

        const item = document.createElement('div');
        item.className =
            'd-flex flex-wrap align-items-center gap-2 border rounded-3 p-2 serie-pendiente';

        const etiqueta = document.createElement('strong');
        etiqueta.textContent = nombre;

        const detalle = document.createElement('span');
        detalle.className = 'text-body-secondary';
        detalle.textContent = 'Serie ' + valor('#numeroSerie') + ' · '
            + valor('#pesoKg') + ' kg × ' + valor('#repeticiones') + ' reps';

        const estado = document.createElement('span');
        estado.className =
            'badge rounded-pill bg-warning-subtle text-warning-emphasis ms-auto';
        estado.textContent = 'Pendiente';

        item.append(etiqueta, detalle, estado);
        lista.appendChild(item);
    }

    /* --- arranque -------------------------------------------------------- */

    document.addEventListener('DOMContentLoaded', function () {
        prepararFormulario();
        pintarIndicador();

        if (navigator.onLine) {
            sincronizar();
        }

        //se comprueba que el navegador lo soporte antes de usarlo, y el catch
        //vacío hace que si falla el registro el resto siga funcionando igual:
        //la cola no depende del service worker, son dos mecanismos separados
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('/sw.js').catch(function () {
                // Sin service worker el resto sigue funcionando.
            });
        }
    });

    //los eventos 'online' y 'offline' los dispara el navegador solo cuando
    //detecta cambios de conexión. acá está la sincronización automática, el
    //usuario no tiene que apretar nada
    window.addEventListener('online', function () {
        pintarIndicador();
        sincronizar();
    });

    window.addEventListener('offline', pintarIndicador);

    // Para poder comprobarlo desde las pruebas.
    //lo único que sale del IIFE. sirve para poder comprobar desde una prueba
    //automática cuántas series hay pendientes, sin mirar la pantalla
    window.gymprogressOffline = {
        pendientes: function () { return leerCola().length; },
        sincronizar: sincronizar
    };
})();


