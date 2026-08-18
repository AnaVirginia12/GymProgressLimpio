document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
    const toggle = document.getElementById("sidebarToggle");

    if (!sidebar || !toggle) {
        return;
    }

    toggle.addEventListener("click", () => {
        sidebar.classList.toggle("is-open");
    });

    document.addEventListener("click", (event) => {
        const clickedOutside =
            !sidebar.contains(event.target) &&
            !toggle.contains(event.target);

        if (clickedOutside) {
            sidebar.classList.remove("is-open");
        }
    });
});

// Modo oscuro / modo claro: slider arriba a la derecha
document.addEventListener("DOMContentLoaded", () => {
    const TEMA_KEY = "gp-theme";
    const switches = document.querySelectorAll(".tema-switch-input");

    function aplicarTema(tema) {
        document.documentElement.setAttribute("data-bs-theme", tema);
        switches.forEach((input) => {
            input.checked = tema === "dark";
        });
    }

    const guardado = localStorage.getItem(TEMA_KEY)
        || document.documentElement.getAttribute("data-bs-theme")
        || "dark";
    aplicarTema(guardado);

    switches.forEach((input) => {
        input.addEventListener("change", () => {
            const tema = input.checked ? "dark" : "light";
            localStorage.setItem(TEMA_KEY, tema);
            aplicarTema(tema);

            fetch("/configuracion/tema", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: "tema=" + encodeURIComponent(tema),
            }).catch(() => {});
        });
    });
});