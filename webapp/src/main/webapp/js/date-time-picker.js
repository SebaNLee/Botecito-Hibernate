(function () {
  const UI_LOCALE = document.documentElement.lang || navigator.language || "es";
  const WEEKDAY_LABELS = buildWeekdayLabels(UI_LOCALE);
  const MONTH_FORMATTER = new Intl.DateTimeFormat(UI_LOCALE, {
    month: "long",
    year: "numeric",
  });
  const DATE_FORMATTER = new Intl.DateTimeFormat(UI_LOCALE, {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  const DATE_TRIGGER_SHORT_WEEKDAY = new Intl.DateTimeFormat(UI_LOCALE, {
    weekday: "short",
    day: "numeric",
    month: "long",
  });
  const FILTER_STORAGE_KEY = "paw.marketplaceFilters";
  const ALL_TIMES = buildAllTimes();
  const MONTH_SECTION_CLASS = "picker-month";
  const MONTH_TITLE_CLASS =
    "mb-3 text-xs font-extrabold uppercase tracking-[0.12em] text-on-surface-variant";
  const WEEKDAY_ROW_CLASS = "mb-2 grid grid-cols-7 gap-1";
  const WEEKDAY_CLASS =
    "flex justify-center text-[11px] font-extrabold uppercase tracking-[0.12em] text-outline";
  const CALENDAR_GRID_CLASS = "grid grid-cols-7 gap-1";
  const CALENDAR_SPACER_CLASS = "min-h-10";
  const DAY_BASE_CLASS = "btn btn-sm btn-square min-h-10 h-10 font-semibold";
  const DAY_AVAILABLE_CLASS = "btn-ghost text-on-surface";
  const DAY_UNAVAILABLE_CLASS = "btn-ghost text-outline/40 pointer-events-none";
  const DAY_OCCUPIED_CLASS =
    "btn-ghost text-error/70 line-through pointer-events-none";
  const DAY_SELECTED_CLASS = "btn-primary shadow-sm";
  const DAY_TODAY_CLASS = "ring-2 ring-primary/35 ring-inset";
  const SLOT_BASE_CLASS =
    "btn btn-sm h-10 min-h-10 min-w-[4.5rem] shrink-0 px-1.5 text-xs font-semibold tabular-nums whitespace-nowrap";
  const SLOT_AVAILABLE_CLASS = "btn-ghost text-on-surface";
  const SLOT_UNAVAILABLE_CLASS =
    "btn-ghost text-outline/40 pointer-events-none";
  const SLOT_OCCUPIED_CLASS =
    "btn-ghost text-error/70 line-through pointer-events-none";
  const SLOT_SELECTED_CLASS = "btn-primary shadow-sm";
  const SLOT_RANGE_CLASS = "btn-primary btn-soft";

  function buildWeekdayLabels(locale) {
    const formatter = new Intl.DateTimeFormat(locale, { weekday: "short" });
    return Array.from({ length: 7 }, (_, index) => {
      const value = formatter.format(new Date(2026, 0, 4 + index));
      return value.replace(/\.$/, "").slice(0, 2).toUpperCase();
    });
  }

  function buildAllTimes() {
    const times = [];

    for (let hour = 0; hour < 24; hour += 1) {
      for (let minute = 0; minute < 60; minute += 30) {
        times.push(formatTime(hour, minute));
      }
    }

    return times;
  }

  function formatTime(hour, minute) {
    return (
      String(hour).padStart(2, "0") + ":" + String(minute).padStart(2, "0")
    );
  }

  function displayTime(time) {
    return time ? time + " hs" : "";
  }

  function parseJson(value, fallback) {
    try {
      return value ? JSON.parse(value) : fallback;
    } catch (error) {
      return fallback;
    }
  }

  function parseBoolean(value, fallback) {
    if (value === "true") {
      return true;
    }

    if (value === "false") {
      return false;
    }

    return fallback;
  }

  const TRIGGER_CLEAR_COLLAPSED_CLASSES = [
    "!w-0",
    "!min-w-0",
    "!max-w-0",
    "overflow-hidden",
    "!p-0",
    "opacity-0",
    "pointer-events-none",
    "!border-0",
  ];

  function syncTriggerClearButtonCollapsed(button, collapsed) {
    if (!button) {
      return;
    }

    TRIGGER_CLEAR_COLLAPSED_CLASSES.forEach((cls) => {
      button.classList.toggle(cls, collapsed);
    });

    if (collapsed) {
      button.setAttribute("aria-hidden", "true");
      button.setAttribute("tabindex", "-1");
    } else {
      button.removeAttribute("aria-hidden");
      button.removeAttribute("tabindex");
    }
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

  function isoToDate(isoDate) {
    return new Date(isoDate + "T00:00:00");
  }

  function formatLongDate(isoDate) {
    if (!isoDate) {
      return "";
    }

    const formatted = DATE_FORMATTER.format(isoToDate(isoDate));
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  function formatDatePickerTriggerLabel(isoDate) {
    if (!isoDate) {
      return "";
    }

    const date = isoToDate(isoDate);
    let formatted = DATE_FORMATTER.format(date);
    if (formatted.length > 32) {
      formatted = DATE_TRIGGER_SHORT_WEEKDAY.format(date);
    }

    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  function formatMonthLabel(date) {
    const formatted = MONTH_FORMATTER.format(date);
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  function createElement(tagName, className, textContent) {
    const element = document.createElement(tagName);

    if (className) {
      element.className = className;
    }

    if (textContent !== undefined) {
      element.textContent = textContent;
    }

    return element;
  }

  function getVisibleTopBoundary(selector, fallback) {
    const element = document.querySelector(selector);

    if (!element) {
      return fallback;
    }

    const style = window.getComputedStyle(element);
    const rect = element.getBoundingClientRect();

    if (
      style.display === "none" ||
      style.visibility === "hidden" ||
      rect.height <= 0 ||
      rect.top >= window.innerHeight
    ) {
      return fallback;
    }

    return rect.top;
  }

  function showPanel(panel, trigger) {
    panel.hidden = false;
    panel.classList.remove("hidden");
    panel.classList.add("flex");
    trigger.setAttribute("aria-expanded", "true");
    panel.style.zIndex = "9999";
    const root = trigger.closest(
      "[data-date-picker], [data-time-range-picker]",
    );
    if (root) {
      root.style.zIndex = "9998";
    }
    positionFloatingPanel(panel, trigger);
  }

  function hidePanel(panel, trigger) {
    panel.hidden = true;
    panel.classList.add("hidden");
    panel.classList.remove("flex");
    trigger.setAttribute("aria-expanded", "false");
    const root = trigger.closest(
      "[data-date-picker], [data-time-range-picker]",
    );
    if (root) {
      root.style.zIndex = "";
    }
  }

  function mountFloatingPanel(panel) {
    if (!panel || panel.dataset.floatingMounted === "true") {
      return;
    }

    document.body.appendChild(panel);
    panel.dataset.floatingMounted = "true";
  }

  function dayStateClass(state, selected) {
    if (selected) {
      return DAY_SELECTED_CLASS;
    }

    if (state === "available") {
      return DAY_AVAILABLE_CLASS;
    }

    if (state === "occupied") {
      return DAY_OCCUPIED_CLASS;
    }

    return DAY_UNAVAILABLE_CLASS;
  }

  function slotStateClass(state) {
    if (state === "selected") {
      return SLOT_SELECTED_CLASS;
    }

    if (state === "range") {
      return SLOT_RANGE_CLASS;
    }

    if (state === "available") {
      return SLOT_AVAILABLE_CLASS;
    }

    if (state === "occupied") {
      return SLOT_OCCUPIED_CLASS;
    }

    return SLOT_UNAVAILABLE_CLASS;
  }

  function positionFloatingPanel(panel, trigger) {
    const margin = 16;
    const header = document.querySelector("header");
    const headerBottom = header
      ? Math.max(0, header.getBoundingClientRect().bottom)
      : 0;
    const safeTop = Math.max(margin, headerBottom + 8);
    const footerTop = getVisibleTopBoundary("footer", window.innerHeight);
    const mobileNavTop = getVisibleTopBoundary(
      "body > nav",
      window.innerHeight,
    );
    const safeBottom = Math.max(
      safeTop + 120,
      Math.min(window.innerHeight - margin, footerTop - 8, mobileNavTop - 8),
    );
    const panelWidth = Math.min(
      parseInt(panel.dataset.panelWidth || "352", 10),
      window.innerWidth - margin * 2,
    );
    panel.style.width = panelWidth + "px";
    panel.style.maxHeight = "";
    panel.style.visibility = "hidden";

    const triggerRect = trigger.getBoundingClientRect();
    const naturalPanelHeight = panel.offsetHeight;
    const maxViewportHeight = Math.max(120, safeBottom - safeTop);
    const panelHeight = Math.min(naturalPanelHeight, maxViewportHeight);
    const gap = 12;
    const spaceBelow = safeBottom - triggerRect.bottom - gap;
    const spaceAbove = triggerRect.top - safeTop - gap;
    const openUpwards = spaceAbove >= spaceBelow;
    const top = openUpwards
      ? Math.max(safeTop, triggerRect.top - panelHeight - gap)
      : Math.min(
          safeBottom - panelHeight,
          Math.max(safeTop, triggerRect.bottom + gap),
        );
    const left = Math.min(
      Math.max(margin, triggerRect.left),
      Math.max(margin, window.innerWidth - panelWidth - margin),
    );
    const availableHeight = openUpwards ? spaceAbove : spaceBelow;

    panel.style.top = top + "px";
    panel.style.left = left + "px";
    if (naturalPanelHeight > Math.max(120, availableHeight)) {
      panel.style.maxHeight = Math.max(120, availableHeight) + "px";
    } else {
      panel.style.maxHeight = "";
    }
    panel.style.visibility = "";
  }

  function isoDateParts(isoDate) {
    return isoDate.split("-").map(Number);
  }

  function toIsoDate(year, monthIndex, day) {
    return [
      String(year).padStart(4, "0"),
      String(monthIndex + 1).padStart(2, "0"),
      String(day).padStart(2, "0"),
    ].join("-");
  }

  function dateObjectToIsoDate(date) {
    return toIsoDate(
      date.getUTCFullYear(),
      date.getUTCMonth(),
      date.getUTCDate(),
    );
  }

  function normalizePickerDateValue(value) {
    if (!value) {
      return "";
    }

    if (value instanceof Date) {
      return dateObjectToIsoDate(value);
    }

    return String(value);
  }

  function todayIsoDate() {
    const today = new Date();
    return toIsoDate(today.getFullYear(), today.getMonth(), today.getDate());
  }

  function visibleRangeEndIsoDate() {
    const today = new Date();
    const maxDate = new Date(
      today.getFullYear(),
      today.getMonth() + 2,
      today.getDate(),
    );
    return dateObjectToIsoDate(maxDate);
  }

  function monthKey(year, monthIndex) {
    return String(year) + "-" + String(monthIndex).padStart(2, "0");
  }

  function compareTimes(left, right) {
    return ALL_TIMES.indexOf(left) - ALL_TIMES.indexOf(right);
  }

  function clampMonthRange() {
    const today = new Date();
    const startMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const endMonth = new Date(today.getFullYear(), today.getMonth() + 2, 1);
    const monthKeys = [];
    const cursor = new Date(startMonth);

    while (cursor <= endMonth) {
      monthKeys.push(monthKey(cursor.getFullYear(), cursor.getMonth()));
      cursor.setMonth(cursor.getMonth() + 1);
    }

    return monthKeys;
  }

  function minutesBetween(startTime, endTime) {
    return (ALL_TIMES.indexOf(endTime) - ALL_TIMES.indexOf(startTime)) * 30;
  }

  function isBlank(value) {
    return !value || !String(value).trim();
  }

  function hasAnyFilter(state) {
    return Boolean(state && (state.date || state.startTime || state.endTime));
  }

  function hasCompleteRange(state) {
    return Boolean(state && state.date && state.startTime && state.endTime);
  }

  function readStoredFilters() {
    try {
      return parseJson(sessionStorage.getItem(FILTER_STORAGE_KEY), {}) || {};
    } catch (error) {
      return {};
    }
  }

  function writeStoredFilters(state) {
    try {
      sessionStorage.setItem(
        FILTER_STORAGE_KEY,
        JSON.stringify({
          date: state.date || "",
          startTime: state.startTime || "",
          endTime: state.endTime || "",
        }),
      );
    } catch (error) {
      // Ignore session storage failures.
    }
  }

  function clearStoredFilters() {
    try {
      sessionStorage.removeItem(FILTER_STORAGE_KEY);
    } catch (error) {
      // Ignore session storage failures.
    }
  }

  function buildUrlWithFilters(baseUrl, state) {
    const url = new URL(baseUrl, window.location.origin);

    ["date", "startTime", "endTime"].forEach((key) => {
      if (state && state[key]) {
        url.searchParams.set(key, state[key]);
      } else {
        url.searchParams.delete(key);
      }
    });

    return url.toString();
  }

  function stripAvailabilityFilters(baseUrl) {
    return buildUrlWithFilters(baseUrl, {});
  }

  function currentPickerState(controls) {
    return {
      date: controls.dateInput.value || "",
      startTime: controls.startInput.value || "",
      endTime: controls.endInput.value || "",
    };
  }

  function syncHiddenFilterMirrors(state) {
    ["date", "startTime", "endTime"].forEach((name) => {
      const selector =
        'input[type="hidden"][name="' +
        name +
        '"]:not([data-picker-input]):not([data-time-start-input]):not([data-time-end-input])';
      document.querySelectorAll(selector).forEach((input) => {
        input.value = state[name] || "";
      });
    });
  }

  function updateMarketplaceItemLinks(state) {
    document
      .querySelectorAll("[data-marketplace-item-link]")
      .forEach((link) => {
        if (!link.dataset.baseHref) {
          link.dataset.baseHref = link.getAttribute("href") || "";
        }

        link.setAttribute(
          "href",
          buildUrlWithFilters(link.dataset.baseHref, state),
        );
      });
  }

  function syncPersistentFilters(state) {
    if (hasAnyFilter(state)) {
      writeStoredFilters(state);
    } else {
      clearStoredFilters();
    }

    syncHiddenFilterMirrors(state);
    updateMarketplaceItemLinks(state);
  }

  function getPickerControls() {
    const dateRoot = document.querySelector("[data-date-picker]");
    const timeRoot = document.querySelector("[data-time-range-picker]");

    if (!dateRoot || !timeRoot) {
      return null;
    }

    return {
      dateRoot,
      timeRoot,
      datePicker: dateRoot.__datePicker || null,
      timePicker: timeRoot.__timeRangePicker || null,
      dateInput: dateRoot.querySelector("[data-picker-input]"),
      startInput: timeRoot.querySelector("[data-time-start-input]"),
      endInput: timeRoot.querySelector("[data-time-end-input]"),
    };
  }

  function initializeReservationPriceSummaries() {
    document
      .querySelectorAll("[data-reservation-price-summary]")
      .forEach((summaryRoot) => {
        const summarySection = summaryRoot.parentElement;
        const form =
          summaryRoot.closest("form") ||
          (summarySection ? summarySection.querySelector("form") : null);

        if (!form) {
          return;
        }

        const dateInput = form.querySelector("[data-picker-input]");
        const startInput = form.querySelector("[data-time-start-input]");
        const endInput = form.querySelector("[data-time-end-input]");
        const totalNode = summaryRoot.querySelector("[data-price-total]");
        const durationNode = summaryRoot.querySelector("[data-price-duration]");
        const pricePerHour = Number.parseFloat(
          String(summaryRoot.dataset.pricePerHour || "").replace(",", "."),
        );

        if (
          !dateInput ||
          !startInput ||
          !endInput ||
          !totalNode ||
          !durationNode ||
          Number.isNaN(pricePerHour)
        ) {
          return;
        }

        const currencySymbol = summaryRoot.dataset.currencySymbol || "$";
        const pendingLabel = summaryRoot.dataset.pricePending || "";
        const pendingHelpLabel = summaryRoot.dataset.pricePendingHelp || "";
        const pickEndLabel = summaryRoot.dataset.pricePickEnd || "";
        const priceForLabel = summaryRoot.dataset.priceForLabel || "";
        const hourLabel = summaryRoot.dataset.priceHourLabel || "";
        const hoursLabel = summaryRoot.dataset.priceHoursLabel || "";
        const numberFormatter = new Intl.NumberFormat("es-AR", {
          minimumFractionDigits: 0,
          maximumFractionDigits: 2,
        });

        function resetSummary() {
          summaryRoot.classList.add("hidden");
          totalNode.textContent = pendingLabel;
          durationNode.textContent = pendingHelpLabel;
        }

        function updateSummary() {
          if (!dateInput.value || !startInput.value) {
            resetSummary();
            return;
          }

          if (!endInput.value) {
            summaryRoot.classList.add("hidden");
            totalNode.textContent = pendingLabel;
            durationNode.textContent = pickEndLabel;
            return;
          }

          const selectedMinutes = minutesBetween(
            startInput.value,
            endInput.value,
          );

          if (selectedMinutes <= 0) {
            resetSummary();
            return;
          }

          const selectedHours = selectedMinutes / 60;
          const totalPrice = pricePerHour * selectedHours;
          const hourUnit = selectedHours === 1 ? hourLabel : hoursLabel;

          summaryRoot.classList.remove("hidden");
          totalNode.textContent =
            currencySymbol + numberFormatter.format(totalPrice);
          durationNode.textContent =
            priceForLabel +
            " " +
            numberFormatter.format(selectedHours) +
            " " +
            hourUnit;
        }

        [dateInput, startInput, endInput].forEach((input) => {
          input.addEventListener("change", updateSummary);
        });

        updateSummary();
      });
  }

  class DatePicker {
    constructor(root) {
      this.root = root;
      this.input = root.querySelector("[data-picker-input]");
      this.trigger = root.querySelector("[data-picker-trigger]");
      this.controlRow = root.querySelector("[data-picker-control-row]");
      this.panel = root.querySelector("[data-picker-panel]");
      this.closeButton = root.querySelector("[data-picker-close]");
      this.clearButton = root.querySelector("[data-picker-clear]");
      this.triggerClearButton = root.querySelector(
        "[data-picker-trigger-clear]",
      );
      this.chevronNode = root.querySelector("[data-picker-chevron]");
      this.valueNode = root.querySelector("[data-picker-value]");
      this.calendar = root.querySelector("[data-picker-calendar]");
      this.placeholder = root.dataset.placeholder || "";
      this.availabilityLabel = root.dataset.availabilityLabel || "";
      this.offeredDates = new Set(parseJson(root.dataset.offeredDates, []));
      this.occupiedDates = new Set(parseJson(root.dataset.occupiedDates, []));
      this.restrictToAvailability = parseBoolean(
        root.dataset.restrictToAvailability,
        true,
      );
      this.today = todayIsoDate();
      this.maxDate = visibleRangeEndIsoDate();
      this.root.__datePicker = this;
      mountFloatingPanel(this.panel);

      this.bind();
      this.syncCalendar();
      this.updateValueLabel();
    }

    bind() {
      const toggleFromControlRow = () => {
        if (this.panel.hidden) {
          this.open();
        } else {
          this.close();
        }
      };

      const row = this.controlRow || this.trigger;
      row.addEventListener("click", () => {
        toggleFromControlRow();
      });

      if (this.closeButton) {
        this.closeButton.addEventListener("click", () => {
          this.close();
        });
      }

      [this.clearButton, this.triggerClearButton]
        .filter(Boolean)
        .forEach((button) => {
          button.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
            this.clear();
            this.close();
          });
        });

      if (this.calendar) {
        this.calendar.addEventListener("change", () => {
          this.selectDate(normalizePickerDateValue(this.calendar.value), true);
        });
        this.calendar.addEventListener("keydown", (event) => {
          if (event.key === "Escape") {
            event.preventDefault();
            this.close();
          }
        });
      }

      if (this.chevronNode) {
        const toggleFromChevron = (event) => {
          event.preventDefault();
          event.stopPropagation();
          if (this.panel.hidden) {
            this.open();
          } else {
            this.close();
          }
        };

        this.chevronNode.addEventListener("click", toggleFromChevron);
        this.chevronNode.addEventListener("keydown", (event) => {
          if (event.key === "Enter" || event.key === " ") {
            toggleFromChevron(event);
          }
        });
      }
    }

    syncCalendar() {
      if (!this.calendar) {
        return;
      }

      this.calendar.value = this.input.value || "";
      this.calendar.today = this.today;
      this.calendar.min = this.today;
      this.calendar.max = this.maxDate;
      this.calendar.locale =
        document.documentElement.lang || navigator.language || "es";
      this.calendar.isDateDisallowed = (date) =>
        !this.isSelectableDate(dateObjectToIsoDate(date));
      this.calendar.getDayParts = (date) => {
        const state = this.getDayState(dateObjectToIsoDate(date));

        if (state === "available") {
          return "available";
        }

        if (state === "occupied") {
          return "occupied";
        }

        return "";
      };
    }

    isSelectableDate(isoDate) {
      return this.getDayState(isoDate) === "available";
    }

    clear() {
      this.input.value = "";
      this.input.dispatchEvent(new Event("change", { bubbles: true }));
      this.syncCalendar();
      this.updateValueLabel();
    }

    setValue(isoDate) {
      if (isBlank(isoDate) || !this.isSelectableDate(isoDate)) {
        return false;
      }

      this.input.value = isoDate;
      this.input.dispatchEvent(new Event("change", { bubbles: true }));
      this.syncCalendar();
      this.updateValueLabel();
      return true;
    }

    getDayState(isoDate) {
      if (isoDate < this.today) {
        return "unavailable";
      }

      if (isoDate > this.maxDate) {
        return "unavailable";
      }

      if (!this.restrictToAvailability) {
        return "available";
      }

      if (this.occupiedDates.has(isoDate)) {
        return "occupied";
      }

      if (this.offeredDates.has(isoDate)) {
        return "available";
      }

      return "unavailable";
    }

    selectDate(isoDate, shouldClose) {
      if (!this.setValue(isoDate)) {
        return;
      }

      if (shouldClose) {
        this.close();
      }
    }

    updateValueLabel() {
      this.valueNode.textContent = this.input.value
        ? formatDatePickerTriggerLabel(this.input.value)
        : this.placeholder;

      const hasValue = Boolean(this.input.value);
      syncTriggerClearButtonCollapsed(this.triggerClearButton, !hasValue);
    }

    open() {
      closeOtherPickers(this.root, true);
      this.syncCalendar();
      showPanel(this.panel, this.trigger);
      if (this.chevronNode) {
        this.chevronNode.classList.add("rotate-180");
      }
    }

    close() {
      hidePanel(this.panel, this.trigger);
      if (this.chevronNode) {
        this.chevronNode.classList.remove("rotate-180");
      }
    }
  }

  class TimeRangePicker {
    constructor(root) {
      this.root = root;
      this.trigger = root.querySelector("[data-picker-trigger]");
      this.controlRow = root.querySelector("[data-picker-control-row]");
      this.panel = root.querySelector("[data-picker-panel]");
      this.scrollRegion = root.querySelector("[data-picker-scroll-region]");
      this.closeButton = root.querySelector("[data-picker-close]");
      this.clearButton = root.querySelector("[data-picker-clear]");
      this.triggerClearButton = root.querySelector(
        "[data-picker-trigger-clear]",
      );
      this.chevronNode = root.querySelector("[data-picker-chevron]");
      this.valueNode = root.querySelector("[data-time-value]");
      this.helperNode = root.querySelector("[data-time-helper]");
      this.slotsNode = root.querySelector("[data-time-slots]");
      this.applyButton = root.querySelector("[data-time-apply]");
      this.startInput = root.querySelector("[data-time-start-input]");
      this.endInput = root.querySelector("[data-time-end-input]");
      this.dateInput = document.getElementById(root.dataset.dateInputId);
      this.placeholder = root.dataset.placeholder || "";
      this.pickDateFirstLabel = root.dataset.pickDateFirstLabel || "";
      this.noTimesLabel = root.dataset.noTimesLabel || "";
      this.pickEndLabel = root.dataset.pickEndLabel || "";
      this.pickStartLabel = root.dataset.pickStartLabel || "";
      this.fromLabel = root.dataset.fromLabel || "";
      this.offeredTimes = parseJson(root.dataset.offeredTimes, {});
      this.occupiedTimes = parseJson(root.dataset.occupiedTimes, {});
      this.restrictToAvailability = parseBoolean(
        root.dataset.restrictToAvailability,
        true,
      );
      this.minimumDurationMinutes = parseInt(
        root.dataset.minimumDurationMinutes || "120",
        10,
      );
      this.root.__timeRangePicker = this;
      mountFloatingPanel(this.panel);

      this.bind();
      this.syncSelection();
      this.render();
    }

    bind() {
      const toggleFromControlRow = () => {
        if (this.panel.hidden) {
          this.open();
        } else {
          this.close();
        }
      };

      const row = this.controlRow || this.trigger;
      row.addEventListener("click", () => {
        toggleFromControlRow();
      });

      this.applyButton.addEventListener("click", () => {
        this.close();
      });

      if (this.closeButton) {
        this.closeButton.addEventListener("click", () => {
          this.close();
        });
      }

      [this.clearButton, this.triggerClearButton]
        .filter(Boolean)
        .forEach((button) => {
          button.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
            this.clear();
            this.close();
          });
        });

      if (this.chevronNode) {
        const toggleFromChevron = (event) => {
          event.preventDefault();
          event.stopPropagation();
          if (this.panel.hidden) {
            this.open();
          } else {
            this.close();
          }
        };

        this.chevronNode.addEventListener("click", toggleFromChevron);
        this.chevronNode.addEventListener("keydown", (event) => {
          if (event.key === "Enter" || event.key === " ") {
            toggleFromChevron(event);
          }
        });
      }

      if (this.panel && this.scrollRegion) {
        this.panel.addEventListener(
          "wheel",
          (event) => {
            if (this.panel.hidden) {
              return;
            }

            event.preventDefault();
            this.scrollRegion.scrollTop += event.deltaY;
          },
          { passive: false },
        );
      }

      if (this.dateInput) {
        this.dateInput.addEventListener("change", () => {
          this.syncSelection();
          this.render();
        });
      }
    }

    currentDate() {
      return this.dateInput ? this.dateInput.value : "";
    }

    getOfferedTimesForDate(isoDate) {
      if (!this.restrictToAvailability && isBlank(isoDate)) {
        return new Set(ALL_TIMES);
      }

      if (isBlank(isoDate)) {
        return new Set();
      }

      if (!this.restrictToAvailability) {
        return new Set(ALL_TIMES);
      }

      return new Set(this.offeredTimes[isoDate] || []);
    }

    getOccupiedTimesForDate(isoDate) {
      if (isBlank(isoDate) || !this.restrictToAvailability) {
        return new Set();
      }

      return new Set(this.occupiedTimes[isoDate] || []);
    }

    currentOfferedTimes() {
      return this.getOfferedTimesForDate(this.currentDate());
    }

    currentOccupiedTimes() {
      return this.getOccupiedTimesForDate(this.currentDate());
    }

    clear() {
      this.startInput.value = "";
      this.endInput.value = "";
      this.dispatchSelectionChange();
      this.render();
    }

    dispatchSelectionChange() {
      this.startInput.dispatchEvent(new Event("change", { bubbles: true }));
      this.endInput.dispatchEvent(new Event("change", { bubbles: true }));
    }

    hasBookableEndFrom(date, startTime) {
      const startIndex = ALL_TIMES.indexOf(startTime);

      if (startIndex < 0) {
        return false;
      }

      for (let index = startIndex + 1; index < ALL_TIMES.length; index += 1) {
        if (this.isContinuousRangeForDate(date, startTime, ALL_TIMES[index])) {
          return true;
        }
      }

      return false;
    }

    hasOccupiedBoundaryInRange(date, startTime, endTime) {
      const occupiedTimes = this.getOccupiedTimesForDate(date);
      const startIndex = ALL_TIMES.indexOf(startTime);
      const endIndex = ALL_TIMES.indexOf(endTime);

      if (startIndex < 0 || endIndex <= startIndex) {
        return false;
      }

      for (let index = startIndex; index < endIndex; index += 1) {
        if (occupiedTimes.has(ALL_TIMES[index])) {
          return true;
        }
      }

      return false;
    }

    isContinuousRangeForDate(date, startTime, endTime) {
      const offeredTimes = this.getOfferedTimesForDate(date);
      const occupiedTimes = this.getOccupiedTimesForDate(date);
      const startIndex = ALL_TIMES.indexOf(startTime);
      const endIndex = ALL_TIMES.indexOf(endTime);

      if (this.restrictToAvailability && isBlank(date)) {
        return false;
      }

      if (startIndex < 0 || endIndex <= startIndex) {
        return false;
      }

      if (minutesBetween(startTime, endTime) < this.minimumDurationMinutes) {
        return false;
      }

      for (let index = startIndex; index < endIndex; index += 1) {
        if (!offeredTimes.has(ALL_TIMES[index])) {
          return false;
        }
      }

      for (let index = startIndex; index < endIndex; index += 1) {
        if (occupiedTimes.has(ALL_TIMES[index])) {
          return false;
        }
      }

      return true;
    }

    setSelection(startTime, endTime) {
      if (isBlank(startTime)) {
        this.clear();
        return true;
      }

      const date = this.currentDate();

      if (isBlank(date) && this.restrictToAvailability) {
        return false;
      }

      if (isBlank(endTime)) {
        if (!this.hasBookableEndFrom(date, startTime)) {
          return false;
        }

        this.startInput.value = startTime;
        this.endInput.value = "";
        this.dispatchSelectionChange();
        this.render();
        return true;
      }

      if (!this.isContinuousRangeForDate(date, startTime, endTime)) {
        return false;
      }

      this.startInput.value = startTime;
      this.endInput.value = endTime;
      this.dispatchSelectionChange();
      this.render();
      return true;
    }

    syncSelection() {
      const date = this.currentDate();

      if (isBlank(date)) {
        if (this.restrictToAvailability) {
          this.startInput.value = "";
          this.endInput.value = "";
          return;
        }

        if (
          this.startInput.value &&
          !this.endInput.value &&
          !this.hasBookableEndFrom(date, this.startInput.value)
        ) {
          this.startInput.value = "";
          this.endInput.value = "";
          return;
        }

        if (
          this.startInput.value &&
          this.endInput.value &&
          !this.isContinuousRangeForDate(
            date,
            this.startInput.value,
            this.endInput.value,
          )
        ) {
          this.endInput.value = "";
        }

        if (!this.startInput.value) {
          this.endInput.value = "";
        }
        return;
      }

      if (
        this.startInput.value &&
        !this.hasBookableEndFrom(date, this.startInput.value)
      ) {
        this.startInput.value = "";
        this.endInput.value = "";
      }

      if (
        this.startInput.value &&
        this.endInput.value &&
        !this.isContinuousRangeForDate(
          date,
          this.startInput.value,
          this.endInput.value,
        )
      ) {
        this.endInput.value = "";
      }

      if (!this.startInput.value) {
        this.endInput.value = "";
      }
    }

    handleSlotClick(time) {
      const date = this.currentDate();

      if (isBlank(date)) {
        return;
      }

      if (!this.startInput.value || this.endInput.value) {
        if (!this.hasBookableEndFrom(date, time)) {
          return;
        }

        this.startInput.value = time;
        this.endInput.value = "";
      } else if (compareTimes(time, this.startInput.value) <= 0) {
        if (!this.hasBookableEndFrom(date, time)) {
          return;
        }

        this.startInput.value = time;
        this.endInput.value = "";
      } else if (
        this.isContinuousRangeForDate(date, this.startInput.value, time)
      ) {
        this.endInput.value = time;
      } else if (this.hasBookableEndFrom(date, time)) {
        this.startInput.value = time;
        this.endInput.value = "";
      } else {
        return;
      }

      this.dispatchSelectionChange();
      this.render();
    }

    open() {
      closeOtherPickers(this.root, true);
      showPanel(this.panel, this.trigger);
      if (this.chevronNode) {
        this.chevronNode.classList.add("rotate-180");
      }
      requestAnimationFrame(() => {
        this.scrollToSelection();
      });
    }

    close() {
      hidePanel(this.panel, this.trigger);
      if (this.chevronNode) {
        this.chevronNode.classList.remove("rotate-180");
      }
    }

    scrollToSelection() {
      const selectedButton = this.slotsNode.querySelector(
        '[data-state="selected"]',
      );

      if (selectedButton) {
        this.slotsNode.scrollTo({
          top:
            selectedButton.offsetTop -
            this.slotsNode.offsetTop -
            this.slotsNode.clientHeight / 2 +
            selectedButton.clientHeight,
          behavior: "smooth",
        });
      }
    }

    helperText(date) {
      if (isBlank(date)) {
        if (
          !this.restrictToAvailability &&
          this.startInput.value &&
          !this.endInput.value
        ) {
          return this.pickEndLabel;
        }
        return this.pickDateFirstLabel;
      }

      if (!ALL_TIMES.some((time) => this.hasBookableEndFrom(date, time))) {
        return this.noTimesLabel;
      }

      if (this.startInput.value && !this.endInput.value) {
        return this.pickEndLabel;
      }

      return this.pickStartLabel;
    }

    render() {
      const selectedDate = this.currentDate();
      const hasDate = Boolean(selectedDate);

      this.slotsNode.innerHTML = "";

      ALL_TIMES.forEach((time) => {
        const state = this.buttonState(time);
        const button = createElement(
          "button",
          SLOT_BASE_CLASS + " " + slotStateClass(state),
          displayTime(time),
        );
        button.type = "button";
        button.dataset.state = state;

        if (state === "selected") {
          button.setAttribute("aria-pressed", "true");
        }

        if (state === "occupied" || state === "unavailable") {
          button.disabled = true;
        } else {
          button.addEventListener("click", () => {
            this.handleSlotClick(time);
          });
        }

        this.slotsNode.appendChild(button);
      });

      const helperText = this.helperText(selectedDate);
      this.helperNode.hidden = isBlank(helperText) || !hasDate;

      if (!hasDate) {
        this.helperNode.hidden = false;
      }

      this.helperNode.textContent = helperText || "";
      this.updateTriggerLabel();
    }

    baseStartState(time) {
      const date = this.currentDate();
      const offeredTimes = this.getOfferedTimesForDate(date);
      const occupiedTimes = this.getOccupiedTimesForDate(date);

      if (!offeredTimes.has(time)) {
        return "unavailable";
      }

      if (occupiedTimes.has(time)) {
        return "occupied";
      }

      if (!this.hasBookableEndFrom(date, time)) {
        return "unavailable";
      }

      return "available";
    }

    buttonState(time) {
      const date = this.currentDate();
      const offeredTimes = this.getOfferedTimesForDate(date);
      const occupiedTimes = this.getOccupiedTimesForDate(date);

      if (isBlank(date)) {
        return "unavailable";
      }

      if (this.startInput.value === time || this.endInput.value === time) {
        return "selected";
      }

      if (
        this.startInput.value &&
        this.endInput.value &&
        compareTimes(time, this.startInput.value) > 0 &&
        compareTimes(time, this.endInput.value) < 0
      ) {
        return "range";
      }

      if (occupiedTimes.has(time)) {
        return "occupied";
      }

      if (!this.startInput.value || this.endInput.value) {
        return this.baseStartState(time);
      }

      if (compareTimes(time, this.startInput.value) <= 0) {
        return this.baseStartState(time);
      }

      if (this.isContinuousRangeForDate(date, this.startInput.value, time)) {
        return "available";
      }

      if (!offeredTimes.has(time)) {
        return "unavailable";
      }

      if (this.hasOccupiedBoundaryInRange(date, this.startInput.value, time)) {
        return "occupied";
      }

      return "unavailable";
    }

    updateTriggerLabel() {
      if (this.startInput.value && this.endInput.value) {
        this.valueNode.textContent =
          displayTime(this.startInput.value) + " - " + displayTime(this.endInput.value);
      } else if (this.startInput.value) {
        this.valueNode.textContent =
          this.fromLabel + " " + displayTime(this.startInput.value);
      } else {
        this.valueNode.textContent = this.placeholder;
      }

      const hasSelection = Boolean(
        this.startInput.value || this.endInput.value,
      );
      syncTriggerClearButtonCollapsed(
        this.triggerClearButton,
        !hasSelection,
      );
    }
  }

  function closeOtherPickers(currentRoot, forceCloseAll) {
    document
      .querySelectorAll("[data-date-picker], [data-time-range-picker]")
      .forEach((root) => {
        if (root === currentRoot) {
          return;
        }

        const panel =
          root.__datePicker?.panel ||
          root.__timeRangePicker?.panel ||
          root.querySelector("[data-picker-panel]");
        const trigger = root.querySelector("[data-picker-trigger]");
        const isTimePicker = root.hasAttribute("data-time-range-picker");

        if (isTimePicker && !forceCloseAll) {
          if (panel && panel.hidden) {
            root.style.zIndex = "";
          }
          return;
        }

        if (panel && !panel.hidden) {
          hidePanel(panel, trigger);
        } else if (panel) {
          panel.classList.add("hidden");
          root.style.zIndex = "";
        }
      });
  }

  function initializeFilterPersistence() {
    const controls = getPickerControls();

    if (!controls || !controls.datePicker || !controls.timePicker) {
      return;
    }

    const path = window.location.pathname;
    const isMarketplacePage = path.endsWith("/marketplace");
    const isItemPage = /\/item\/\d+$/.test(path);
    const storedState = readStoredFilters();
    const pageState = currentPickerState(controls);

    if (isItemPage) {
      initializeItemAvailabilityAlert(controls, storedState);
    } else if (hasAnyFilter(pageState)) {
      syncPersistentFilters(pageState);
    } else if (hasAnyFilter(storedState)) {
      controls.datePicker.setValue(storedState.date);
      controls.timePicker.setSelection(
        storedState.startTime,
        storedState.endTime,
      );
      syncPersistentFilters(currentPickerState(controls));

      if (isMarketplacePage && storedState.date) {
        const syncedUrl = buildUrlWithFilters(
          window.location.href,
          storedState,
        );

        if (syncedUrl !== window.location.href) {
          window.location.replace(syncedUrl);
          return;
        }
      }
    } else {
      syncPersistentFilters(pageState);
    }

    [controls.dateInput, controls.startInput, controls.endInput].forEach(
      (input) => {
        input.addEventListener("change", () => {
          syncPersistentFilters(currentPickerState(controls));
        });
      },
    );
  }

  function initializeItemAvailabilityAlert(controls, storedState) {
    const alertRoot = document.querySelector("[data-item-unavailable-alert]");

    if (!alertRoot) {
      syncPersistentFilters(currentPickerState(controls));
      return;
    }

    const clearButton = alertRoot.querySelector(
      "[data-item-unavailable-clear]",
    );
    const marketplaceButton = alertRoot.querySelector(
      "[data-item-unavailable-marketplace]",
    );

    clearButton.addEventListener("click", () => {
      clearStoredFilters();
      window.location.assign(stripAvailabilityFilters(window.location.href));
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
        buildUrlWithFilters(alertRoot.dataset.marketplaceUrl, storedState),
      );
    });

    if (!hasCompleteRange(storedState)) {
      syncPersistentFilters(currentPickerState(controls));
      return;
    }

    if (
      !controls.datePicker.isSelectableDate(storedState.date) ||
      !controls.timePicker.isContinuousRangeForDate(
        storedState.date,
        storedState.startTime,
        storedState.endTime,
      )
    ) {
      openUnavailableAlert(alertRoot);
      return;
    }

    controls.datePicker.setValue(storedState.date);
    controls.timePicker.setSelection(
      storedState.startTime,
      storedState.endTime,
    );
    syncPersistentFilters(currentPickerState(controls));
  }

  document.addEventListener("click", (event) => {
    if (event.target.closest("[data-picker-panel]")) {
      return;
    }

    const pickerRoot = event.target.closest(
      "[data-date-picker], [data-time-range-picker]",
    );

    if (pickerRoot) {
      closeOtherPickers(pickerRoot, false);
      return;
    }

    closeOtherPickers(null, false);
  });

  document.addEventListener(
    "wheel",
    (event) => {
      if (!event.target.closest("[data-picker-panel]")) {
        closeOtherPickers(null, true);
      }
    },
    { passive: true },
  );

  document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-date-picker]").forEach((root) => {
      new DatePicker(root);
    });

    document.querySelectorAll("[data-time-range-picker]").forEach((root) => {
      new TimeRangePicker(root);
    });

    initializeReservationPriceSummaries();
  });

  window.addEventListener("resize", () => {
    closeOtherPickers(null, true);
  });
  window.addEventListener(
    "scroll",
    () => {
      closeOtherPickers(null, true);
    },
    { passive: true },
  );
})();
