(function() {
  function setLoadingState(form) {
    const button = form.querySelector("[data-submit-loading-button]");
    if (!button || button.disabled) {
      return;
    }

    const content = button.querySelector("[data-submit-loading-content]");
    const spinner = button.querySelector("[data-submit-loading-spinner]");
    const loadingText = button.dataset.loadingText;

    button.disabled = true;
    button.setAttribute("aria-busy", "true");

    if (content) {
      content.classList.add("opacity-0");
      content.classList.add("pointer-events-none");
      if (loadingText) {
        button.setAttribute("aria-label", loadingText);
      }
    }

    if (spinner) {
      spinner.classList.remove("hidden");
      spinner.classList.add("flex");
    }
  }

  function resetLoadingState(form) {
    const button = form.querySelector("[data-submit-loading-button]");
    if (!button) {
      return;
    }

    const content = button.querySelector("[data-submit-loading-content]");
    const spinner = button.querySelector("[data-submit-loading-spinner]");

    button.disabled = false;
    button.removeAttribute("aria-busy");
    button.removeAttribute("aria-label");

    if (content) {
      content.classList.remove("opacity-0");
      content.classList.remove("pointer-events-none");
    }

    if (spinner) {
      spinner.classList.add("hidden");
      spinner.classList.remove("flex");
    }
  }

  function initSubmitLoadingState() {
    const forms = document.querySelectorAll("[data-submit-loading-form]");
    forms.forEach((form) => {
      resetLoadingState(form);
      form.addEventListener("submit", function() {
        setLoadingState(form);
      });
    });
  }

  document.addEventListener("DOMContentLoaded", initSubmitLoadingState);
  window.addEventListener("pageshow", initSubmitLoadingState);
})();
