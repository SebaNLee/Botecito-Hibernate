(function () {
  var WEEKDAYS = [
    { key: "MONDAY" },
    { key: "TUESDAY" },
    { key: "WEDNESDAY" },
    { key: "THURSDAY" },
    { key: "FRIDAY" },
    { key: "SATURDAY" },
    { key: "SUNDAY" },
  ];

  var MIN_DURATION_MINUTES = 120;
  var SLOT_STEP_MINUTES = 30;
  var ALL_TIMES = buildAllTimes();

  function buildAllTimes() {
    var times = [];
    for (var h = 0; h < 24; h++) {
      for (var m = 0; m < 60; m += SLOT_STEP_MINUTES) {
        times.push(padTime(h, m));
      }
    }
    return times;
  }

  function padTime(h, m) {
    return String(h).padStart(2, "0") + ":" + String(m).padStart(2, "0");
  }

  function timeIndex(t) {
    return ALL_TIMES.indexOf(t);
  }

  function minutesBetween(a, b) {
    return (timeIndex(b) - timeIndex(a)) * SLOT_STEP_MINUTES;
  }

  var SLOT_BASE =
    "min-h-9 rounded-lg border text-xs font-bold transition-all duration-150 select-none";
  var SLOT_AVAILABLE =
    "border-transparent bg-surface-container-low text-on-surface cursor-pointer hover:-translate-y-0.5 hover:shadow-[0_8px_16px_rgba(0,93,167,0.10)]";
  var SLOT_SELECTED =
    "border-transparent bg-gradient-to-br from-primary to-primary-container text-on-primary shadow-[0_8px_20px_rgba(0,93,167,0.22)]";
  var SLOT_PENDING =
    "border-primary bg-primary/15 text-primary animate-pulse cursor-pointer";
  var SLOT_UNAVAILABLE =
    "border-transparent bg-[rgba(212,227,255,0.35)] text-[rgba(64,71,82,0.38)] cursor-not-allowed";
  var SLOT_DISABLED =
    "border-transparent bg-surface-container-high/40 text-outline/30 cursor-not-allowed";

  function WeeklyAvailabilityGrid(root) {
    this.root = root;
    this.hiddenContainer = root.querySelector("[data-availability-hidden-inputs]");
    this.form = root.closest("form");
    this.minDuration = parseInt(root.dataset.minDuration || MIN_DURATION_MINUTES, 10);
    this.noRangesText = root.dataset.noRangesText || "No time ranges selected";

    this.days = {};
    for (var i = 0; i < WEEKDAYS.length; i++) {
      this.days[WEEKDAYS[i].key] = {
        enabled: false,
        ranges: [],
        pendingStart: null,
      };
    }

    this.loadExisting();
    this.bindToggles();
    this.renderAll();
    this.bindFormSubmit();
  }

  WeeklyAvailabilityGrid.prototype.loadExisting = function () {
    var json = this.root.dataset.existingSlots;
    if (!json) return;
    try {
      var slots = JSON.parse(json);
      for (var i = 0; i < slots.length; i++) {
        var s = slots[i];
        if (!s.weekday || !this.days[s.weekday]) continue;
        this.days[s.weekday].ranges.push({ start: s.startTime, end: s.endTime });
      }
    } catch (e) {
      // ignore parse errors
    }

    for (var j = 0; j < WEEKDAYS.length; j++) {
      var key = WEEKDAYS[j].key;
      var toggle = this.root.querySelector('[data-day-toggle="' + key + '"]');
      if (toggle) {
        this.days[key].enabled = toggle.checked;
      }
    }
  };

  WeeklyAvailabilityGrid.prototype.bindToggles = function () {
    var self = this;
    for (var i = 0; i < WEEKDAYS.length; i++) {
      (function (key) {
        var toggle = self.root.querySelector('[data-day-toggle="' + key + '"]');
        if (!toggle) return;
        self.days[key].enabled = toggle.checked;
        toggle.addEventListener("change", function () {
          self.days[key].enabled = toggle.checked;
          self.days[key].pendingStart = null;
          self.renderDay(key);
        });
      })(WEEKDAYS[i].key);
    }
  };

  WeeklyAvailabilityGrid.prototype.renderAll = function () {
    for (var i = 0; i < WEEKDAYS.length; i++) {
      this.renderDay(WEEKDAYS[i].key);
    }
  };

  WeeklyAvailabilityGrid.prototype.renderDay = function (weekday) {
    var container = this.root.querySelector('[data-day-slots="' + weekday + '"]');
    if (!container) return;

    var day = this.days[weekday];
    var dayRow = container.closest("[data-availability-row]");
    if (dayRow) {
      dayRow.classList.toggle("opacity-50", !day.enabled);
    }

    container.innerHTML = "";

    for (var i = 0; i < ALL_TIMES.length; i++) {
      var time = ALL_TIMES[i];
      var state = this.slotState(weekday, time);
      var btn = document.createElement("button");
      btn.type = "button";
      btn.textContent = time;
      btn.className = SLOT_BASE + " " + this.stateClass(state);
      btn.dataset.state = state;

      if (state === "disabled" || state === "unavailable") {
        btn.disabled = true;
      } else {
        btn.addEventListener("click", this.handleClick.bind(this, weekday, time));
      }

      container.appendChild(btn);
    }

    this.renderRangeSummary(weekday);
  };

  WeeklyAvailabilityGrid.prototype.renderRangeSummary = function (weekday) {
    var summaryEl = this.root.querySelector('[data-day-summary="' + weekday + '"]');
    if (!summaryEl) return;

    var day = this.days[weekday];
    if (!day.enabled || day.ranges.length === 0) {
      summaryEl.textContent = day.enabled ? this.noRangesText : "";
      summaryEl.className = "mt-2 text-xs font-medium text-outline";
      return;
    }

    var sorted = day.ranges.slice().sort(function (a, b) {
      return timeIndex(a.start) - timeIndex(b.start);
    });

    var parts = sorted.map(function (r) { return r.start + " - " + r.end; });
    summaryEl.textContent = parts.join("  ·  ");
    summaryEl.className = "mt-2 text-xs font-bold text-primary";
  };

  WeeklyAvailabilityGrid.prototype.slotState = function (weekday, time) {
    var day = this.days[weekday];
    if (!day.enabled) return "disabled";

    if (day.pendingStart === time) return "pending";

    for (var i = 0; i < day.ranges.length; i++) {
      var r = day.ranges[i];
      var si = timeIndex(r.start);
      var ei = timeIndex(r.end);
      var ti = timeIndex(time);
      if (ti >= si && ti < ei) return "selected";
    }

    if (day.pendingStart !== null) {
      var pi = timeIndex(day.pendingStart);
      var ti2 = timeIndex(time);

      if (ti2 <= pi) return "available";

      var wouldOverlap = this.wouldOverlap(weekday, day.pendingStart, time);
      var duration = minutesBetween(day.pendingStart, time);

      if (wouldOverlap) return "unavailable";

      if (duration < this.minDuration) return "unavailable";

      return "available";
    }

    return "available";
  };

  WeeklyAvailabilityGrid.prototype.stateClass = function (state) {
    switch (state) {
      case "selected": return SLOT_SELECTED;
      case "pending": return SLOT_PENDING;
      case "unavailable": return SLOT_UNAVAILABLE;
      case "disabled": return SLOT_DISABLED;
      default: return SLOT_AVAILABLE;
    }
  };

  WeeklyAvailabilityGrid.prototype.wouldOverlap = function (weekday, start, end) {
    var day = this.days[weekday];
    var si = timeIndex(start);
    var ei = timeIndex(end);

    for (var i = 0; i < day.ranges.length; i++) {
      var r = day.ranges[i];
      var rsi = timeIndex(r.start);
      var rei = timeIndex(r.end);
      if (si < rei && ei > rsi) return true;
    }
    return false;
  };

  WeeklyAvailabilityGrid.prototype.findRangeAt = function (weekday, time) {
    var day = this.days[weekday];
    var ti = timeIndex(time);
    for (var i = 0; i < day.ranges.length; i++) {
      var r = day.ranges[i];
      if (ti >= timeIndex(r.start) && ti < timeIndex(r.end)) return i;
    }
    return -1;
  };

  WeeklyAvailabilityGrid.prototype.handleClick = function (weekday, time) {
    var day = this.days[weekday];
    if (!day.enabled) return;

    var existingIdx = this.findRangeAt(weekday, time);
    if (existingIdx >= 0) {
      day.ranges.splice(existingIdx, 1);
      day.pendingStart = null;
      this.renderDay(weekday);
      return;
    }

    if (day.pendingStart === null) {
      day.pendingStart = time;
      this.renderDay(weekday);
      return;
    }

    if (day.pendingStart === time) {
      day.pendingStart = null;
      this.renderDay(weekday);
      return;
    }

    var pi = timeIndex(day.pendingStart);
    var ti = timeIndex(time);

    if (ti <= pi) {
      day.pendingStart = time;
      this.renderDay(weekday);
      return;
    }

    var duration = minutesBetween(day.pendingStart, time);
    if (duration < this.minDuration) {
      day.pendingStart = time;
      this.renderDay(weekday);
      return;
    }

    if (this.wouldOverlap(weekday, day.pendingStart, time)) {
      day.pendingStart = time;
      this.renderDay(weekday);
      return;
    }

    day.ranges.push({ start: day.pendingStart, end: time });
    day.pendingStart = null;
    this.renderDay(weekday);
  };

  WeeklyAvailabilityGrid.prototype.bindFormSubmit = function () {
    var self = this;
    if (!this.form) return;

    this.form.addEventListener("submit", function () {
      self.serializeToHiddenInputs();
    });
  };

  WeeklyAvailabilityGrid.prototype.serializeToHiddenInputs = function () {
    if (!this.hiddenContainer) return;
    this.hiddenContainer.innerHTML = "";

    for (var i = 0; i < WEEKDAYS.length; i++) {
      var key = WEEKDAYS[i].key;
      var day = this.days[key];
      if (!day.enabled) continue;

      var sorted = day.ranges.slice().sort(function (a, b) {
        return timeIndex(a.start) - timeIndex(b.start);
      });

      for (var j = 0; j < sorted.length; j++) {
        var r = sorted[j];
        this.hiddenContainer.appendChild(
          hiddenInput("availabilityRanges", key + "|" + r.start + "|" + r.end)
        );
      }
    }
  };

  function hiddenInput(name, value) {
    var input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value;
    return input;
  }

  document.addEventListener("DOMContentLoaded", function () {
    var grids = document.querySelectorAll("[data-weekly-availability-grid]");
    grids.forEach(function (root) {
      new WeeklyAvailabilityGrid(root);
    });
  });
})();
