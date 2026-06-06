(function () {
  "use strict";

  var STORAGE_KEY = "botecito.editDraft.v1";
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

  function readItemId(root) {
    if (!root || !root.dataset.itemId) {
      return null;
    }
    var parsed = parseInt(root.dataset.itemId, 10);
    return Number.isNaN(parsed) ? null : parsed;
  }

  function bootstrapUrl(itemId) {
    return "/edit/" + itemId;
  }

  function detailsUrl(itemId) {
    return "/edit/" + itemId + "/details";
  }

  function draftMatchesItem(draft, itemId) {
    return draft && draft.itemId != null && String(draft.itemId) === String(itemId);
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

  function readDraftForRoot(root) {
    var draft = readDraft();
    var itemId = readItemId(root);
    if (!draft || itemId == null || !draftMatchesItem(draft, itemId)) {
      return null;
    }
    return draft;
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

  function restoreStep2Availability(grid, availability, notifyGrid) {
    if (!grid || !availability) {
      return;
    }

    var slots = rangesToExistingSlots(availability);
    if (slots.length) {
      grid.dataset.existingSlots = JSON.stringify(slots);
    }

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
    var hidden = form.querySelector("[data-edit-wizard-hidden-fields]");
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

  function initStep1(root) {
    var form = findWizardForm(root);
    if (!form) {
      return;
    }
    var draft = readDraftForRoot(root);
    if (!draft) {
      var itemId = readItemId(root);
      window.location.assign(itemId == null ? "/my-boats" : bootstrapUrl(itemId));
      return;
    }
    if (draft) {
      restoreStep1Form(form, draft);
    }
    form.addEventListener("submit", function () {
      saveDraft(mergeDraft(readDraft(), collectStep1FromForm(form)));
    });
  }

  function initStep2(root) {
    var draft = readDraftForRoot(root);
    var itemId = readItemId(root);
    if (!draft || !draft.title) {
      window.location.assign(itemId == null ? "/my-boats" : bootstrapUrl(itemId));
      return;
    }

    var form = findWizardForm(root);
    var hiddenStep1 = form && form.querySelector("[data-edit-wizard-step1-hidden]");
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

  function initEditGallery(form, draft) {
    var root = form.querySelector("[data-image-gallery]");
    if (!root) {
      return;
    }

    var previewList = root.querySelector("[data-gallery-local-preview]");
    var emptyState = root.querySelector("[data-gallery-empty]");
    var input = root.querySelector("[data-gallery-file-input]");
    var orderInput = form.querySelector("[data-edit-gallery-order-input]");
    if (!previewList || !input) {
      return;
    }

    var freshInput = input.cloneNode(true);
    input.parentNode.replaceChild(freshInput, input);
    input = freshInput;

    var maxImages = parseInt(root.dataset.maxImages || "3", 10);
    var coverBadge = root.dataset.galleryCoverBadge || "";
    var badgeClass = root.dataset.galleryBadgeClass || "badge badge-primary badge-sm font-bold";
    var removeLabel = root.dataset.galleryRemoveLabel || "Remove image";
    var moveLeftLabel = root.dataset.galleryMoveLeftLabel || "Move left";
    var moveRightLabel = root.dataset.galleryMoveRightLabel || "Move right";
    var entries = [];
    var nextNewId = 0;

    (draft.images || []).forEach(function (image) {
      if (!image || image.id == null) {
        return;
      }
      entries.push({
        type: "existing",
        id: image.id,
        url: image.url,
        key: "e:" + image.id,
      });
    });

    function syncGalleryOrder() {
      if (!orderInput) {
        return;
      }
      orderInput.value = entries
        .map(function (entry) {
          return entry.key;
        })
        .join(",");
    }

    function syncInputFromEntries() {
      if (typeof DataTransfer === "undefined") {
        return;
      }
      var transfer = new DataTransfer();
      entries.forEach(function (entry) {
        if (entry.type === "new" && entry.file) {
          transfer.items.add(entry.file);
        }
      });
      input.files = transfer.files;
    }

    function createIconButton(className, icon, label, extraAttrs) {
      var button = document.createElement("button");
      button.type = "button";
      button.className = className;
      button.setAttribute("aria-label", label);
      if (extraAttrs) {
        Object.keys(extraAttrs).forEach(function (key) {
          button.setAttribute(key, extraAttrs[key]);
        });
      }
      var iconEl = document.createElement("span");
      iconEl.className = "material-symbols-outlined text-base";
      iconEl.textContent = icon;
      button.appendChild(iconEl);
      return button;
    }

    function reorderEntriesFromDom() {
      var ordered = [];
      previewList.querySelectorAll("[data-gallery-item]").forEach(function (node) {
        var key = node.getAttribute("data-gallery-key");
        var entry = entries.find(function (candidate) {
          return candidate.key === key;
        });
        if (entry) {
          ordered.push(entry);
        }
      });
      entries = ordered;
      render();
    }

    function render() {
      previewList.innerHTML = "";
      entries.forEach(function (entry, index) {
        var item = document.createElement("li");
        item.className =
          "relative rounded-xl overflow-hidden border border-outline-variant/30 bg-base-200 group";
        item.setAttribute("data-gallery-item", "");
        item.setAttribute("data-gallery-key", entry.key);

        var img = document.createElement("img");
        img.src = entry.type === "existing" ? entry.url : entry.previewUrl;
        img.alt = "";
        img.className = "w-full aspect-square object-cover block";
        item.appendChild(img);

        if (index === 0 && coverBadge) {
          var badge = document.createElement("span");
          badge.className = "absolute top-2 left-2 " + badgeClass;
          badge.textContent = coverBadge;
          item.appendChild(badge);
        }

        var controls = document.createElement("div");
        controls.className =
          "absolute inset-x-0 bottom-0 p-2 flex items-center justify-between gap-2 bg-gradient-to-t from-black/60 to-transparent opacity-100 pointer-events-auto transition-opacity duration-150 [@media(hover:hover)]:opacity-0 [@media(hover:hover)]:pointer-events-none [@media(hover:hover)]:group-hover:opacity-100 [@media(hover:hover)]:group-hover:pointer-events-auto [@media(hover:hover)]:group-focus-within:opacity-100 [@media(hover:hover)]:group-focus-within:pointer-events-auto";

        var moveGroup = document.createElement("div");
        moveGroup.className = "flex gap-1";
        var moveLeft = createIconButton(
          "btn btn-xs btn-circle btn-ghost text-white",
          "chevron_left",
          moveLeftLabel,
          { "data-gallery-move": "left" },
        );
        if (index === 0) {
          moveLeft.disabled = true;
        }
        var moveRight = createIconButton(
          "btn btn-xs btn-circle btn-ghost text-white",
          "chevron_right",
          moveRightLabel,
          { "data-gallery-move": "right" },
        );
        if (index === entries.length - 1) {
          moveRight.disabled = true;
        }
        moveGroup.appendChild(moveLeft);
        moveGroup.appendChild(moveRight);

        var removeButton = createIconButton(
          "btn btn-xs btn-circle btn-error",
          "close",
          removeLabel,
          { "data-edit-gallery-remove": entry.key },
        );

        controls.appendChild(moveGroup);
        controls.appendChild(removeButton);
        item.appendChild(controls);
        previewList.appendChild(item);
      });

      var hasEntries = entries.length > 0;
      previewList.classList.toggle("hidden", !hasEntries);
      if (emptyState) {
        emptyState.classList.toggle("hidden", hasEntries);
      }
      var uploadLabel = input.closest("label");
      if (uploadLabel) {
        uploadLabel.classList.toggle("hidden", entries.length >= maxImages);
      }
      syncGalleryOrder();
      syncInputFromEntries();
      bindGalleryInteractions();
    }

    function bindGalleryInteractions() {
      previewList.querySelectorAll("[data-gallery-move]").forEach(function (button) {
        button.addEventListener("click", function (event) {
          event.preventDefault();
          var item = button.closest("[data-gallery-item]");
          if (!item) {
            return;
          }
          var direction = button.getAttribute("data-gallery-move");
          var sibling = direction === "left" ? item.previousElementSibling : item.nextElementSibling;
          if (!sibling) {
            return;
          }
          if (direction === "left") {
            previewList.insertBefore(item, sibling);
          } else {
            previewList.insertBefore(sibling, item);
          }
          reorderEntriesFromDom();
        });
      });

      previewList.querySelectorAll("[data-edit-gallery-remove]").forEach(function (button) {
        button.addEventListener("click", function (event) {
          event.preventDefault();
          var key = button.getAttribute("data-edit-gallery-remove");
          entries = entries.filter(function (entry) {
            return entry.key !== key;
          });
          render();
        });
      });

      var draggedItem = null;
      previewList.querySelectorAll("[data-gallery-item]").forEach(function (item) {
        item.setAttribute("draggable", "true");
        item.addEventListener("dragstart", function (event) {
          draggedItem = item;
          if (event.dataTransfer) {
            event.dataTransfer.effectAllowed = "move";
            event.dataTransfer.setData("text/plain", item.getAttribute("data-gallery-key") || "");
          }
          item.classList.add("opacity-50");
        });
        item.addEventListener("dragend", function () {
          item.classList.remove("opacity-50");
          draggedItem = null;
        });
        item.addEventListener("dragover", function (event) {
          event.preventDefault();
          if (event.dataTransfer) {
            event.dataTransfer.dropEffect = "move";
          }
        });
        item.addEventListener("drop", function (event) {
          event.preventDefault();
          if (!draggedItem || draggedItem === item) {
            return;
          }
          var items = Array.prototype.slice.call(
            previewList.querySelectorAll("[data-gallery-item]"),
          );
          var draggedIndex = items.indexOf(draggedItem);
          var targetIndex = items.indexOf(item);
          if (draggedIndex < targetIndex) {
            previewList.insertBefore(draggedItem, item.nextElementSibling);
          } else {
            previewList.insertBefore(draggedItem, item);
          }
          reorderEntriesFromDom();
        });
      });
    }

    input.addEventListener("change", function () {
      var picked = Array.prototype.slice.call(input.files || []);
      input.value = "";
      var remaining = maxImages - entries.length;
      picked.forEach(function (file) {
        if (remaining <= 0) {
          return;
        }
        if (!file || !file.type || file.type.indexOf("image/") !== 0) {
          return;
        }
        var key = "n:" + nextNewId++;
        var previewUrl =
          typeof URL !== "undefined" && typeof URL.createObjectURL === "function"
            ? URL.createObjectURL(file)
            : "";
        entries.push({ type: "new", file: file, key: key, previewUrl: previewUrl });
        remaining -= 1;
      });
      render();
    });

    form.addEventListener(
      "submit",
      function () {
        syncGalleryOrder();
        syncInputFromEntries();
      },
      true,
    );

    render();
  }

  function initStep3(root) {
    var draft = readDraftForRoot(root);
    var itemId = readItemId(root);
    if (
      !draft ||
      !draft.title ||
      !draft.availability ||
      !draft.availability.ranges ||
      !draft.availability.ranges.length
    ) {
      window.location.assign(
        itemId == null
          ? "/my-boats"
          : root.dataset.editAvailabilityUrl || "/edit/" + itemId + "/availability",
      );
      return;
    }

    var titleEl = root.querySelector("[data-edit-wizard-title]");
    if (titleEl && draft.title) {
      titleEl.textContent = draft.title;
    }

    var form = findWizardForm(root);
    if (!form) {
      return;
    }

    injectStep3Payload(form, draft);

    setTimeout(function () {
      initEditGallery(form, readDraftForRoot(root) || draft);
    }, 0);

    var submitButton = form.querySelector("[data-submit-loading-button]");
    lockProgrammaticSubmit(form, submitButton);

    var backLink = root.querySelector("[data-edit-wizard-back]");
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
    var draft = readDraftForRoot(root);
    if (!draft || !draft.availability) {
      return;
    }
    var grid = root.querySelector("[data-weekly-availability-grid]");
    if (grid) {
      restoreStep2Availability(grid, draft.availability, true);
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-edit-wizard-clear]").forEach(function () {
      clearDraft();
    });

    document.querySelectorAll('[data-edit-wizard-root="step1"]').forEach(initStep1);
    document.querySelectorAll('[data-edit-wizard-root="step2"]').forEach(initStep2);
    document.querySelectorAll('[data-edit-wizard-root="step3"]').forEach(initStep3);
  });

  function reapplyStep1FromDraft(root) {
    var form = findWizardForm(root);
    var draft = readDraftForRoot(root);
    if (!form || !draft) {
      return;
    }
    restoreStep1Form(form, draft);
  }

  window.addEventListener("pageshow", function (event) {
    if (!event.persisted) {
      return;
    }
    document.querySelectorAll('[data-edit-wizard-root="step1"]').forEach(reapplyStep1FromDraft);
    document.querySelectorAll('[data-edit-wizard-root="step2"]').forEach(reapplyStep2FromDraft);
  });
})();
