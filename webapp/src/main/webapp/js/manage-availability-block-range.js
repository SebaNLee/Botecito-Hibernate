(function () {
  "use strict";

  /**
   * First click: slot index a (block starts at slots[a].start).
   * Second click: slot index b with b > a (block ends at slots[b].start, i.e. exclusive end).
   * Example: a=05:00 row, b=05:30 row → one 30-minute block 05:00–05:30.
   */

  function parseSlotsJson() {
    const el = document.getElementById("manage-availability-slots-json");
    if (!el) {
      return [];
    }
    try {
      return JSON.parse(el.textContent || "[]");
    } catch (e) {
      return [];
    }
  }

  /** Slots [a, b) must exist and all be AVAILABLE; requires b > a. */
  function contiguousExclusiveEnd(slots, a, b) {
    if (b <= a) {
      return false;
    }
    for (let i = a; i < b; i += 1) {
      if (!slots[i] || slots[i].state !== "AVAILABLE") {
        return false;
      }
    }
    return true;
  }

  function setup() {
    const root = document.querySelector("[data-personal-block-root]");
    if (!root) {
      return;
    }

    const slots = parseSlotsJson();
    const buttons = Array.from(
      root.querySelectorAll("[data-personal-block-slot]"),
    );
    if (buttons.length === 0 || slots.length === 0) {
      return;
    }

    const toolbar = root.querySelector("[data-personal-block-toolbar]");
    const hint = root.querySelector("[data-personal-block-hint]");
    const openBtn = root.querySelector("[data-personal-block-open]");
    const clearBtn = root.querySelector("[data-personal-block-clear]");
    const dialog = document.getElementById("personal-block-confirm-dialog");
    const summary = document.getElementById("personal-block-summary");
    const inpStart = document.getElementById("personal-block-input-start");
    const inpEnd = document.getElementById("personal-block-input-end");

    if (
      !toolbar ||
      !hint ||
      !openBtn ||
      !clearBtn ||
      !dialog ||
      !summary ||
      !inpStart ||
      !inpEnd
    ) {
      return;
    }

    const selectedDate = root.dataset.selectedDate || "";
    const hintPickEnd = root.dataset.hintPickEnd || "";
    const hintInvalid = root.dataset.hintInvalid || "";

    /** Start slot index. */
    let anchor = null;
    /** Exclusive end: index of first slot AFTER the blocked range (must be > anchor). */
    let endBoundIdx = null;

    function resetVisual() {
      buttons.forEach((btn) => {
        btn.classList.remove(
          "ring-2",
          "ring-inset",
          "ring-primary",
          "bg-primary/15",
        );
      });
    }

    /** Highlight covered slots [a, b) (b may be null → only index a). */
    function highlightExclusiveRange(a, b) {
      resetVisual();
      const hi = b == null ? a : b - 1;
      buttons.forEach((btn) => {
        const idx = parseInt(btn.getAttribute("data-slot-index"), 10);
        if (!Number.isNaN(idx) && idx >= a && idx <= hi) {
          btn.classList.add(
            "ring-2",
            "ring-inset",
            "ring-primary",
            "bg-primary/15",
          );
        }
      });
    }

    function updateUI() {
      const hasAnchor = anchor != null;
      const hasRange =
        hasAnchor &&
        endBoundIdx != null &&
        contiguousExclusiveEnd(slots, anchor, endBoundIdx);

      toolbar.classList.toggle("hidden", !hasAnchor);
      openBtn.disabled = !hasRange;

      if (!hasAnchor) {
        hint.textContent = "";
        return;
      }

      if (!hasRange) {
        hint.textContent = hintPickEnd;
        highlightExclusiveRange(anchor, null);
        return;
      }

      highlightExclusiveRange(anchor, endBoundIdx);
      hint.textContent = `${slots[anchor].start} – ${slots[endBoundIdx].start}`;
    }

    buttons.forEach((btn) => {
      btn.addEventListener("click", () => {
        const idx = parseInt(btn.getAttribute("data-slot-index"), 10);
        if (Number.isNaN(idx)) {
          return;
        }

        if (anchor == null || endBoundIdx != null) {
          anchor = idx;
          endBoundIdx = null;
          toolbar.classList.remove("hidden");
          updateUI();
          return;
        }

        if (idx <= anchor) {
          anchor = idx;
          endBoundIdx = null;
          updateUI();
          return;
        }

        if (!contiguousExclusiveEnd(slots, anchor, idx)) {
          anchor = idx;
          endBoundIdx = null;
          toolbar.classList.remove("hidden");
          openBtn.disabled = true;
          hint.textContent = hintInvalid;
          resetVisual();
          highlightExclusiveRange(anchor, null);
          return;
        }

        endBoundIdx = idx;
        updateUI();
      });
    });

    clearBtn.addEventListener("click", () => {
      anchor = null;
      endBoundIdx = null;
      resetVisual();
      toolbar.classList.add("hidden");
      hint.textContent = "";
      openBtn.disabled = true;
    });

    openBtn.addEventListener("click", () => {
      if (anchor == null || endBoundIdx == null) {
        return;
      }
      if (!contiguousExclusiveEnd(slots, anchor, endBoundIdx)) {
        return;
      }

      inpStart.value = slots[anchor].start;
      inpEnd.value = slots[endBoundIdx].start;

      summary.textContent = `${selectedDate} · ${slots[anchor].start} – ${slots[endBoundIdx].start}`;

      if (typeof dialog.showModal === "function") {
        dialog.showModal();
      }
    });
  }

  document.addEventListener("DOMContentLoaded", setup);
})();
