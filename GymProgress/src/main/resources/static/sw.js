/*
 * HU2 — Service worker del modo sin conexión.
 *
 * Guarda en caché la rutina activa y los recursos estáticos para que la
 * pantalla siga funcionando cuando no hay Internet. Las peticiones que
 * cambian datos (POST) no se cachean: de esas se encarga la cola de
 * offline.js.
 */

const CACHE = 'gymprogress-v1';

// Lo mínimo para que la rutina de hoy se vea sin conexión.
const PRECARGA = [
    '/rutinas/hoy',
    '/css/app.css',
    '/js/app.js',
    '/js/offline.js'
];

self.addEventListener('install', function (evento) {
    evento.waitUntil(
        caches.open(CACHE)
            .then(function (cache) {
                // Si alguno falla (por ejemplo el CDN), no se aborta la instalación.
                return Promise.allSettled(PRECARGA.map(function (url) {
                    return cache.add(url);
                }));
            })
            .then(function () {
                return self.skipWaiting();
            })
    );
});

self.addEventListener('activate', function (evento) {
    evento.waitUntil(
        caches.keys()
            .then(function (nombres) {
                return Promise.all(nombres
                    .filter(function (n) { return n !== CACHE; })
                    .map(function (n) { return caches.delete(n); }));
            })
            .then(function () {
                return self.clients.claim();
            })
    );
});

self.addEventListener('fetch', function (evento) {
    const peticion = evento.request;

    // Los POST los maneja la cola de offline.js, no el caché.
    if (peticion.method !== 'GET') {
        return;
    }

    // Solo se cachea lo de esta misma aplicación.
    if (new URL(peticion.url).origin !== self.location.origin) {
        return;
    }

    /*
     * Primero la red, y si falla lo guardado. Así con conexión siempre se
     * ven datos frescos, y sin conexión se ve la última versión cargada.
     */
    evento.respondWith(
        fetch(peticion)
            .then(function (respuesta) {
                if (respuesta && respuesta.ok) {
                    const copia = respuesta.clone();
                    caches.open(CACHE).then(function (cache) {
                        cache.put(peticion, copia);
                    });
                }
                return respuesta;
            })
            .catch(function () {
                return caches.match(peticion).then(function (guardada) {
                    if (guardada) {
                        return guardada;
                    }
                    return caches.match('/rutinas/hoy');
                });
            })
    );
});

