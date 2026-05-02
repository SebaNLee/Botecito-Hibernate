(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.querySelector("[data-manage-availability-date-form]");
        if (!form) {
            return;
        }
        const dateInput = form.querySelector('input[name="date"]');
        if (!dateInput) {
            return;
        }
        dateInput.addEventListener("change", function () {
            if (dateInput.value) {
                form.submit();
            }
        });
    });
})();
