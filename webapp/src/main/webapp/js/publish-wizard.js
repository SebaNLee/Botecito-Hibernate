(function () {
  "use strict";

  var STORAGE_KEY = "botecito.publishDraft.v1";
  var DRAFT_VERSION = 1;
  var MAX_TITLE_LENGTH = 100;
  var MAX_DESCRIPTION_LENGTH = 1000;

  function truncateText(value, max) {
    if (value === null || value === undefined) {
      return value;
    }
    var text = String(value);
    return text.length > max ? text.slice(0, max) : text;
  }

  function readDraft() {
    try {
      var raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      var parsed = JSON.parse(raw);
      if (!parsed || parsed.v !== DRAFT_VERSION) {
        return null;
      }
      return parsed;
    } catch (_e) {
      return null;
    }
  }

  function saveDraft(draft) {
    var payload = Object.assign({ v: DRAFT_VERSION }, draft);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  }

  function clearDraft() {
    localStorage.removeItem(STORAGE_KEY);
  }

  function parseIntField(form, name) {
    var raw = fieldValue(form, name);
    if (!raw) {
      return null;
    }
    var parsed = parseInt(raw, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }

  function collectStep1FromForm(form) {
    if (!form) {
      return {};
    }
    return {
      title: truncateText(fieldValue(form, "title"), MAX_TITLE_LENGTH),
      description: truncateText(fieldValue(form, "description"), MAX_DESCRIPTION_LENGTH),
      itemTypeId: fieldValue(form, "itemTypeId"),
      pricePerHour: fieldValue(form, "pricePerHour"),
      capacity: fieldValue(form, "capacity"),
      locationOptionId: fieldValue(form, "locationOptionId"),
      difficulty: parseIntField(form, "difficulty"),
      weight: parseIntField(form, "weight"),
    };
  }

  function mergeDraft(base, patch) {
    var merged = Object.assign({}, base || {});
    if (!patch) {
      return merged;
    }
    Object.keys(patch).forEach(function (key) {
      if (key === "availability") {
        if (patch.availability) {
          merged.availability = patch.availability;
        }
        return;
      }
      var value = patch[key];
      if (value !== null && value !== undefined && value !== "") {
        merged[key] = value;
      }
    });
    return merged;
  }

  function fieldValue(form, name) {
    var el = form.querySelector('[name="' + name + '"]');
    return el ? String(el.value || "").trim() : "";
  }

  function setFieldValue(form, name, value) {
    if (value === null || value === undefined) {
      return;
    }
    var el = form.querySelector('[name="' + name + '"]');
    if (el) {
      el.value = String(value);
      el.dispatchEvent(new Event("change", { bubbles: true }));
    }
  }

  function restoreStep1Form(form, draft) {
    if (!form || !draft) {
      return;
    }
    setFieldValue(form, "title", truncateText(draft.title, MAX_TITLE_LENGTH));
    setFieldValue(form, "description", truncateText(draft.description, MAX_DESCRIPTION_LENGTH));
    setFieldValue(form, "itemTypeId", draft.itemTypeId);
    setFieldValue(form, "pricePerHour", draft.pricePerHour);
    setFieldValue(form, "capacity", draft.capacity);
    setFieldValue(form, "locationOptionId", draft.locationOptionId);
    if (draft.difficulty != null) {
      setFieldValue(form, "difficulty", draft.difficulty);
    }
    if (draft.weight != null) {
      setFieldValue(form, "weight", draft.weight);
    }
    syncStep1Widgets(form, draft);
  }

  function syncStep1Widgets(form, draft) {
    if (!form || !draft) {
      return;
    }

    if (draft.capacity !== null && draft.capacity !== undefined && draft.capacity !== "") {
      form.querySelectorAll("[data-people-count]").forEach(function (root) {
        if (root.__peopleCount) {
          root.__peopleCount.setValue(String(draft.capacity), false);
        }
      });
    }

    syncOptionsPicker(form, "locationOptionId", draft.locationOptionId);
    syncOptionsPicker(form, "itemTypeId", draft.itemTypeId);
  }

  function syncOptionsPicker(form, fieldName, value) {
    if (value === null || value === undefined || value === "") {
      return;
    }
    var hidden = form.querySelector('[name="' + fieldName + '"]');
    if (!hidden) {
      return;
    }
    var pickerRoot = hidden.closest("[data-options-picker]");
    if (pickerRoot && pickerRoot.__optionsPicker) {
      pickerRoot.__optionsPicker.setSelectedValue(String(value));
    }
  }

  function enabledDaysFromRanges(ranges) {
    var days = [];
    var seen = {};
    (ranges || []).forEach(function (range) {
      var weekday = String(range).split("|")[0];
      if (weekday && !seen[weekday]) {
        seen[weekday] = true;
        days.push(weekday);
      }
    });
    return days;
  }

  function resolveEnabledDays(availability) {
    if (!availability) {
      return [];
    }
    if (availability.enabledDays && availability.enabledDays.length) {
      return availability.enabledDays;
    }
    return enabledDaysFromRanges(availability.ranges);
  }

  function applyExistingSlotsToGrid(grid, slots) {
    if (!grid) {
      return;
    }
    grid.querySelectorAll("[data-existing-slot]").forEach(function (node) {
      node.remove();
    });
    (slots || []).forEach(function (slot) {
      if (!slot || !slot.weekday) {
        return;
      }
      var span = document.createElement("span");
      span.className = "hidden";
      span.setAttribute("data-existing-slot", "");
      span.setAttribute("data-weekday", slot.weekday);
      span.setAttribute("data-start", slot.startTime);
      span.setAttribute("data-end", slot.endTime);
      grid.insertBefore(span, grid.firstChild);
    });
  }

  function restoreStep2Availability(grid, availability, notifyGrid) {
    if (!grid || !availability) {
      return;
    }

    applyExistingSlotsToGrid(grid, rangesToExistingSlots(availability));

    resolveEnabledDays(availability).forEach(function (day) {
      var toggle = grid.querySelector('[data-day-toggle="' + day + '"]');
      if (!toggle) {
        return;
      }
      toggle.checked = true;
      if (notifyGrid) {
        toggle.dispatchEvent(new Event("change", { bubbles: true }));
      }
    });
  }

  function rangesToExistingSlots(availability) {
    if (!availability || !availability.ranges || !availability.ranges.length) {
      return [];
    }
    var slots = [];
    availability.ranges.forEach(function (range) {
      var parts = String(range).split("|");
      if (parts.length !== 3) {
        return;
      }
      slots.push({
        weekday: parts[0],
        startTime: parts[1],
        endTime: parts[2],
      });
    });
    return slots;
  }

  function readAvailabilityRangeField(form, index, field) {
    var el = form.querySelector(
      '[name="availabilityRanges[' + index + "]." + field + '"]',
    );
    return el ? String(el.value || "").trim() : "";
  }

  function collectAvailabilityFromForm(form) {
    var enabledDays = [];
    form.querySelectorAll('input[name="enabledDays"]:checked').forEach(function (cb) {
      enabledDays.push(cb.value);
    });
    var ranges = [];
    var index = 0;
    while (true) {
      var weekday = readAvailabilityRangeField(form, index, "weekday");
      if (!weekday) {
        break;
      }
      var startTime = readAvailabilityRangeField(form, index, "startTime");
      var endTime = readAvailabilityRangeField(form, index, "endTime");
      if (startTime && endTime) {
        ranges.push(weekday + "|" + startTime + "|" + endTime);
      }
      index += 1;
    }
    return { enabledDays: enabledDays, ranges: ranges };
  }

  function appendHiddenField(container, name, value) {
    if (value === null || value === undefined || value === "") {
      return;
    }
    var input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = String(value);
    container.appendChild(input);
  }

  function injectStep1HiddenFields(container, draft) {
    if (!container || !draft) {
      return;
    }
    container.innerHTML = "";
    appendHiddenField(container, "title", truncateText(draft.title, MAX_TITLE_LENGTH));
    appendHiddenField(container, "description", truncateText(draft.description, MAX_DESCRIPTION_LENGTH));
    appendHiddenField(container, "itemTypeId", draft.itemTypeId);
    appendHiddenField(container, "pricePerHour", draft.pricePerHour);
    appendHiddenField(container, "capacity", draft.capacity);
    appendHiddenField(container, "locationOptionId", draft.locationOptionId);
    if (draft.difficulty != null) {
      appendHiddenField(container, "difficulty", draft.difficulty);
    }
    if (draft.weight != null) {
      appendHiddenField(container, "weight", draft.weight);
    }
  }

  function injectStep3Payload(form, draft) {
    if (!form || !draft) {
      return;
    }
    var hidden = form.querySelector("[data-publish-wizard-hidden-fields]");
    if (!hidden) {
      return;
    }
    hidden.innerHTML = "";
    injectStep1HiddenFields(hidden, draft);
    if (draft.availability) {
      injectAvailabilityHiddenFields(hidden, draft.availability);
    }
  }

  function injectAvailabilityHiddenFields(container, availability) {
    if (!container || !availability) {
      return;
    }
    resolveEnabledDays(availability).forEach(function (day) {
      var input = document.createElement("input");
      input.type = "hidden";
      input.name = "enabledDays";
      input.value = day;
      container.appendChild(input);
    });
    (availability.ranges || []).forEach(function (range, index) {
      var parts = String(range).split("|");
      if (parts.length !== 3) {
        return;
      }
      appendHiddenField(container, "availabilityRanges[" + index + "].weekday", parts[0]);
      appendHiddenField(container, "availabilityRanges[" + index + "].startTime", parts[1]);
      appendHiddenField(container, "availabilityRanges[" + index + "].endTime", parts[2]);
    });
  }

  function findWizardForm(root) {
    if (!root) {
      return null;
    }
    return root.querySelector("form");
  }

  function hasSelectedFiles(form) {
    if (form.querySelector("[data-gallery-local-preview] [data-gallery-item]")) {
      return true;
    }
    var inputs = form.querySelectorAll("[data-gallery-file-input], input[type='file'][name='files']");
    for (var i = 0; i < inputs.length; i++) {
      if (inputs[i].files && inputs[i].files.length > 0) {
        return true;
      }
    }
    return false;
  }

  function lockProgrammaticSubmit(form, submitButton) {
    if (!form || !submitButton) {
      return;
    }

    form.submit = function () {};

    if (typeof form.requestSubmit === "function") {
      form.requestSubmit = function () {};
    }

    form.addEventListener(
      "submit",
      function (event) {
        if (event.submitter !== submitButton) {
          event.preventDefault();
          event.stopImmediatePropagation();
        }
      },
      true,
    );
  }

  function pendingOptionsPickers(form) {
    return Array.prototype.slice
      .call(form.querySelectorAll("[data-options-picker]"))
      .map(function (root) {
        return root.__optionsPicker;
      })
      .filter(function (picker) {
        return picker && typeof picker.whenReady === "function" && !picker.isOptionsLoaded();
      });
  }

  function waitForOptionsPickersThenSubmit(form, event) {
    var pending = pendingOptionsPickers(form);
    if (!pending.length) {
      return false;
    }

    event.preventDefault();
    Promise.all(
      pending.map(function (picker) {
        return picker.whenReady();
      }),
    ).then(function () {
      if (typeof form.requestSubmit === "function") {
        form.requestSubmit(event.submitter || undefined);
      } else {
        form.submit();
      }
    });
    return true;
  }

  function initStep1(root) {
    var form = findWizardForm(root);
    if (!form) {
      return;
    }
    var draft = readDraft();
    if (draft) {
      restoreStep1Form(form, draft);
    }
    form.addEventListener("submit", function (event) {
      if (waitForOptionsPickersThenSubmit(form, event)) {
        return;
      }
      saveDraft(mergeDraft(readDraft(), collectStep1FromForm(form)));
    });
  }

  function initStep2(root) {
    var draft = readDraft();
    if (!draft || !draft.title) {
      if (root.dataset.publishUrl) {
        window.location.assign(root.dataset.publishUrl);
      }
      return;
    }

    var form = findWizardForm(root);
    var hiddenStep1 = form && form.querySelector("[data-publish-wizard-step1-hidden]");
    if (hiddenStep1) {
      injectStep1HiddenFields(hiddenStep1, draft);
    }

    var grid = root.querySelector("[data-weekly-availability-grid]");
    if (grid && draft.availability) {
      restoreStep2Availability(grid, draft.availability);
    }

    if (form) {
      form.addEventListener(
        "submit",
        function () {
          var draft = readDraft() || {};
          if (hiddenStep1) {
            injectStep1HiddenFields(hiddenStep1, draft);
          }
        },
        true,
      );

      // Register after weekly-availability.js binds its submit handler so ranges
      // are serialized into hidden inputs before we read them for localStorage.
      setTimeout(function () {
        form.addEventListener("submit", function () {
          var availability = collectAvailabilityFromForm(form);
          if (!availability.enabledDays.length && availability.ranges.length) {
            availability.enabledDays = enabledDaysFromRanges(availability.ranges);
          }
          saveDraft(mergeDraft(readDraft(), { availability: availability }));
        });
      }, 0);
    }
  }

  function initStep3(root) {
    var draft = readDraft();
    if (
      !draft ||
      !draft.title ||
      !draft.availability ||
      !draft.availability.ranges ||
      !draft.availability.ranges.length
    ) {
      if (root.dataset.availabilityUrl) {
        window.location.assign(root.dataset.availabilityUrl);
      }
      return;
    }

    var titleEl = root.querySelector("[data-publish-wizard-title]");
    if (titleEl && draft.title) {
      titleEl.textContent = draft.title;
    }

    var form = findWizardForm(root);
    if (!form) {
      return;
    }

    injectStep3Payload(form, draft);

    var submitButton = form.querySelector("[data-submit-loading-button]");
    lockProgrammaticSubmit(form, submitButton);

    var backLink = root.querySelector("[data-publish-wizard-back]");
    if (backLink) {
      backLink.addEventListener("click", function (event) {
        if (hasSelectedFiles(form)) {
          var msg = root.dataset.imagesLostMessage || "";
          if (msg && !window.confirm(msg)) {
            event.preventDefault();
          }
        }
      });
    }

    form.addEventListener(
      "submit",
      function () {
        injectStep3Payload(form, readDraft() || draft);
      },
      true,
    );
  }

  function reapplyStep2FromDraft(root) {
    var draft = readDraft();
    if (!draft || !draft.availability) {
      return;
    }
    var grid = root.querySelector("[data-weekly-availability-grid]");
    if (grid) {
      restoreStep2Availability(grid, draft.availability, true);
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-publish-wizard-clear]").forEach(function () {
      clearDraft();
    });

    document.querySelectorAll('[data-publish-wizard-root="step1"]').forEach(initStep1);
    document.querySelectorAll('[data-publish-wizard-root="step2"]').forEach(initStep2);
    document.querySelectorAll('[data-publish-wizard-root="step3"]').forEach(initStep3);
  });

  function reapplyStep1FromDraft(root) {
    var form = findWizardForm(root);
    var draft = readDraft();
    if (!form || !draft) {
      return;
    }
    restoreStep1Form(form, draft);
  }

  window.addEventListener("pageshow", function (event) {
    if (!event.persisted) {
      return;
    }
    document.querySelectorAll('[data-publish-wizard-root="step1"]').forEach(reapplyStep1FromDraft);
    document.querySelectorAll('[data-publish-wizard-root="step2"]').forEach(reapplyStep2FromDraft);
  });
})();
