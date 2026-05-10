(function () {
  const FILTER_KEYS = [
    "searchQuery",
    "location",
    "itemType",
    "date",
    "startTime",
    "endTime",
    "capacity",
    "maxWeight",
    "difficultyLevel",
    "minAvgRating",
  ];

  /** Query params restored with marketplace listing (sidebar + toolbar). */
  const MARKETPLACE_TOOLBAR_KEYS = ["sortBy", "pageSize"];
  const APPLIED_FILTERS_KEY = "paw.marketplaceFilters";
  const DRAFT_FILTERS_KEY = "paw.marketplaceFilterDraft";
  /** @type {Map<string, Promise<Array<{ value: string, name: string }>>>} */
  const selectableOptionsByUrl = new Map();

  function parseJson(value, fallback) {
    try {
      return value ? JSON.parse(value) : fallback;
    } catch (error) {
      return fallback;
    }
  }

  function normalizeText(value) {
    return (value || "").toString().trim().toLowerCase();
  }

  /** Marketplace `location` must be a slug; strip legacy numeric-only ids from URL/session. */
  function normalizeMarketplaceLocation(value) {
    const raw = (value || "").toString().trim();
    if (!raw) {
      return "";
    }
    if (/^\d+$/.test(raw)) {
      return "";
    }
    return raw;
  }

  function openUnavailableAlert(alertRoot) {
    if (!alertRoot) {
      return;
    }

    alertRoot.hidden = false;
    if (typeof alertRoot.showModal === "function") {
      if (!alertRoot.open) {
        alertRoot.showModal();
      }
      return;
    }

    alertRoot.classList.remove("hidden");
    alertRoot.classList.add("flex");
  }

  function closeUnavailableAlert(alertRoot) {
    if (!alertRoot) {
      return;
    }

    if (typeof alertRoot.close === "function" && alertRoot.open) {
      alertRoot.close();
    }
    alertRoot.hidden = true;
    alertRoot.classList.add("hidden");
    alertRoot.classList.remove("flex");
  }

  function normalizeState(state) {
    const normalized = {};

    FILTER_KEYS.forEach((key) => {
      normalized[key] = state && state[key] ? String(state[key]).trim() : "";
    });
    normalized.location = normalizeMarketplaceLocation(normalized.location);
    normalized.itemType = normalizeMarketplaceLocation(normalized.itemType);

    MARKETPLACE_TOOLBAR_KEYS.forEach((key) => {
      if (!state || !Object.prototype.hasOwnProperty.call(state, key)) {
        return;
      }
      normalized[key] =
        state[key] != null && String(state[key]).trim() !== ""
          ? String(state[key]).trim()
          : "";
    });

    return normalized;
  }

  function hasAnyFilter(state) {
    return FILTER_KEYS.some((key) => normalizeText(state[key]));
  }

  function hasPersistableMarketplaceDraft(normalized) {
    if (hasAnyFilter(normalized)) {
      return true;
    }
    return MARKETPLACE_TOOLBAR_KEYS.some(
      (key) =>
        Object.prototype.hasOwnProperty.call(normalized, key) &&
        normalizeText(normalized[key]),
    );
  }

  function readStoredState(storageKey) {
    try {
      const raw = parseJson(sessionStorage.getItem(storageKey), {}) || {};
      if (raw.locationSlug && !raw.location) {
        raw.location = raw.locationSlug;
      }
      if (raw.itemTypeSlug && !raw.itemType) {
        raw.itemType = raw.itemTypeSlug;
      }
      return normalizeState(raw);
    } catch (error) {
      return normalizeState({});
    }
  }

  function writeStoredState(storageKey, state) {
    let merged = {};
    try {
      merged = parseJson(sessionStorage.getItem(storageKey), {}) || {};
    } catch (error) {
      merged = {};
    }
    merged = { ...merged, ...state };
    const normalized = normalizeState(merged);

    try {
      if (hasPersistableMarketplaceDraft(normalized)) {
        sessionStorage.setItem(storageKey, JSON.stringify(normalized));
      } else {
        sessionStorage.removeItem(storageKey);
      }
    } catch (error) {
      // Ignore storage failures.
    }
  }

  function clearStoredStates() {
    try {
      sessionStorage.removeItem(APPLIED_FILTERS_KEY);
      sessionStorage.removeItem(DRAFT_FILTERS_KEY);
    } catch (error) {
      // Ignore storage failures.
    }
  }

  function parseInteger(value) {
    const parsed = Number.parseInt(String(value || "").trim(), 10);
    return Number.isFinite(parsed) ? parsed : null;
  }

  function buildUrlWithFilters(baseUrl, state) {
    const url = new URL(baseUrl, window.location.origin);
    const normalized = normalizeState(state);

    FILTER_KEYS.forEach((key) => {
      if (normalized[key]) {
        url.searchParams.set(key, normalized[key]);
      } else {
        url.searchParams.delete(key);
      }
    });

    MARKETPLACE_TOOLBAR_KEYS.forEach((key) => {
      if (
        Object.prototype.hasOwnProperty.call(normalized, key) &&
        normalized[key]
      ) {
        url.searchParams.set(key, normalized[key]);
      } else {
        url.searchParams.delete(key);
      }
    });

    return url.toString();
  }

  function readUrlState() {
    const params = new URLSearchParams(window.location.search);
    const state = FILTER_KEYS.reduce((acc, key) => {
      acc[key] = params.get(key) || "";
      return acc;
    }, {});
    if (!state.location && params.get("locationSlug")) {
      state.location = params.get("locationSlug") || "";
    }
    if (!state.itemType && params.get("itemTypeSlug")) {
      state.itemType = params.get("itemTypeSlug") || "";
    }
    MARKETPLACE_TOOLBAR_KEYS.forEach((key) => {
      if (params.has(key)) {
        state[key] = params.get(key) || "";
      }
    });
    return normalizeState(state);
  }

  function getDateTimeControls(scope) {
    const rootScope = scope || document;
    const dateRoot = rootScope.querySelector("[data-date-picker]");
    const timeRoot = rootScope.querySelector("[data-time-range-picker]");

    if (!dateRoot || !timeRoot) {
      return null;
    }

    return {
      datePicker: dateRoot.__datePicker || null,
      timePicker: timeRoot.__timeRangePicker || null,
      dateInput: dateRoot.querySelector("[data-picker-input]"),
      startInput: timeRoot.querySelector("[data-time-start-input]"),
      endInput: timeRoot.querySelector("[data-time-end-input]"),
    };
  }

  function readDateTimeState(scope) {
    const controls = getDateTimeControls(scope);
    if (!controls) {
      return {};
    }

    return {
      date: controls.dateInput ? controls.dateInput.value || "" : "",
      startTime: controls.startInput ? controls.startInput.value || "" : "",
      endTime: controls.endInput ? controls.endInput.value || "" : "",
    };
  }

  function setDateTimeState(scope, state) {
    const controls = getDateTimeControls(scope);
    if (!controls || !controls.datePicker || !controls.timePicker) {
      return;
    }

    if (state.date) {
      controls.datePicker.setValue(state.date);
    } else {
      controls.datePicker.clear();
    }

    if (state.startTime && state.endTime) {
      controls.timePicker.setSelection(state.startTime, state.endTime);
    } else if (state.startTime) {
      if (!controls.timePicker.setSelection(state.startTime, "")) {
        controls.timePicker.startInput.value = state.startTime;
        controls.timePicker.endInput.value = "";
        controls.timePicker.dispatchSelectionChange();
        controls.timePicker.render();
      }
    } else {
      controls.timePicker.clear();
    }
  }

  function bindDateTimeDraftPersistence(scope, callback) {
    const controls = getDateTimeControls(scope);
    if (!controls) {
      return;
    }

    [controls.dateInput, controls.startInput, controls.endInput]
      .filter(Boolean)
      .forEach((input) => {
        input.addEventListener("change", callback);
      });
  }

  function readMarketplacePersistSnapshot() {
    const filterForm = document.querySelector('[data-filter-form="marketplace"]');
    const toolbarForm = document.querySelector("[data-marketplace-toolbar-form]");
    if (!filterForm && !toolbarForm) {
      return normalizeState({});
    }
    const filters = filterForm ? readFilterStateFromForm(filterForm) : normalizeState({});
    const params = new URLSearchParams(window.location.search);
    const sortBy =
      toolbarForm?.querySelector('[name="sortBy"]')?.value?.trim() ||
      (params.has("sortBy") ? String(params.get("sortBy") || "").trim() : "");
    const pageSize =
      toolbarForm?.querySelector('[name="pageSize"]')?.value?.trim() ||
      filterForm?.querySelector('[name="pageSize"]')?.value?.trim() ||
      (params.has("pageSize") ? String(params.get("pageSize") || "").trim() : "");
    const extra = {};
    if (sortBy) {
      extra.sortBy = sortBy;
    }
    if (pageSize) {
      extra.pageSize = pageSize;
    }
    return normalizeState({ ...filters, ...extra });
  }

  function hydrateMarketplaceToolbar(toolbarForm, state) {
    if (!toolbarForm || !state) {
      return;
    }
    const sortEl = toolbarForm.querySelector('[name="sortBy"]');
    if (
      sortEl &&
      Object.prototype.hasOwnProperty.call(state, "sortBy") &&
      state.sortBy
    ) {
      sortEl.value = String(state.sortBy);
    }
    const pageSizeEl = toolbarForm.querySelector('[name="pageSize"]');
    if (
      pageSizeEl &&
      Object.prototype.hasOwnProperty.call(state, "pageSize") &&
      state.pageSize
    ) {
      pageSizeEl.value = String(state.pageSize);
    }
  }

  function navigateMarketplaceToolbar(toolbarForm) {
    const snapshot = readMarketplacePersistSnapshot();
    writeStoredState(DRAFT_FILTERS_KEY, snapshot);
    writeStoredState(APPLIED_FILTERS_KEY, snapshot);
    reflectAppliedState(snapshot);
    const action = toolbarForm.getAttribute("action") || "/marketplace";
    const merged = new URL(buildUrlWithFilters(action, snapshot), window.location.origin);
    merged.searchParams.set("page", "1");
    window.location.assign(merged.toString());
  }

  function bindMarketplaceToolbarForm(toolbarForm) {
    if (!toolbarForm) {
      return;
    }

    toolbarForm.addEventListener("submit", (event) => {
      event.preventDefault();
      navigateMarketplaceToolbar(toolbarForm);
    });

    toolbarForm.querySelectorAll('[name="sortBy"], [name="pageSize"]').forEach((el) => {
      el.addEventListener("change", () => {
        if (typeof toolbarForm.requestSubmit === "function") {
          toolbarForm.requestSubmit();
        } else {
          navigateMarketplaceToolbar(toolbarForm);
        }
      });
    });
  }

  function readFilterStateFromForm(form) {
    if (!form) {
      return normalizeState({});
    }

    return normalizeState({
      searchQuery:
        form.querySelector('[name="searchQuery"]')?.value ||
        document.querySelector(
          '[data-marketplace-search-input][form="' +
            form.getAttribute("id") +
            '"]',
        )?.value ||
        "",
      location:
        form.querySelector('[data-option-value][name="location"]')?.value ||
        form.querySelector('[name="location"]')?.value ||
        "",
      itemType:
        form.querySelector('[data-option-value][name="itemType"]')?.value ||
        form.querySelector('[name="itemType"]')?.value ||
        "",
      date:
        form.querySelector("[data-picker-input]")?.value ||
        form.querySelector('[name="date"]')?.value ||
        "",
      startTime:
        form.querySelector("[data-time-start-input]")?.value ||
        form.querySelector('[name="startTime"]')?.value ||
        "",
      endTime:
        form.querySelector("[data-time-end-input]")?.value ||
        form.querySelector('[name="endTime"]')?.value ||
        "",
      capacity:
        form.querySelector("[data-people-input]")?.value ||
        form.querySelector('[name="capacity"]')?.value ||
        "",
      maxWeight:
        form.querySelector("[data-weight-value-input]")?.value ||
        form.querySelector('[name="maxWeight"]')?.value ||
        "",
      difficultyLevel:
        form.querySelector('[name="difficultyLevel"]')?.value || "",
      minAvgRating:
        form.querySelector("[data-min-rating-value-input]")?.value ||
        form.querySelector('[name="minAvgRating"]')?.value ||
        "",
    });
  }

  function reflectAppliedState(state) {
    const normalized = normalizeState(state);

    document
      .querySelectorAll("[data-applied-filter-mirror]")
      .forEach((input) => {
        input.value = normalized[input.name] || "";
      });

    document
      .querySelectorAll("[data-marketplace-item-link]")
      .forEach((link) => {
        if (!link.dataset.baseHref) {
          link.dataset.baseHref = link.getAttribute("href") || "";
        }

        link.setAttribute(
          "href",
          buildUrlWithFilters(link.dataset.baseHref, normalized),
        );
      });

    const toolbarForm = document.querySelector("[data-marketplace-toolbar-form]");
    hydrateMarketplaceToolbar(toolbarForm, normalized);
  }

  function formatMismatchReasons(reasons, conjunction) {
    const joinWith = conjunction || "and";
    if (reasons.length === 1) {
      return reasons[0];
    }

    if (reasons.length === 2) {
      return reasons[0] + " " + joinWith + " " + reasons[1];
    }

    return (
      reasons.slice(0, -1).join(", ") +
      " " +
      joinWith +
      " " +
      reasons[reasons.length - 1]
    );
  }

  function fetchSelectableOptions(url, slugMode) {
    const resolved = (url || "/location-options").trim() || "/location-options";
    const baseKey = new URL(resolved, window.location.origin).href;
    const cacheKey = baseKey + (slugMode ? "#slug" : "#id");
    if (!selectableOptionsByUrl.has(cacheKey)) {
      selectableOptionsByUrl.set(
        cacheKey,
        fetch(resolved)
          .then((response) => (response.ok ? response.json() : []))
          .catch(() => [])
          .then((options) => {
            if (!Array.isArray(options)) {
              return [];
            }
            if (slugMode) {
              return options
                .filter(
                  (option) =>
                    option &&
                    typeof option.name === "string" &&
                    typeof option.slug === "string" &&
                    option.slug.trim() !== "",
                )
                .map((option) => ({
                  value: String(option.slug).trim(),
                  name: option.name,
                }));
            }
            return options
              .filter(
                (option) =>
                  option &&
                  typeof option.name === "string" &&
                  Object.prototype.hasOwnProperty.call(option, "id") &&
                  option.id != null &&
                  String(option.id).trim() !== "",
              )
              .map((option) => ({
                value: String(option.id).trim(),
                name: option.name,
              }));
          }),
      );
    }

    return selectableOptionsByUrl.get(cacheKey);
  }

  class OptionsPicker {
    constructor(root) {
      this.root = root;
      this.hiddenInput = root.querySelector("[data-option-value]");
      this.queryInput = root.querySelector("[data-option-query]");
      this.trigger = root.querySelector("[data-option-trigger]");
      this.clearButton = root.querySelector("[data-option-clear]");
      this.chevron = root.querySelector("[data-option-chevron]");
      this.panel = root.querySelector("[data-option-panel]");
      this.optionsContainer = root.querySelector("[data-option-options]");
      this.emptyState = root.querySelector("[data-option-empty]");
      this.options = [];
      this.root.__optionsPicker = this;
      this.bind();
      this.bindSubmitToForm();
      this.loadOptions();
    }

    /** Sync hidden location id before submit; optionally require a selection (publish). */
    bindSubmitToForm() {
      const form = this.root.closest("form");
      if (!form || !this.hiddenInput || !this.queryInput) {
        return;
      }

      this._formSubmitListener = (event) => {
        this.prepareForSubmit();
        if (
          this.hiddenInput.name === "location" ||
          this.hiddenInput.name === "itemType"
        ) {
          this.hiddenInput.value = normalizeMarketplaceLocation(
            this.hiddenInput.value,
          );
        }
        const msg = (this.root.dataset.requiredMessage || "").trim();
        if (msg && !String(this.hiddenInput.value || "").trim()) {
          event.preventDefault();
          this.queryInput.setCustomValidity(msg);
          this.queryInput.reportValidity();
          this.queryInput.setCustomValidity("");
          this.queryInput.focus();
        }
      };
      form.addEventListener("submit", this._formSubmitListener);
    }

    bind() {
      if (this.trigger) {
        this.trigger.addEventListener("click", (event) => {
          if (event.target.closest("[data-option-clear]")) {
            return;
          }

          this.open();
          this.queryInput.focus();
          this.render();
        });
      }

      this.queryInput.addEventListener("focus", () => {
        this.open();
        this.render();
      });

      this.queryInput.addEventListener("click", () => {
        this.open();
        this.render();
      });

      this.queryInput.addEventListener("input", () => {
        const exactMatch = this.findExact(this.queryInput.value);
        this.hiddenInput.value = exactMatch ? exactMatch.value : "";
        this.hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
        this.open();
        this.render();
      });

      this.queryInput.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
          this.close();
        }
      });

      if (this.clearButton) {
        this.clearButton.addEventListener("click", (event) => {
          event.preventDefault();
          event.stopPropagation();
          this.clear();
        });
      }

      document.addEventListener("click", (event) => {
        if (!event.target.closest("[data-options-picker]")) {
          this.close();
        }
      });
    }

    loadOptions() {
      const slugMode =
        this.hiddenInput.name === "location" ||
        this.hiddenInput.name === "itemType";
      fetchSelectableOptions(
        this.root.dataset.optionsUrl || "/location-options",
        slugMode,
      ).then((options) => {
        this.options = options;
        this.setSelectedValue(this.hiddenInput.value);
        this.render();
      });
    }

    getFilteredOptions() {
      const query = normalizeText(this.queryInput.value);
      if (!query) {
        return this.options;
      }

      return this.options.filter((option) =>
        normalizeText(option.name).includes(query),
      );
    }

    findExact(value) {
      const normalized = normalizeText(value);
      const match = this.options.find(
        (option) => normalizeText(option.name) === normalized,
      );
      return match || null;
    }

    setSelectedValue(value) {
      const slugMode =
        this.hiddenInput.name === "location" ||
        this.hiddenInput.name === "itemType";
      const normalized = slugMode
        ? normalizeMarketplaceLocation(value)
        : (value || "").toString().trim();
      this.hiddenInput.value = normalized;
      const selected = this.options.find(
        (option) => String(option.value) === String(normalized || ""),
      );
      this.queryInput.value = selected ? selected.name : "";
      this.render();
    }

    clear() {
      this.hiddenInput.value = "";
      this.queryInput.value = "";
      this.hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
      this.open();
      this.render();
      this.queryInput.focus();
    }

    prepareForSubmit() {
      const slugMode =
        this.hiddenInput.name === "location" ||
        this.hiddenInput.name === "itemType";
      const exactMatch = this.findExact(this.queryInput.value);
      const raw = exactMatch ? exactMatch.value : "";
      this.hiddenInput.value = slugMode
        ? normalizeMarketplaceLocation(raw)
        : raw;
      this.queryInput.value = exactMatch
        ? exactMatch.name
        : this.queryInput.value || "";
    }

    open() {
      this.panel.hidden = false;
      this.panel.classList.remove("hidden");
      this.root.style.zIndex = "10010";
      this.chevron?.classList.add("rotate-180");
    }

    close() {
      this.panel.hidden = true;
      this.panel.classList.add("hidden");
      this.root.style.zIndex = "";
      this.chevron?.classList.remove("rotate-180");
    }

    selectOption(option) {
      if (String(option.value) === String(this.hiddenInput.value)) {
        this.clear();
        return;
      }

      this.hiddenInput.value = String(option.value);
      this.queryInput.value = option.name;
      this.hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
      this.close();
      this.render();
    }

    render() {
      if (!this.optionsContainer) {
        return;
      }

      const filteredOptions = this.getFilteredOptions();
      this.optionsContainer.innerHTML = "";

      filteredOptions.forEach((option) => {
        const button = document.createElement("button");
        const isSelected =
          String(option.value) === String(this.hiddenInput.value);
        button.type = "button";
        button.className =
          "flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm font-medium text-on-surface transition-all hover:bg-primary/10 hover:text-primary active:bg-primary/14";
        if (isSelected) {
          button.classList.add(
            "bg-primary/10",
            "text-primary",
            "shadow-[inset_0_0_0_1px_rgba(0,93,167,0.18)]",
          );
        }
        button.addEventListener("mousedown", (event) => {
          event.preventDefault();
          this.selectOption(option);
        });

        const content = document.createElement("div");
        content.className = "min-w-0 flex-1";
        const title = document.createElement("div");
        title.className = "truncate";
        title.textContent = option.name;
        content.appendChild(title);
        button.appendChild(content);

        this.optionsContainer.appendChild(button);
      });

      const hasMatches = filteredOptions.length > 0;
      this.emptyState.classList.toggle("hidden", hasMatches);
      this.optionsContainer.classList.toggle("hidden", !hasMatches);
      this.clearButton?.classList.toggle(
        "opacity-0",
        !this.queryInput.value && !this.hiddenInput.value,
      );
      this.clearButton?.classList.toggle(
        "pointer-events-none",
        !this.queryInput.value && !this.hiddenInput.value,
      );
    }
  }

  class PeopleCount {
    constructor(root) {
      this.root = root;
      this.input = root.querySelector("[data-people-input]");
      this.valueField = root.querySelector("[data-people-field]");
      this.decrementButton = root.querySelector("[data-people-decrement]");
      this.clearButton = root.querySelector("[data-people-clear]");
      this.incrementButton = root.querySelector("[data-people-increment]");
      this.min = parseInteger(root.dataset.min) ?? 1;
      this.max = parseInteger(root.dataset.max) ?? 20;
      this.step = parseInteger(root.dataset.step) ?? 1;
      this.allowEmpty = root.dataset.allowEmpty === "true";
      this.placeholder = root.dataset.placeholder || "Cuantos?";
      this.root.__peopleCount = this;
      this.bind();
      this.syncDisplay();
    }

    bind() {
      this.decrementButton.addEventListener("click", () => {
        this.adjust(-1);
      });

      if (this.clearButton) {
        this.clearButton.addEventListener("click", () => {
          this.setValue("");
        });
      }

      if (this.valueField) {
        this.valueField.addEventListener("input", () => {
          const digitsOnly = this.valueField.value.replace(/\D+/g, "");
          this.valueField.value = digitsOnly;

          if (!digitsOnly) {
            this.setValue("");
            return;
          }

          this.setValue(digitsOnly);
        });

        this.valueField.addEventListener("blur", () => {
          const current = this.currentValue();
          if (current == null) {
            this.setValue("");
            return;
          }

          this.setValue(current);
        });
      }

      this.incrementButton.addEventListener("click", () => {
        this.adjust(1);
      });
    }

    currentValue() {
      return parseInteger(this.input.value);
    }

    setValue(value, shouldDispatch = true) {
      const parsedValue = parseInteger(value);
      const normalizedValue =
        value === "" || value == null || parsedValue == null
          ? ""
          : String(Math.min(this.max, Math.max(this.min, parsedValue)));
      this.input.value = normalizedValue;
      if (shouldDispatch) {
        this.input.dispatchEvent(new Event("change", { bubbles: true }));
      }
      this.syncDisplay();
    }

    adjust(direction) {
      const current = this.currentValue();

      if (current == null) {
        if (direction > 0) {
          this.setValue(this.min);
        }
        return;
      }

      const nextValue = current + direction * this.step;
      if (direction < 0 && nextValue < this.min && this.allowEmpty) {
        this.setValue("");
        return;
      }

      this.setValue(Math.min(this.max, Math.max(this.min, nextValue)));
    }

    syncDisplay() {
      const current = this.currentValue();
      const isEmpty = current == null;
      if (this.valueField) {
        this.valueField.value = isEmpty ? "" : String(current);
        this.valueField.placeholder = this.placeholder;
        this.valueField.classList.toggle("text-outline", isEmpty);
        this.valueField.classList.toggle("text-on-surface", !isEmpty);
      }
      this.decrementButton.disabled = isEmpty;
      this.decrementButton.classList.toggle("opacity-40", isEmpty);
      if (this.clearButton) {
        this.clearButton.classList.toggle("hidden", isEmpty);
        this.clearButton.classList.toggle("inline-flex", !isEmpty);
      }
    }
  }

  const MIN_RATING_STEPS = [0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5];

  function normalizeminAvgRatingString(raw) {
    const trimmed = raw == null ? "" : String(raw).trim();
    if (!trimmed) {
      return "";
    }
    const n = Number.parseFloat(trimmed);
    if (!Number.isFinite(n)) {
      return "";
    }
    const match = MIN_RATING_STEPS.find((step) => Math.abs(step - n) < 1e-6);
    if (match == null) {
      return "";
    }
    if (Math.abs(match - Math.trunc(match)) < 1e-6) {
      return String(Math.trunc(match));
    }
    return String(match);
  }

  class minAvgRatingPicker {
    constructor(root) {
      this.root = root;
      this.hiddenInput = root.querySelector("[data-min-rating-value-input]");
      this.starsContainer = root.querySelector("[data-min-rating-stars]");
      this.clearBtn = root.querySelector("[data-min-rating-clear]");
      this.hoverValue = null;
      this.root.__minAvgRatingPicker = this;
      this.bind();
      this.render();
    }

    parseValue(raw) {
      const normalized = normalizeminAvgRatingString(raw);
      if (!normalized) {
        return null;
      }
      return Number.parseFloat(normalized);
    }

    bind() {
      this.root.querySelectorAll("[data-min-rating-step]").forEach((btn) => {
        btn.addEventListener("click", (event) => {
          event.preventDefault();
          const step = Number.parseFloat(
            btn.getAttribute("data-min-rating-step") || "",
          );
          if (!Number.isFinite(step)) {
            return;
          }
          const current = this.parseValue(this.hiddenInput.value);
          if (current != null && Math.abs(current - step) < 1e-6) {
            this.setValue("");
            return;
          }
          this.setValue(String(step));
        });

        btn.addEventListener("mouseenter", () => {
          const step = Number.parseFloat(
            btn.getAttribute("data-min-rating-step") || "",
          );
          if (!Number.isFinite(step)) {
            return;
          }
          this.hoverValue = step;
          this.render();
        });
      });

      if (this.starsContainer) {
        this.starsContainer.addEventListener("mouseleave", () => {
          this.hoverValue = null;
          this.render();
        });
      }

      if (this.clearBtn) {
        this.clearBtn.addEventListener("click", (event) => {
          event.preventDefault();
          this.setValue("");
        });
      }
    }

    effectiveValue() {
      if (this.hoverValue != null) {
        return this.hoverValue;
      }
      return this.parseValue(this.hiddenInput.value);
    }

    render() {
      const v = this.effectiveValue();
      const baseIcon =
        "material-symbols-outlined text-3xl leading-none transition-colors duration-150";
      for (let star = 1; star <= 5; star += 1) {
        const icon = this.root.querySelector(
          `[data-min-rating-icon="${star}"]`,
        );
        if (!icon) {
          continue;
        }
        let symbol = "star";
        let filled = false;
        if (v != null) {
          if (v >= star - 1e-6) {
            symbol = "star";
            filled = true;
          } else if (v + 1e-6 >= star - 0.5) {
            symbol = "star_half";
            filled = true;
          }
        }
        icon.textContent = symbol;
        if (filled) {
          icon.className = `${baseIcon} text-warning opacity-100`;
          icon.style.fontVariationSettings =
            '"FILL" 1, "wght" 400, "GRAD" 0, "opsz" 24';
        } else {
          icon.className = `${baseIcon} text-outline opacity-35`;
          icon.style.fontVariationSettings =
            '"FILL" 0, "wght" 400, "GRAD" 0, "opsz" 24';
        }
      }
    }

    setValue(raw) {
      const normalized = normalizeminAvgRatingString(raw);
      this.hiddenInput.value = normalized;
      this.hoverValue = null;
      this.hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
      this.root.dispatchEvent(
        new CustomEvent("paw:min-rating-change", { bubbles: true }),
      );
      this.render();
    }
  }

  class WeightSlider {
    constructor(root) {
      this.root = root;
      this.hiddenInput = root.querySelector("[data-weight-value-input]");
      this.input = root.querySelector("[data-weight-input]");
      this.valueNode = root.querySelector("[data-weight-value]");
      this.unit = root.dataset.unit || "kg";
      this.root.__weightSlider = this;
      this.bind();
      this.syncDisplay();
    }

    bind() {
      ["input", "change"].forEach((eventName) => {
        this.input.addEventListener(eventName, () => {
          this.hiddenInput.value = this.input.value;
          this.hiddenInput.dispatchEvent(
            new Event("change", { bubbles: true }),
          );
          this.syncDisplay();
        });
      });
    }

    setValue(value) {
      if (value) {
        this.hiddenInput.value = value;
        this.input.value = value;
      } else {
        this.hiddenInput.value = "";
        this.input.value = this.input.min || "100";
      }
      this.syncDisplay();
    }

    syncDisplay() {
      this.valueNode.textContent = this.input.value + " " + this.unit;
    }
  }

  function hydrateOptionsPickers(form, normalized) {
    if (!form || !normalized) {
      return;
    }
    form.querySelectorAll("[data-options-picker]").forEach((root) => {
      const hidden = root.querySelector("[data-option-value]");
      const picker = root.__optionsPicker;
      if (!hidden || !picker) {
        return;
      }
      const key = hidden.name;
      let next = "";
      if (
        Object.prototype.hasOwnProperty.call(normalized, key) &&
        normalized[key] != null &&
        String(normalized[key]).trim() !== ""
      ) {
        next = String(normalized[key]);
      } else if (hidden.value) {
        next = hidden.value;
      }
      picker.setSelectedValue(next);
    });
  }

  function hydrateForm(form, state) {
    const normalized = normalizeState(state);
    const searchInput = document.querySelector(
      '[data-marketplace-search-input][form="' + form.getAttribute("id") + '"]',
    );

    if (searchInput) {
      searchInput.value = normalized.searchQuery;
    }

    hydrateOptionsPickers(form, normalized);

    form.querySelectorAll("[data-people-count]").forEach((root) => {
      root.__peopleCount?.setValue(normalized.capacity || "");
    });

    form.querySelectorAll("[data-weight-slider]").forEach((root) => {
      root.__weightSlider?.setValue(normalized.maxWeight);
    });

    form.querySelectorAll("[data-min-rating-picker]").forEach((root) => {
      root.__minAvgRatingPicker?.setValue(normalized.minAvgRating);
    });

    const difficultySelect = form.querySelector('[name="difficultyLevel"]');
    if (difficultySelect) {
      difficultySelect.value = normalized.difficultyLevel || "";
    }

    setDateTimeState(form, normalized);

    const toolbarForm = document.querySelector("[data-marketplace-toolbar-form]");
    hydrateMarketplaceToolbar(toolbarForm, normalized);
  }

  function bindDraftPersistence(form) {
    const persistDraft = () => {
      if (form.getAttribute("data-filter-form") === "marketplace") {
        writeStoredState(DRAFT_FILTERS_KEY, readMarketplacePersistSnapshot());
        return;
      }
      writeStoredState(DRAFT_FILTERS_KEY, readFilterStateFromForm(form));
    };
    const searchInput = document.querySelector(
      '[data-marketplace-search-input][form="' + form.getAttribute("id") + '"]',
    );

    if (searchInput) {
      searchInput.addEventListener("change", persistDraft);
      searchInput.addEventListener("input", persistDraft);
    }

    form
      .querySelectorAll(
        '[data-option-value], [data-people-input], [data-weight-input], [data-min-rating-value-input], [name="difficultyLevel"]',
      )
      .forEach((input) => {
        input.addEventListener("change", persistDraft);
        input.addEventListener("input", persistDraft);
      });

    form.querySelectorAll("[data-min-rating-picker]").forEach((root) => {
      root.addEventListener("paw:min-rating-change", persistDraft);
    });

    bindDateTimeDraftPersistence(form, persistDraft);

    const toolbarForm = document.querySelector("[data-marketplace-toolbar-form]");
    if (toolbarForm) {
      toolbarForm.querySelectorAll('[name="sortBy"], [name="pageSize"]').forEach((el) => {
        el.addEventListener("change", persistDraft);
      });
    }
  }

  function bindMarketplaceReset(control) {
    if (!control) {
      return;
    }

    control.addEventListener("click", (event) => {
      event.preventDefault();
      clearStoredStates();
      window.location.assign(
        control.href || buildUrlWithFilters(window.location.href, {}),
      );
    });
  }

  function syncMarketplaceOptionsPickersFromDom(form) {
    const domState = readFilterStateFromForm(form);
    form.querySelectorAll("[data-options-picker]").forEach((root) => {
      const hidden = root.querySelector("[data-option-value]");
      if (!hidden) {
        return;
      }
      if (hidden.name === "location") {
        root.__optionsPicker?.setSelectedValue(domState.location);
      } else if (hidden.name === "itemType") {
        root.__optionsPicker?.setSelectedValue(domState.itemType);
      }
    });
  }

  function initializeLandingFilters() {
    const form = document.querySelector('[data-filter-form="landing"]');
    if (!form) {
      return;
    }

    const draftState = readStoredState(DRAFT_FILTERS_KEY);
    if (hasAnyFilter(draftState)) {
      hydrateForm(form, draftState);
    }
    syncMarketplaceOptionsPickersFromDom(form);

    bindDraftPersistence(form);
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      form.querySelectorAll("[data-options-picker]").forEach((root) => {
        root.__optionsPicker?.prepareForSubmit();
      });
      const nextState = readFilterStateFromForm(form);
      writeStoredState(DRAFT_FILTERS_KEY, nextState);
      writeStoredState(APPLIED_FILTERS_KEY, nextState);
      const action = form.getAttribute("action") || "/marketplace";
      window.location.assign(buildUrlWithFilters(action, nextState));
    });
  }

  function initializeMarketplaceFilters() {
    const toolbarForm = document.querySelector("[data-marketplace-toolbar-form]");
    bindMarketplaceToolbarForm(toolbarForm);

    const form = document.querySelector('[data-filter-form="marketplace"]');
    if (!form) {
      return;
    }
    const resetControls = document.querySelectorAll(
      "[data-clear-marketplace-filters]",
    );

    const snapshot = readMarketplacePersistSnapshot();
    const draftState = readStoredState(DRAFT_FILTERS_KEY);

    if (hasAnyFilter(snapshot)) {
      hydrateForm(form, snapshot);
      writeStoredState(DRAFT_FILTERS_KEY, snapshot);
      writeStoredState(APPLIED_FILTERS_KEY, snapshot);
      reflectAppliedState(snapshot);
    } else if (hasPersistableMarketplaceDraft(draftState)) {
      if (hasAnyFilter(draftState)) {
        hydrateForm(form, draftState);
      }
      hydrateMarketplaceToolbar(toolbarForm, draftState);
      writeStoredState(APPLIED_FILTERS_KEY, draftState);
      reflectAppliedState(draftState);
    } else {
      hydrateMarketplaceToolbar(toolbarForm, snapshot);
      if (hasPersistableMarketplaceDraft(snapshot)) {
        writeStoredState(DRAFT_FILTERS_KEY, snapshot);
        writeStoredState(APPLIED_FILTERS_KEY, snapshot);
        reflectAppliedState(snapshot);
      } else {
        writeStoredState(APPLIED_FILTERS_KEY, {});
        reflectAppliedState({});
      }
    }

    bindDraftPersistence(form);
    resetControls.forEach(bindMarketplaceReset);
    syncMarketplaceOptionsPickersFromDom(form);
    form.addEventListener("submit", (event) => {
      event.preventDefault();
      form.querySelectorAll("[data-options-picker]").forEach((root) => {
        root.__optionsPicker?.prepareForSubmit();
      });
      const nextState = readMarketplacePersistSnapshot();
      writeStoredState(DRAFT_FILTERS_KEY, nextState);
      writeStoredState(APPLIED_FILTERS_KEY, nextState);
      reflectAppliedState(nextState);
      const action = form.getAttribute("action") || "/marketplace";
      const merged = new URL(buildUrlWithFilters(action, nextState), window.location.origin);
      merged.searchParams.set("page", "1");
      window.location.assign(merged.toString());
    });
  }

  function initializeItemAvailabilityAlert() {
    const alertRoot = document.querySelector("[data-item-unavailable-alert]");
    if (!alertRoot) {
      return;
    }

    const appliedState = normalizeState({
      ...readStoredState(APPLIED_FILTERS_KEY),
      ...readUrlState(),
    });
    const messageNode = alertRoot.querySelector(
      "[data-item-unavailable-message]",
    );
    const clearButton = alertRoot.querySelector(
      "[data-item-unavailable-clear]",
    );
    const marketplaceButton = alertRoot.querySelector(
      "[data-item-unavailable-marketplace]",
    );
    const itemLocationSlug = alertRoot.dataset.itemLocationSlug || "";
    const itemCapacity = parseInteger(alertRoot.dataset.itemCapacity);
    const itemMaxWeight = parseInteger(alertRoot.dataset.itemMaxWeight);
    const itemDifficulty = parseInteger(alertRoot.dataset.itemDifficultyLevel);
    const mismatchPrefix =
      alertRoot.dataset.mismatchPrefix ||
      "This item does not match the saved filters for";
    const mismatchSuffix =
      alertRoot.dataset.mismatchSuffix ||
      "You can keep viewing it without those filters or return to marketplace to choose another.";
    const mismatchJoin = alertRoot.dataset.mismatchJoin || "and";
    const mismatchLocation = alertRoot.dataset.mismatchLocation || "location";
    const mismatchCapacity =
      alertRoot.dataset.mismatchCapacity || "people capacity";
    const mismatchWeight =
      alertRoot.dataset.mismatchWeight || "required weight";
    const mismatchDateTime =
      alertRoot.dataset.mismatchDateTime || "date and time";
    const mismatchDifficulty =
      alertRoot.dataset.mismatchDifficulty || "difficulty level";
    const controls = getDateTimeControls(document);
    const mismatchReasons = [];
    const requestedCapacity = parseInteger(appliedState.capacity);
    const requestedWeight = parseInteger(appliedState.maxWeight);
    const requestedDifficulty = parseInteger(appliedState.difficultyLevel);

    if (
      appliedState.location &&
      String(appliedState.location) !== String(itemLocationSlug)
    ) {
      mismatchReasons.push(mismatchLocation);
    }

    if (
      requestedCapacity != null &&
      itemCapacity != null &&
      itemCapacity < requestedCapacity
    ) {
      mismatchReasons.push(mismatchCapacity);
    }

    if (
      requestedWeight != null &&
      itemMaxWeight != null &&
      itemMaxWeight < requestedWeight
    ) {
      mismatchReasons.push(mismatchWeight);
    }

    if (
      requestedDifficulty != null &&
      (itemDifficulty == null || itemDifficulty !== requestedDifficulty)
    ) {
      mismatchReasons.push(mismatchDifficulty);
    }

    if (
      controls &&
      appliedState.date &&
      appliedState.startTime &&
      appliedState.endTime &&
      (!controls.datePicker?.isSelectableDate(appliedState.date) ||
        !controls.timePicker?.isContinuousRangeForDate(
          appliedState.date,
          appliedState.startTime,
          appliedState.endTime,
        ))
    ) {
      mismatchReasons.push(mismatchDateTime);
    }

    clearButton.addEventListener("click", () => {
      clearStoredStates();
      window.location.assign(buildUrlWithFilters(window.location.href, {}));
    });

    marketplaceButton.addEventListener("click", () => {
      const referrer = document.referrer;

      if (
        referrer &&
        referrer.indexOf(window.location.origin) === 0 &&
        referrer.indexOf("/marketplace") >= 0
      ) {
        window.location.assign(referrer);
        return;
      }

      window.location.assign(
        buildUrlWithFilters(alertRoot.dataset.marketplaceUrl, appliedState),
      );
    });

    if (mismatchReasons.length > 0) {
      if (messageNode) {
        messageNode.textContent =
          mismatchPrefix +
          " " +
          formatMismatchReasons(mismatchReasons, mismatchJoin) +
          ". " +
          mismatchSuffix;
      }
      openUnavailableAlert(alertRoot);
      return;
    }

    closeUnavailableAlert(alertRoot);

    if (controls && appliedState.date) {
      setDateTimeState(document, appliedState);
    }

    if (controls) {
      const persistItemState = () => {
        const nextState = normalizeState({
          ...readStoredState(APPLIED_FILTERS_KEY),
          ...readDateTimeState(document),
        });
        writeStoredState(DRAFT_FILTERS_KEY, nextState);
        writeStoredState(APPLIED_FILTERS_KEY, nextState);
      };

      bindDateTimeDraftPersistence(document, persistItemState);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-options-picker]").forEach((root) => {
      new OptionsPicker(root);
    });

    document.querySelectorAll("[data-people-count]").forEach((root) => {
      new PeopleCount(root);
    });

    document.querySelectorAll("[data-weight-slider]").forEach((root) => {
      new WeightSlider(root);
    });

    document.querySelectorAll("[data-min-rating-picker]").forEach((root) => {
      new minAvgRatingPicker(root);
    });

    initializeLandingFilters();
    initializeMarketplaceFilters();
    initializeItemAvailabilityAlert();
  });
})();
