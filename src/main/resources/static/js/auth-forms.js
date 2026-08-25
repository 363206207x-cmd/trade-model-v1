document.querySelectorAll("[data-password-toggle]").forEach(function (toggle) {
    toggle.addEventListener("change", function () {
        const visible = toggle.checked;
        String(toggle.dataset.passwordTargets || "").split(",").forEach(function (targetId) {
            const input = document.getElementById(targetId.trim());
            if (input) input.type = visible ? "text" : "password";
        });
    });
});
