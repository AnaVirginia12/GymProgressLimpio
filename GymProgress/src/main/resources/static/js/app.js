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