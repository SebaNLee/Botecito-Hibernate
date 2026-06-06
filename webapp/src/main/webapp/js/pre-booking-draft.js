(function () {
  const STORAGE_PREFIX = "botecito.preBookingDraft.";
  const MAX_MESSAGE_LENGTH = 255;

  function truncateText(value, max) {
    if (value === null || value === undefined) {
      return value;
    }
    const text = String(value);
    return text.length > max ? text.slice(0, max) : text;
  }

  function storageKey(itemId) {
    return STORAGE_PREFIX + itemId;
  }

  function readDraft(itemId) {
    try {
      const raw = localStorage.getItem(storageKey(itemId));
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw);
      if (!parsed || parsed.v !== 1 || String(parsed.itemId) !== String(itemId)) {
        return null;
      }
      return parsed;
    } catch (_e) {
      return null;
    }
  }

  function saveDraft(itemId, draft) {
    const payload = Object.assign({ v: 1, itemId: Number(itemId) }, draft);
    localStorage.setItem(storageKey(itemId), JSON.stringify(payload));
  }

  function clearDraft(itemId) {
    localStorage.removeItem(storageKey(itemId));
  }

  function getDetailPickers(form) {
    const dateRoot = form.querySelector("[data-date-picker]");
    const timeRoot = form.querySelector("[data-time-range-picker]");
    if (!dateRoot || !timeRoot) {
      return null;
    }
    return {
      datePicker: dateRoot.__datePicker || null,
      timePicker: timeRoot.__timeRangePicker || null,
    };
  }

  function collectDraft(form) {
    const versionInput = form.querySelector('input[name="versionId"]');
    const dateInput = form.querySelector("[data-picker-input]");
    const startInput = form.querySelector("[data-time-start-input]");
    const endInput = form.querySelector("[data-time-end-input]");
    const messageInput = form.querySelector('textarea[name="message"]');
    const toggle = form.querySelector(
      '[data-optional-toggle="detail-prebook-message-panel"]',
    );
    return {
      versionId: versionInput ? String(versionInput.value || "") : "",
      date: dateInput ? String(dateInput.value || "") : "",
      startTime: startInput ? String(startInput.value || "") : "",
      endTime: endInput ? String(endInput.value || "") : "",
      message: messageInput
        ? truncateText(String(messageInput.value || "").trim(), MAX_MESSAGE_LENGTH)
        : "",
      messageOpen: Boolean(toggle && toggle.checked),
    };
  }

  function restoreDraftToForm(form, draft) {
    const pickers = getDetailPickers(form);
    if (!pickers || !pickers.datePicker || !pickers.timePicker) {
      return;
    }

    if (draft.date) {
      pickers.datePicker.setValue(draft.date);
    }

    if (draft.startTime && draft.endTime) {
      pickers.timePicker.setSelection(draft.startTime, draft.endTime);
    } else if (draft.startTime) {
      pickers.timePicker.setSelection(draft.startTime, "");
    }

    const toggle = form.querySelector(
      '[data-optional-toggle="detail-prebook-message-panel"]',
    );
    const messageInput = form.querySelector('textarea[name="message"]');
    const wantMessage =
      Boolean(draft.messageOpen) ||
      (typeof draft.message === "string" && draft.message.length > 0);

    if (toggle && wantMessage) {
      toggle.checked = true;
      toggle.dispatchEvent(new Event("change", { bubbles: true }));
    }

    if (messageInput && typeof draft.message === "string" && draft.message.length > 0) {
      messageInput.value = truncateText(draft.message, MAX_MESSAGE_LENGTH);
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    const root = document.querySelector("[data-prebook-draft-root]");
    if (!root) {
      return;
    }

    const itemId = root.getAttribute("data-item-id");
    if (!itemId) {
      return;
    }

    const form = document.getElementById("detail-prebook-form");
    if (!form) {
      return;
    }

    const loggedIn = root.getAttribute("data-viewer-logged-in") === "true";
    const loginButton = form.querySelector(".js-prebook-login");

    if (loginButton) {
      form.addEventListener(
        "submit",
        function (event) {
          event.preventDefault();
          event.stopImmediatePropagation();
        },
        true,
      );
      loginButton.addEventListener("click", function () {
        saveDraft(itemId, collectDraft(form));
        const loginBase = root.dataset.loginUrl || "/login";
        const nextPath = "/item/" + itemId;
        const sep = loginBase.indexOf("?") >= 0 ? "&" : "?";
        window.location.assign(
          loginBase + sep + "next=" + encodeURIComponent(nextPath),
        );
      });
    }

    if (!loggedIn) {
      return;
    }

    const draft = readDraft(itemId);
    if (!draft) {
      return;
    }

    restoreDraftToForm(form, draft);
    clearDraft(itemId);
  });
})();
