/*
 * app.js es el javascript general de la aplicación.
 *
 * hace dos cosas independientes: abrir y cerrar la barra lateral en pantallas
 * chicas, y el interruptor de modo claro y oscuro
 *
 * cada una va en su propio DOMContentLoaded para que si una falla, la otra
 * siga funcionando
 */

// ---------- la barra lateral en móvil ----------
document.addEventListener("DOMContentLoaded", () => {
    //DOMContentLoaded espera a que el html esté listo, sin esto
    //getElementById podría ejecutarse antes de que exista el elemento
    const sidebar = document.getElementById("sidebar");
    const toggle = document.getElementById("sidebarToggle");

    if (!sidebar || !toggle) {
        return;
        //en login y registro no hay barra lateral, así que este bloque
        //simplemente no hace nada en vez de dar error
    }

    //el botón de las tres rayas. toggle agrega la clase si no está y la quita
    //si está, y toda la animación la hace el css con .sidebar.is-open
    toggle.addEventListener("click", () => {
        sidebar.classList.toggle("is-open");
    });

    //un detalle de usabilidad: si tocás fuera del menú, se cierra solo
    //se comprueba que el clic no haya sido ni dentro del menú ni en el botón,
    //porque si no el botón abriría y este cerraría, y nunca se vería abierto
    document.addEventListener("click", (event) => {
        const clickedOutside =
            !sidebar.contains(event.target) &&
            !toggle.contains(event.target);

        if (clickedOutside) {
            sidebar.classList.remove("is-open");
        }
    });
});

// ---------- modo oscuro y claro ----------
// Modo oscuro / modo claro: slider arriba a la derecha
document.addEventListener("DOMContentLoaded", () => {
    const TEMA_KEY = "gp-theme"; //la clave con la que se guarda el tema en el navegador
    const switches = document.querySelectorAll(".tema-switch-input");

    function aplicarTema(tema) {
        //data-bs-theme es el atributo que usa bootstrap 5.3 para cambiar entre
        //claro y oscuro. se pone en el <html> y bootstrap recolorea todo solo
        document.documentElement.setAttribute("data-bs-theme", tema);
        switches.forEach((input) => {
            input.checked = tema === "dark";
        });
    }

    /**
     * una cascada de tres niveles para decidir el tema, en orden de prioridad:
     * primero lo que el usuario eligió en este navegador, después lo que traiga
     * el html, que viene de la base de datos, y por último "dark" como último
     * recurso
     *
     * el || va tomando el primero que no sea null ni vacío
     */
    const guardado = localStorage.getItem(TEMA_KEY)
        || document.documentElement.getAttribute("data-bs-theme")
        || "dark";
    aplicarTema(guardado);

    switches.forEach((input) => {
        input.addEventListener("change", () => {
            const tema = input.checked ? "dark" : "light";
            //se guarda en el navegador y se aplica al instante, antes de
            //avisarle al servidor, así el cambio se ve inmediato aunque la red
            //esté lenta
            localStorage.setItem(TEMA_KEY, tema);
            aplicarTema(tema);

            /**
             * y en paralelo se guarda en la base, para que el tema te siga si
             * entrás desde otro dispositivo
             *
             * el .catch(() => {}) del final se traga cualquier error a propósito:
             * si el servidor no responde, el tema ya se aplicó localmente, así
             * que no hay nada que avisarle al usuario
             */
            fetch("/configuracion/tema", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: "tema=" + encodeURIComponent(tema),
            }).catch(() => {});
        });
    });
});