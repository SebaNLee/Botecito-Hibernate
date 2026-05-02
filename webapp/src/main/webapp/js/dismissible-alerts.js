(function () {
  document.addEventListener("click", function (event) {
    const dismiss = event.target.closest("[data-paw-alert-dismiss]");
    if (!dismiss) {
      return;
    }
    const root = dismiss.closest("[data-paw-alert]");
    if (root) {
      root.remove();
    }
  });
})();
