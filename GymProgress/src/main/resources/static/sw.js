/*
 * HU2 — Service worker del modo sin conexión.
 *
 * Guarda en caché la rutina activa y los recursos estáticos para que la
 * pantalla siga funcionando cuando no hay Internet. Las peticiones que
 * cambian datos (POST) no se cachean: de esas se encarga la cola de
 * offline.js.
 */

/**
 * un service worker es un script que corre en segundo plano, separado de la
 * página, y que puede interceptar todas las peticiones de red del sitio. sigue
 * vivo aunque cierres la pestaña
 *
 * este archivo tiene que estar en la raíz de static, no dentro de js, porque un
 * service worker solo puede controlar las urls que están por debajo de la
 * carpeta donde vive:
 *   /sw.js    controla todo el sitio
 *   /js/sw.js controlaría solo /js/
 */

const CACHE = 'gymprogress-v1';
//el -v1 del nombre importa: cuando cambies los archivos y quieras que los
//usuarios reciban la versión nueva, subís a -v2 y el evento activate borra los
//cachés viejos. sin ese número la gente se quedaría con la versión vieja

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
                /**
                 * es Promise.allSettled y no Promise.all, y la diferencia
                 * importa: con all, si uno falla falla todo, y con allSettled
                 * espera a todos y sigue igual
                 *
                 * con all, si un solo archivo no se pudiera descargar, la
                 * instalación entera del service worker fallaría y no habría
                 * modo sin conexión en absoluto
                 */
                return Promise.allSettled(PRECARGA.map(function (url) {
                    return cache.add(url);
                }));
            })
            .then(function () {
                //skipWaiting hace que el service worker nuevo tome el control
                //enseguida, en vez de esperar a que se cierren todas las pestañas
                return self.skipWaiting();
            })
    );
});

self.addEventListener('activate', function (evento) {
    evento.waitUntil(
        caches.keys()
            .then(function (nombres) {
                //borra todos los cachés que no sean el actual, o sea la
                //limpieza de las versiones viejas
                return Promise.all(nombres
                    .filter(function (n) { return n !== CACHE; })
                    .map(function (n) { return caches.delete(n); }));
            })
            .then(function () {
                //claim hace que el service worker controle las pestañas que ya
                //estaban abiertas, sin esperar a que se recarguen
                return self.clients.claim();
            })
    );
});

self.addEventListener('fetch', function (evento) {
    const peticion = evento.request;

    // Los POST los maneja la cola de offline.js, no el caché.
    /**
     * acá queda clara la división del trabajo entre los dos archivos:
     *   sw.js se ocupa de los get, para que las pantallas se vean sin conexión
     *   offline.js se ocupa de los post, para que las series no se pierdan
     *
     * cachear un post sería un desastre, significaría devolver una respuesta
     * guardada a una petición que quiere modificar datos
     */
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
    /**
     * la estrategia es "red primero, caché de respaldo": se intenta la red y si
     * falla se usa lo guardado. es lo correcto para esta app porque con conexión
     * el usuario siempre ve datos frescos
     *
     * la estrategia contraria sería más rápida pero mostraría datos viejos, que
     * en una app donde acabás de registrar una serie sería muy confuso
     */
    evento.respondWith(
        fetch(peticion)
            .then(function (respuesta) {
                if (respuesta && respuesta.ok) {
                    /**
                     * el clone confunde a todo el mundo pero es obligatorio: una
                     * respuesta http en javascript solo se puede leer una vez,
                     * porque su cuerpo es un flujo que se consume
                     *
                     * acá hay que hacer dos cosas con ella, guardarla en el
                     * caché y devolvérsela a la página, así que sin el clone la
                     * página recibiría una respuesta ya vacía
                     */
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
                    //el respaldo del respaldo: si no hay nada guardado para esa
                    //url concreta se devuelve la rutina de hoy, así el usuario
                    //ve algo útil en vez de la pantalla del dinosaurio
                    return caches.match('/rutinas/hoy');
                });
            })
    );
});

