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
    'use strict';

    const CLAVE_COLA = 'gymprogress.colaSeries';

    /* --- cola en localStorage ------------------------------------------ */

    function leerCola() {
        try {
            return JSON.parse(localStorage.getItem(CLAVE_COLA)) || [];
        } catch (e) {
            return [];
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
            barra.style.position = 'sticky';
            barra.style.top = '0';
            barra.style.zIndex = '1020';

            /*
             * Va dentro de la columna de contenido para no taparse con la
             * barra lateral, que está fija a la izquierda. En las pantallas
             * sin barra lateral (login, registro) se pone arriba del todo.
             */
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

        if (sinConexion) {
            barra.className = 'alert alert-warning mb-0 text-center rounded-0 py-2';
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
            }

            evento.preventDefault();

            const datos = new URLSearchParams(new FormData(formulario)).toString();

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

        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('/sw.js').catch(function () {
                // Sin service worker el resto sigue funcionando.
            });
        }
    });

    window.addEventListener('online', function () {
        pintarIndicador();
        sincronizar();
    });

    window.addEventListener('offline', pintarIndicador);

    // Para poder comprobarlo desde las pruebas.
    window.gymprogressOffline = {
        pendientes: function () { return leerCola().length; },
        sincronizar: sincronizar
    };
})();


