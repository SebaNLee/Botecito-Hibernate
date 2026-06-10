(function () {
  "use strict";

  function resolveGalleryRoot(rootOrForm) {
    if (!rootOrForm) {
      return null;
    }
    if (rootOrForm.matches && rootOrForm.matches("[data-image-gallery]")) {
      return rootOrForm;
    }
    if (rootOrForm.querySelector) {
      return rootOrForm.querySelector("[data-image-gallery]");
    }
    return null;
  }

  function showGalleryFileError(rootOrForm, message) {
    const root = resolveGalleryRoot(rootOrForm);
    if (!root) {
      return;
    }
    const el = root.querySelector("[data-gallery-file-error]");
    if (!el) {
      return;
    }
    el.textContent = message;
    el.classList.remove("hidden");
  }

  function clearGalleryFileError(rootOrForm) {
    const root = resolveGalleryRoot(rootOrForm);
    if (!root) {
      return;
    }
    const el = root.querySelector("[data-gallery-file-error]");
    if (!el) {
      return;
    }
    el.textContent = "";
    el.classList.add("hidden");
  }

  window.pawGalleryFileError = {
    show: showGalleryFileError,
    clear: clearGalleryFileError,
  };
})();
