(function () {
  function toInt(value, fallback) {
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? fallback : parsed;
  }

  function clampRating(value) {
    return Math.min(5, Math.max(0, value));
  }

  function updateStars(stars, value) {
    stars.forEach((button) => {
      const starValue = toInt(button.dataset.ratingStar, 0);
      const icon = button.querySelector(".material-symbols-outlined");
      const selected = starValue <= value && value > 0;
      button.setAttribute("aria-pressed", selected ? "true" : "false");
      if (icon) {
        icon.textContent = "star";
        icon.classList.toggle("icon-star-filled", selected);
        icon.classList.toggle("icon-star-outline", !selected);
      }
    });
  }

  function bindRatingRoot(root) {
    const input = root.querySelector("[data-rating-value]");
    const stars = Array.from(root.querySelectorAll("[data-rating-star]"));

    if (!input || stars.length === 0) {
      return;
    }

    let committedValue = clampRating(toInt(input.value, 0));

    function commit(value) {
      committedValue = clampRating(value);
      input.value = committedValue > 0 ? String(committedValue) : "";
      updateStars(stars, committedValue);
    }

    function preview(value) {
      updateStars(stars, clampRating(value));
    }

    stars.forEach((button) => {
      const value = clampRating(toInt(button.dataset.ratingStar, 0));

      button.addEventListener("click", () => {
        commit(value);
      });

      button.addEventListener("mouseenter", () => {
        preview(value);
      });

      button.addEventListener("focus", () => {
        preview(value);
      });

      button.addEventListener("keydown", (event) => {
        if (event.key === "ArrowRight" || event.key === "ArrowUp") {
          event.preventDefault();
          const next = Math.min(5, committedValue + 1);
          commit(next);
          const nextButton = stars[next - 1];
          if (nextButton) {
            nextButton.focus();
          }
        }

        if (event.key === "ArrowLeft" || event.key === "ArrowDown") {
          event.preventDefault();
          const next = Math.max(1, committedValue - 1);
          commit(next);
          const nextButton = stars[next - 1];
          if (nextButton) {
            nextButton.focus();
          }
        }
      });
    });

    root.addEventListener("mouseleave", () => {
      preview(committedValue);
    });

    root.addEventListener("focusout", (event) => {
      if (!root.contains(event.relatedTarget)) {
        preview(committedValue);
      }
    });

    preview(committedValue);
  }

  document.querySelectorAll("[data-rating-stars]").forEach(bindRatingRoot);
})();
