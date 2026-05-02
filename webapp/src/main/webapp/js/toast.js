(function () {
  const SUCCESS_DURATION_MS = 5000;
  const ERROR_DURATION_MS = 8000;
  const FADE_OUT_MS = 200;

  function dismissToast(node) {
    if (!node || node.classList.contains("paw-toast-fading")) return;
    node.classList.add("paw-toast-fading");
    setTimeout(function () {
      if (node.parentNode) node.parentNode.removeChild(node);
    }, FADE_OUT_MS);
  }

  function scheduleAutoDismiss(toast) {
    const type = toast.getAttribute("data-paw-toast-type");
    const duration = type === "error" || type === "warning" ? ERROR_DURATION_MS : SUCCESS_DURATION_MS;
    setTimeout(function () {
      dismissToast(toast);
    }, duration);
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-paw-toast]").forEach(scheduleAutoDismiss);
  });

  document.addEventListener("click", function (event) {
    const dismiss = event.target.closest("[data-paw-toast-dismiss]");
    if (!dismiss) return;
    dismissToast(dismiss.closest("[data-paw-toast]"));
  });
})();
