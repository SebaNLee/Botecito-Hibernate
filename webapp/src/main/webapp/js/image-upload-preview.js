(function () {
  function bindImagePreviewInput(input) {
    var previewId = input.getAttribute("data-image-preview-target-id");
    var fileNameId = input.getAttribute("data-image-preview-filename-id");
    var placeholderSrc = input.getAttribute("data-image-preview-placeholder") || "";

    var previewElement = previewId ? document.getElementById(previewId) : null;
    if (!previewElement) {
      return;
    }

    var fileNameElement = fileNameId ? document.getElementById(fileNameId) : null;
    var defaultFileNameText = fileNameElement ? fileNameElement.textContent : "";
    var currentObjectUrl = null;

    function resetPreview() {
      if (currentObjectUrl) {
        URL.revokeObjectURL(currentObjectUrl);
        currentObjectUrl = null;
      }
      previewElement.src = placeholderSrc;
      if (fileNameElement) {
        fileNameElement.textContent = defaultFileNameText;
      }
    }

    input.addEventListener("change", function () {
      var file = input.files && input.files[0] ? input.files[0] : null;

      if (!file) {
        resetPreview();
        return;
      }

      if (fileNameElement) {
        fileNameElement.textContent = file.name;
      }

      if (!file.type || file.type.indexOf("image/") !== 0) {
        resetPreview();
        return;
      }

      if (typeof URL === "undefined" || typeof URL.createObjectURL !== "function") {
        resetPreview();
        return;
      }

      if (currentObjectUrl) {
        URL.revokeObjectURL(currentObjectUrl);
      }
      currentObjectUrl = URL.createObjectURL(file);
      previewElement.src = currentObjectUrl;
    });

    window.addEventListener("beforeunload", function () {
      if (currentObjectUrl) {
        URL.revokeObjectURL(currentObjectUrl);
        currentObjectUrl = null;
      }
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    var inputs = document.querySelectorAll("[data-image-preview-input]");
    for (var i = 0; i < inputs.length; i += 1) {
      bindImagePreviewInput(inputs[i]);
    }
  });
})();
