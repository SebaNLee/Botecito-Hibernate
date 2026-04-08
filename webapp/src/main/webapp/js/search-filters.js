(function () {
  const FILTER_KEYS = [
    "location",
    "date",
    "startTime",
    "endTime",
    "capacity",
    "maxWeight",
  ];
  const APPLIED_FILTERS_KEY = "paw.marketplaceFilters";
  const DRAFT_FILTERS_KEY = "paw.marketplaceFilterDraft";
  let locationOptionsPromise = null;

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

  function normalizeState(state) {
    const normalized = {};

    FILTER_KEYS.forEach((key) => {
      normalized[key] = state && state[key] ? String(state[key]).trim() : "";
    });

    return normalized;
  }

  function hasAnyFilter(state) {
    return FILTER_KEYS.some((key) => normalizeText(state[key]));
  }

  function readStoredState(storageKey) {
    try {
      return normalizeState(
        parseJson(sessionStorage.getItem(storageKey), {}) || {},
      );
    } catch (error) {
      return normalizeState({});
    }
  }

  function writeStoredState(storageKey, state) {
    const normalized = normalizeState(state);

    try {
      if (hasAnyFilter(normalized)) {
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

    return url.toString();
  }

  function readUrlState() {
    const params = new URLSearchParams(window.location.search);
    return normalizeState(
      FILTER_KEYS.reduce((state, key) => {
        state[key] = params.get(key) || "";
        return state;
      }, {}),
    );
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

  function readFilterStateFromForm(form) {
    if (!form) {
      return normalizeState({});
    }

    return normalizeState({
      location:
        form.querySelector("[data-location-value]")?.value ||
        form.querySelector('[name="location"]')?.value ||
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
    });
  }

  function reflectAppliedState(state) {
    const normalized = normalizeState(state);

    document.querySelectorAll("[data-applied-filter-mirror]").forEach((input) => {
      input.value = normalized[input.name] || "";
    });

    document.querySelectorAll("[data-marketplace-item-link]").forEach((link) => {
      if (!link.dataset.baseHref) {
        link.dataset.baseHref = link.getAttribute("href") || "";
      }

      link.setAttribute("href", buildUrlWithFilters(link.dataset.baseHref, normalized));
    });
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

  function fetchLocationOptions(url) {
    if (!locationOptionsPromise) {
      locationOptionsPromise = fetch(url)
        .then((response) => (response.ok ? response.json() : []))
        .catch(() => [])
        .then((options) =>
          Array.isArray(options)
            ? options.filter((option) => typeof option === "string")
            : [],
        );
    }

    return locationOptionsPromise;
  }

  class LocationPicker {
    constructor(root) {
      this.root = root;
      this.hiddenInput = root.querySelector("[data-location-value]");
      this.queryInput = root.querySelector("[data-location-query]");
      this.trigger = root.querySelector("[data-location-trigger]");
      this.clearButton = root.querySelector("[data-location-clear]");
      this.chevron = root.querySelector("[data-location-chevron]");
      this.panel = root.querySelector("[data-location-panel]");
      this.optionsContainer = root.querySelector("[data-location-options]");
      this.emptyState = root.querySelector("[data-location-empty]");
      this.options = [];
      this.root.__locationPicker = this;
      this.bind();
      this.loadOptions();
    }

    bind() {
      if (this.trigger) {
        this.trigger.addEventListener("click", (event) => {
          if (event.target.closest("[data-location-clear]")) {
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
        this.hiddenInput.value = exactMatch || "";
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
        if (!event.target.closest("[data-location-picker]")) {
          this.close();
        }
      });
    }

    loadOptions() {
      fetchLocationOptions(this.root.dataset.optionsUrl || "/location-options").then(
        (options) => {
          this.options = options;
          this.render();
        },
      );
    }

    getFilteredOptions() {
      const query = normalizeText(this.queryInput.value);
      if (!query) {
        return this.options;
      }

      return this.options.filter((option) =>
        normalizeText(option).includes(query),
      );
    }

    findExact(value) {
      const normalized = normalizeText(value);
      const match = this.options.find(
        (option) => normalizeText(option) === normalized,
      );
      return match || "";
    }

    setSelectedValue(value) {
      this.hiddenInput.value = value || "";
      this.queryInput.value = value || "";
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
      const exactMatch = this.findExact(this.queryInput.value);
      this.hiddenInput.value = exactMatch || "";
      this.queryInput.value = exactMatch || this.queryInput.value || "";
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
      if (normalizeText(option) === normalizeText(this.hiddenInput.value)) {
        this.clear();
        return;
      }

      this.hiddenInput.value = option;
      this.queryInput.value = option;
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
          normalizeText(option) === normalizeText(this.hiddenInput.value);
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
        title.textContent = option;
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
          this.hiddenInput.dispatchEvent(new Event("change", { bubbles: true }));
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

  function hydrateForm(form, state) {
    const normalized = normalizeState(state);

    form.querySelectorAll("[data-location-picker]").forEach((root) => {
      root.__locationPicker?.setSelectedValue(normalized.location);
    });

    form.querySelectorAll("[data-people-count]").forEach((root) => {
      root.__peopleCount?.setValue(normalized.capacity || "");
    });

    form.querySelectorAll("[data-weight-slider]").forEach((root) => {
      root.__weightSlider?.setValue(normalized.maxWeight);
    });

    setDateTimeState(form, normalized);
  }

  function prepareFilterForm(form) {
    form.querySelectorAll("[data-location-picker]").forEach((root) => {
      root.__locationPicker?.prepareForSubmit();
    });
  }

  function bindDraftPersistence(form) {
    const persistDraft = () => {
      writeStoredState(DRAFT_FILTERS_KEY, readFilterStateFromForm(form));
    };

    form
      .querySelectorAll("[data-location-value], [data-people-input], [data-weight-input]")
      .forEach((input) => {
        input.addEventListener("change", persistDraft);
        input.addEventListener("input", persistDraft);
      });

    bindDateTimeDraftPersistence(form, persistDraft);
  }

  function bindMarketplaceReset(control) {
    if (!control) {
      return;
    }

    control.addEventListener("click", (event) => {
      event.preventDefault();
      clearStoredStates();
      window.location.assign(control.href || buildUrlWithFilters(window.location.href, {}));
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

    bindDraftPersistence(form);
    form.addEventListener("submit", () => {
      prepareFilterForm(form);
      const nextState = readFilterStateFromForm(form);
      writeStoredState(DRAFT_FILTERS_KEY, nextState);
      writeStoredState(APPLIED_FILTERS_KEY, nextState);
    });
  }

  function initializeMarketplaceFilters() {
    const form = document.querySelector('[data-filter-form="marketplace"]');
    if (!form) {
      return;
    }
    const resetControl = document.querySelector("[data-clear-marketplace-filters]");

    const pageState = readFilterStateFromForm(form);
    const draftState = readStoredState(DRAFT_FILTERS_KEY);

    if (hasAnyFilter(pageState)) {
      hydrateForm(form, pageState);
      writeStoredState(DRAFT_FILTERS_KEY, pageState);
      writeStoredState(APPLIED_FILTERS_KEY, pageState);
      reflectAppliedState(pageState);
    } else {
      if (hasAnyFilter(draftState)) {
        hydrateForm(form, draftState);
      }
      writeStoredState(APPLIED_FILTERS_KEY, {});
      reflectAppliedState({});
    }

    bindDraftPersistence(form);
    bindMarketplaceReset(resetControl);
    form.addEventListener("submit", () => {
      prepareFilterForm(form);
      const nextState = readFilterStateFromForm(form);
      writeStoredState(DRAFT_FILTERS_KEY, nextState);
      writeStoredState(APPLIED_FILTERS_KEY, nextState);
      reflectAppliedState(nextState);
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
    const messageNode = alertRoot.querySelector("[data-item-unavailable-message]");
    const clearButton = alertRoot.querySelector("[data-item-unavailable-clear]");
    const marketplaceButton = alertRoot.querySelector(
      "[data-item-unavailable-marketplace]",
    );
    const itemLocation = alertRoot.dataset.itemLocation || "";
    const itemCapacity = parseInteger(alertRoot.dataset.itemCapacity);
    const itemMaxWeight = parseInteger(alertRoot.dataset.itemMaxWeight);
    const mismatchPrefix =
      alertRoot.dataset.mismatchPrefix ||
      "This item does not match the saved filters for";
    const mismatchSuffix =
      alertRoot.dataset.mismatchSuffix ||
      "You can keep viewing it without those filters or return to marketplace to choose another.";
    const mismatchJoin = alertRoot.dataset.mismatchJoin || "and";
    const mismatchLocation =
      alertRoot.dataset.mismatchLocation || "location";
    const mismatchCapacity =
      alertRoot.dataset.mismatchCapacity || "people capacity";
    const mismatchWeight =
      alertRoot.dataset.mismatchWeight || "required weight";
    const mismatchDateTime =
      alertRoot.dataset.mismatchDateTime || "date and time";
    const controls = getDateTimeControls(document);
    const mismatchReasons = [];
    const requestedCapacity = parseInteger(appliedState.capacity);
    const requestedWeight = parseInteger(appliedState.maxWeight);

    if (
      appliedState.location &&
      normalizeText(appliedState.location) !== normalizeText(itemLocation)
    ) {
      mismatchReasons.push(mismatchLocation);
    }

    if (requestedCapacity != null && itemCapacity != null && itemCapacity < requestedCapacity) {
      mismatchReasons.push(mismatchCapacity);
    }

    if (requestedWeight != null && itemMaxWeight != null && itemMaxWeight < requestedWeight) {
      mismatchReasons.push(mismatchWeight);
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
      alertRoot.hidden = false;
      alertRoot.classList.remove("hidden");
      alertRoot.classList.add("flex");
      return;
    }

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
    document.querySelectorAll("[data-location-picker]").forEach((root) => {
      new LocationPicker(root);
    });

    document.querySelectorAll("[data-people-count]").forEach((root) => {
      new PeopleCount(root);
    });

    document.querySelectorAll("[data-weight-slider]").forEach((root) => {
      new WeightSlider(root);
    });

    initializeLandingFilters();
    initializeMarketplaceFilters();
    initializeItemAvailabilityAlert();
  });
})();
