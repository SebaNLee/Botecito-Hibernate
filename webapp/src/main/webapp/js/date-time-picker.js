(function () {
  const WEEKDAY_LABELS = ["DO", "LU", "MA", "MI", "JU", "VI", "SA"];
  const MONTH_FORMATTER = new Intl.DateTimeFormat("es-AR", {
    month: "long",
    year: "numeric",
  });
  const DATE_FORMATTER = new Intl.DateTimeFormat("es-AR", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  const FILTER_STORAGE_KEY = "paw.marketplaceFilters";
  const ALL_TIMES = buildAllTimes();
  const MONTH_SECTION_CLASS = "picker-month";
  const MONTH_TITLE_CLASS =
    "mb-3 text-xs font-extrabold uppercase tracking-[0.12em] text-on-surface-variant";
  const WEEKDAY_ROW_CLASS = "mb-2 grid grid-cols-7 gap-1.5";
  const WEEKDAY_CLASS =
    "flex justify-center text-[11px] font-extrabold uppercase tracking-[0.12em] text-outline";
  const CALENDAR_GRID_CLASS = "grid grid-cols-7 gap-1.5";
  const CALENDAR_SPACER_CLASS = "min-h-10";
  const DAY_BASE_CLASS =
    "min-h-10 rounded-xl border border-transparent text-sm font-bold transition-all duration-150";
  const DAY_AVAILABLE_CLASS =
    "bg-surface-container-low text-on-surface cursor-pointer hover:-translate-y-0.5 hover:shadow-[0_12px_24px_rgba(0,93,167,0.12)]";
  const DAY_UNAVAILABLE_CLASS =
    "bg-[rgba(212,227,255,0.45)] text-[rgba(64,71,82,0.46)] cursor-not-allowed";
  const DAY_OCCUPIED_CLASS =
    "bg-error-container/70 text-on-error-container/70 line-through cursor-not-allowed";
  const DAY_SELECTED_CLASS =
    "bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_16px_32px_rgba(0,93,167,0.24)]";
  const DAY_TODAY_CLASS = "border-primary ring-2 ring-primary/18 ring-inset";
  const SLOT_BASE_CLASS =
    "min-h-10 rounded-xl border border-transparent text-sm font-bold transition-all duration-150";
  const SLOT_AVAILABLE_CLASS =
    "bg-surface-container-low text-on-surface cursor-pointer hover:-translate-y-0.5 hover:shadow-[0_12px_24px_rgba(0,93,167,0.12)]";
  const SLOT_UNAVAILABLE_CLASS =
    "bg-[rgba(212,227,255,0.45)] text-[rgba(64,71,82,0.46)] cursor-not-allowed";
  const SLOT_OCCUPIED_CLASS =
    "border border-error/25 bg-error/14 text-error line-through cursor-not-allowed";
  const SLOT_SELECTED_CLASS =
    "bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_16px_32px_rgba(0,93,167,0.24)]";
  const SLOT_RANGE_CLASS =
    "bg-gradient-to-br from-primary to-primary-container text-on-primary";

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

  function getTriggerBand(trigger) {
    const scope = trigger.closest("form") || document;
    const triggerRect = trigger.getBoundingClientRect();
    const triggerCenter = (triggerRect.top + triggerRect.bottom) / 2;
    const relatedTriggers = [
      ...scope.querySelectorAll("[data-picker-trigger]"),
    ].filter((otherTrigger) => {
      const rect = otherTrigger.getBoundingClientRect();
      const center = (rect.top + rect.bottom) / 2;
      return (
        Math.abs(center - triggerCenter) <=
        Math.max(triggerRect.height, rect.height, 56)
      );
    });

    return relatedTriggers.reduce(
      (band, otherTrigger) => {
        const rect = otherTrigger.getBoundingClientRect();
        return {
          top: Math.min(band.top, rect.top),
          bottom: Math.max(band.bottom, rect.bottom),
        };
      },
      { top: triggerRect.top, bottom: triggerRect.bottom },
    );
  }

  function showPanel(panel, trigger) {
    panel.hidden = false;
    panel.classList.remove("hidden");
    panel.classList.add("flex");
    trigger.setAttribute("aria-expanded", "true");
    panel.style.zIndex = "9999";
    const root = panel.closest("[data-date-picker], [data-time-range-picker]");
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
    const root = panel.closest("[data-date-picker], [data-time-range-picker]");
    if (root) {
      root.style.zIndex = "";
    }
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
    const triggerBand = getTriggerBand(trigger);
    const naturalPanelHeight = panel.offsetHeight;
    const maxViewportHeight = Math.max(120, safeBottom - safeTop);
    const panelHeight = Math.min(naturalPanelHeight, maxViewportHeight);
    const gap = 12;
    const spaceBelow = safeBottom - triggerBand.bottom - gap;
    const spaceAbove = triggerBand.top - safeTop - gap;
    const openUpwards = spaceAbove >= spaceBelow;
    const top = openUpwards
      ? Math.max(safeTop, triggerBand.top - panelHeight - gap)
      : Math.min(
          safeBottom - panelHeight,
          Math.max(safeTop, triggerBand.bottom + gap),
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
      panel.style.maxHeight = naturalPanelHeight + "px";
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

  function todayIsoDate() {
    const today = new Date();
    return toIsoDate(today.getFullYear(), today.getMonth(), today.getDate());
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
    return Boolean(
      state && (state.date || state.startTime || state.endTime),
    );
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
    document.querySelectorAll("[data-marketplace-item-link]").forEach((link) => {
      if (!link.dataset.baseHref) {
        link.dataset.baseHref = link.getAttribute("href") || "";
      }

      link.setAttribute("href", buildUrlWithFilters(link.dataset.baseHref, state));
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

  class DatePicker {
    constructor(root) {
      this.root = root;
      this.input = root.querySelector("[data-picker-input]");
      this.trigger = root.querySelector("[data-picker-trigger]");
      this.panel = root.querySelector("[data-picker-panel]");
      this.scrollRegion = root.querySelector("[data-picker-scroll-region]");
      this.closeButton = root.querySelector("[data-picker-close]");
      this.clearButton = root.querySelector("[data-picker-clear]");
      this.valueNode = root.querySelector("[data-picker-value]");
      this.monthTitleNode = root.querySelector("[data-picker-month-title]");
      this.monthsContainer = root.querySelector("[data-picker-months]");
      this.placeholder = root.dataset.placeholder || "Selecciona fecha";
      this.offeredDates = new Set(parseJson(root.dataset.offeredDates, []));
      this.occupiedDates = new Set(parseJson(root.dataset.occupiedDates, []));
      this.restrictToAvailability = parseBoolean(
        root.dataset.restrictToAvailability,
        true,
      );
      this.monthKeys = clampMonthRange();
      this.activeMonthIndex = this.resolveMonthIndex(this.input.value);
      this.today = todayIsoDate();
      this.root.__datePicker = this;

      this.bind();
      this.render();
      this.updateValueLabel();
    }

    bind() {
      this.trigger.addEventListener("click", () => {
        if (this.panel.hidden) {
          this.open();
        }
      });

      this.root.querySelectorAll("[data-picker-nav]").forEach((button) => {
        button.addEventListener("click", () => {
          const direction = button.dataset.pickerNav === "next" ? 1 : -1;
          this.navigateMonth(direction);
        });
      });

      if (this.closeButton) {
        this.closeButton.addEventListener("click", () => {
          this.close();
        });
      }

      if (this.clearButton) {
        this.clearButton.addEventListener("click", () => {
          this.clear();
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
    }

    resolveMonthIndex(isoDate) {
      if (!isoDate) {
        return 0;
      }

      const [year, month] = isoDateParts(isoDate);
      const key = monthKey(year, month - 1);
      const index = this.monthKeys.indexOf(key);
      return index >= 0 ? index : 0;
    }

    isSelectableDate(isoDate) {
      return this.getDayState(isoDate) === "available";
    }

    clear() {
      this.input.value = "";
      this.input.dispatchEvent(new Event("change", { bubbles: true }));
      this.updateValueLabel();
      this.render();
    }

    setValue(isoDate) {
      if (isBlank(isoDate) || !this.isSelectableDate(isoDate)) {
        return false;
      }

      this.input.value = isoDate;
      this.input.dispatchEvent(new Event("change", { bubbles: true }));
      this.updateValueLabel();
      this.activeMonthIndex = this.resolveMonthIndex(isoDate);
      this.render();
      return true;
    }

    render() {
      this.monthsContainer.innerHTML = "";
      const key = this.monthKeys[this.activeMonthIndex];

      if (key) {
        const [year, monthIndex] = key.split("-").map(Number);
        this.monthsContainer.appendChild(
          this.buildMonthSection(year, monthIndex),
        );
      }

      this.updateMonthTitle();
      this.updateNavigationState();
    }

    buildMonthSection(year, monthIndex) {
      const section = createElement("section", MONTH_SECTION_CLASS);
      section.dataset.monthKey = monthKey(year, monthIndex);

      const title = createElement(
        "h4",
        MONTH_TITLE_CLASS,
        formatMonthLabel(new Date(year, monthIndex, 1)),
      );
      section.appendChild(title);

      const weekdayRow = createElement("div", WEEKDAY_ROW_CLASS);
      WEEKDAY_LABELS.forEach((label) => {
        weekdayRow.appendChild(createElement("span", WEEKDAY_CLASS, label));
      });
      section.appendChild(weekdayRow);

      const grid = createElement("div", CALENDAR_GRID_CLASS);
      const firstDay = new Date(year, monthIndex, 1).getDay();
      const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();

      for (let spacer = 0; spacer < firstDay; spacer += 1) {
        grid.appendChild(createElement("span", CALENDAR_SPACER_CLASS));
      }

      for (let day = 1; day <= daysInMonth; day += 1) {
        const isoDate = toIsoDate(year, monthIndex, day);
        const state = this.getDayState(isoDate);
        const selected = this.input.value === isoDate;
        const isToday = this.today === isoDate;
        const className = [
          DAY_BASE_CLASS,
          dayStateClass(state, selected),
          isToday ? DAY_TODAY_CLASS : "",
        ]
          .filter(Boolean)
          .join(" ");
        const button = createElement("button", className, String(day));

        button.type = "button";
        button.dataset.state = state;

        if (selected) {
          button.dataset.selected = "true";
        }

        if (isToday) {
          button.dataset.today = "true";
          button.setAttribute("aria-current", "date");
        }

        if (state !== "available") {
          button.disabled = true;
        } else {
          button.addEventListener("click", () => {
            this.selectDate(isoDate, true);
          });
        }

        grid.appendChild(button);
      }

      section.appendChild(grid);
      return section;
    }

    getDayState(isoDate) {
      if (isoDate < this.today) {
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
        ? formatLongDate(this.input.value)
        : this.placeholder;
    }

    open() {
      closeOtherPickers(this.root, true);
      this.activeMonthIndex = this.resolveMonthIndex(this.input.value || this.today);
      this.render();
      showPanel(this.panel, this.trigger);
    }

    close() {
      hidePanel(this.panel, this.trigger);
    }

    navigateMonth(direction) {
      const nextIndex = Math.min(
        Math.max(this.activeMonthIndex + direction, 0),
        this.monthKeys.length - 1,
      );

      if (nextIndex === this.activeMonthIndex) {
        return;
      }

      this.activeMonthIndex = nextIndex;
      this.render();
    }

    updateMonthTitle() {
      const key = this.monthKeys[this.activeMonthIndex];

      if (!key) {
        this.monthTitleNode.textContent = "Selecciona fecha";
        return;
      }

      const [year, monthIndex] = key.split("-").map(Number);
      this.monthTitleNode.textContent = formatMonthLabel(
        new Date(year, monthIndex, 1),
      );
    }

    updateNavigationState() {
      this.root.querySelectorAll("[data-picker-nav]").forEach((button) => {
        const isNext = button.dataset.pickerNav === "next";
        const disabled = isNext
          ? this.activeMonthIndex >= this.monthKeys.length - 1
          : this.activeMonthIndex <= 0;

        button.disabled = disabled;
        button.classList.toggle("opacity-40", disabled);
        button.classList.toggle("cursor-not-allowed", disabled);
        button.classList.toggle("cursor-pointer", !disabled);
        button.setAttribute("aria-disabled", String(disabled));
      });
    }
  }

  class TimeRangePicker {
    constructor(root) {
      this.root = root;
      this.trigger = root.querySelector("[data-picker-trigger]");
      this.panel = root.querySelector("[data-picker-panel]");
      this.scrollRegion = root.querySelector("[data-picker-scroll-region]");
      this.closeButton = root.querySelector("[data-picker-close]");
      this.clearButton = root.querySelector("[data-picker-clear]");
      this.valueNode = root.querySelector("[data-time-value]");
      this.helperNode = root.querySelector("[data-time-helper]");
      this.slotsNode = root.querySelector("[data-time-slots]");
      this.applyButton = root.querySelector("[data-time-apply]");
      this.startInput = root.querySelector("[data-time-start-input]");
      this.endInput = root.querySelector("[data-time-end-input]");
      this.dateInput = document.getElementById(root.dataset.dateInputId);
      this.placeholder = root.dataset.placeholder || "Inicio - Fin";
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

      this.bind();
      this.syncSelection();
      this.render();
    }

    bind() {
      this.trigger.addEventListener("click", () => {
        if (this.panel.hidden) {
          this.open();
        } else {
          this.close();
        }
      });

      this.applyButton.addEventListener("click", () => {
        this.close();
      });

      if (this.closeButton) {
        this.closeButton.addEventListener("click", () => {
          this.close();
        });
      }

      if (this.clearButton) {
        this.clearButton.addEventListener("click", () => {
          this.clear();
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

      if (isBlank(date) || startIndex < 0 || endIndex <= startIndex) {
        return false;
      }

      if (minutesBetween(startTime, endTime) < this.minimumDurationMinutes) {
        return false;
      }

      for (let index = startIndex; index <= endIndex; index += 1) {
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

      if (isBlank(date)) {
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
        this.startInput.value = "";
        this.endInput.value = "";
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
      requestAnimationFrame(() => {
        this.scrollToSelection();
      });
    }

    close() {
      hidePanel(this.panel, this.trigger);
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
        return "Selecciona una fecha para ver los horarios libres.";
      }

      if (!ALL_TIMES.some((time) => this.hasBookableEndFrom(date, time))) {
        return "No hay horarios libres para la fecha elegida.";
      }

      if (this.startInput.value && !this.endInput.value) {
        return "Selecciona un horario de fin.";
      }

      return "Selecciona un horario de inicio.";
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
          time,
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

      if (
        this.isContinuousRangeForDate(date, this.startInput.value, time)
      ) {
        return "available";
      }

      if (!offeredTimes.has(time)) {
        return "unavailable";
      }

      if (
        this.hasOccupiedBoundaryInRange(date, this.startInput.value, time)
      ) {
        return "occupied";
      }

      return "unavailable";
    }

    updateTriggerLabel() {
      if (this.startInput.value && this.endInput.value) {
        this.valueNode.textContent =
          this.startInput.value + " - " + this.endInput.value;
        return;
      }

      if (this.startInput.value) {
        this.valueNode.textContent = "Desde " + this.startInput.value;
        return;
      }

      this.valueNode.textContent = this.placeholder;
    }
  }

  function closeOtherPickers(currentRoot, forceCloseAll) {
    document
      .querySelectorAll("[data-date-picker], [data-time-range-picker]")
      .forEach((root) => {
        if (root === currentRoot) {
          return;
        }

        const panel = root.querySelector("[data-picker-panel]");
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
        const syncedUrl = buildUrlWithFilters(window.location.href, storedState);

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

    const clearButton = alertRoot.querySelector("[data-item-unavailable-clear]");
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
      alertRoot.hidden = false;
      alertRoot.classList.remove("hidden");
      alertRoot.classList.add("flex");
      return;
    }

    controls.datePicker.setValue(storedState.date);
    controls.timePicker.setSelection(storedState.startTime, storedState.endTime);
    syncPersistentFilters(currentPickerState(controls));
  }

  document.addEventListener("click", (event) => {
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

    initializeFilterPersistence();
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
