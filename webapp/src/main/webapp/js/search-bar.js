(function () {
  function hasSearchText(input) {
    return Boolean((input.value || "").trim());
  }

  function syncClearButton(root, input, clearButton) {
    const visible = hasSearchText(input);
    clearButton.classList.toggle("opacity-0", !visible);
    clearButton.classList.toggle("pointer-events-none", !visible);
  }

  function resetPageField(form) {
    const pageInput = form.querySelector('[name="page"]');
    if (pageInput) {
      pageInput.value = "1";
    }
  }

  function submitLinkedForm(input) {
    const formId = input.getAttribute("form");
    const form = formId ? document.getElementById(formId) : input.closest("form");
    if (!form) {
      return;
    }

    resetPageField(form);

    if (typeof form.requestSubmit === "function") {
      form.requestSubmit();
      return;
    }

    form.submit();
  }

  function bindSearchBar(root) {
    const input = root.querySelector('input[type="search"]');
    const clearButton = root.querySelector("[data-search-clear]");
    if (!input || !clearButton) {
      return;
    }

    const sync = () => syncClearButton(root, input, clearButton);

    input.addEventListener("input", sync);
    sync();

    clearButton.addEventListener("click", (event) => {
      event.preventDefault();
      if (!hasSearchText(input)) {
        return;
      }

      input.value = "";
      input.dispatchEvent(new Event("input", { bubbles: true }));
      input.dispatchEvent(new Event("change", { bubbles: true }));
      sync();
      submitLinkedForm(input);
    });
  }

  document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-search-bar]").forEach(bindSearchBar);
  });
})();
