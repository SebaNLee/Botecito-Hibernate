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
  var TOTAL_MINUTES = 24 * 60;
  var TOTAL_STEPS = TOTAL_MINUTES / SLOT_STEP_MINUTES;
  var DAY_END_DISPLAY_LABEL = "23:59h";

  var PREVIEW_BLOCK_CLASS =
    "absolute top-0 h-8 rounded-md border border-primary/60 bg-primary/20 hidden pointer-events-none";
  var PREVIEW_LABEL_CLASS =
    "absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-primary/35 bg-surface px-2 py-0.5 text-[10px] font-bold text-primary";
  var PLACED_BLOCK_CLASS =
    "absolute top-0 h-8 rounded-md bg-gradient-to-r from-primary to-primary-container text-on-primary shadow-[0_8px_16px_rgba(0,93,167,0.28)] cursor-grab active:cursor-grabbing";
  var HANDLE_CLASS =
    "absolute top-0 h-8 w-4 -translate-x-1/2 cursor-ew-resize touch-none";
  var HANDLE_BAR_CLASS =
    "absolute left-1/2 top-0 h-8 w-[3px] -translate-x-1/2 rounded-full bg-on-primary";
  var HANDLE_LABEL_CLASS =
    "absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-outline-variant/30 bg-surface px-2 py-0.5 text-[10px] font-bold text-on-surface";
  var DELETE_CLASS =
    "absolute -bottom-8 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-error/40 bg-error/15 px-2 py-0.5 text-[10px] font-bold text-error hover:bg-error/25 transition-colors";

  function WeeklyAvailabilityGrid(root) {
    this.root = root;
    this.hiddenContainer = root.querySelector("[data-availability-hidden-inputs]");
    this.form = root.closest("form");
    this.minDuration = parseInt(root.dataset.minDuration || MIN_DURATION_MINUTES, 10);
    this.minSteps = Math.max(1, Math.ceil(this.minDuration / SLOT_STEP_MINUTES));
    this.noRangesText = root.dataset.noRangesText || "No time ranges selected";
    this.deleteText = root.dataset.deleteText || "Delete";
    this.dragState = null;

    this.days = {};
    for (var i = 0; i < WEEKDAYS.length; i++) {
      this.days[WEEKDAYS[i].key] = {
        enabled: false,
        ranges: [],
        nextId: 1,
        previewStart: null,
        dom: null,
      };
    }

    this.loadExisting();
    this.buildTimelines();
    this.bindToggles();
    this.bindGlobalDragging();
    this.renderAll();
    this.bindFormSubmit();
  }

  WeeklyAvailabilityGrid.prototype.loadExisting = function () {
    var json = this.root.dataset.existingSlots;
    if (!json) return;

    try {
      var slots = JSON.parse(json);
      for (var i = 0; i < slots.length; i++) {
        var slot = slots[i];
        if (!slot.weekday || !this.days[slot.weekday]) continue;

        var startStep = parseTimeToStep(slot.startTime, false);
        var endStep = parseTimeToStep(slot.endTime, true);
        if (startStep === null || endStep === null || endStep <= startStep) continue;

        this.days[slot.weekday].ranges.push({
          id: this.days[slot.weekday].nextId++,
          start: startStep,
          end: endStep,
        });
      }
    } catch (ignored) {
      // ignore parse errors
    }

    for (var j = 0; j < WEEKDAYS.length; j++) {
      var key = WEEKDAYS[j].key;
      var toggle = this.root.querySelector('[data-day-toggle="' + key + '"]');
      if (toggle) {
        this.days[key].enabled = toggle.checked;
      }

      this.days[key].ranges.sort(function (a, b) {
        return a.start - b.start;
      });
    }
  };

  WeeklyAvailabilityGrid.prototype.buildTimelines = function () {
    for (var i = 0; i < WEEKDAYS.length; i++) {
      var weekday = WEEKDAYS[i].key;
      var container = this.root.querySelector('[data-day-slots="' + weekday + '"]');
      if (!container) continue;

      container.innerHTML = "";

      var shell = document.createElement("div");
      shell.className = "mt-1";

      var wrapper = document.createElement("div");
      wrapper.className = "relative pt-7 pb-8";

      var axis = document.createElement("div");
      axis.className = "absolute inset-x-0 top-0 flex items-center justify-between text-[11px] font-bold text-outline";
      var axisStart = document.createElement("span");
      axisStart.textContent = "00:00h";
      var axisEnd = document.createElement("span");
      axisEnd.textContent = DAY_END_DISPLAY_LABEL;
      axis.appendChild(axisStart);
      axis.appendChild(axisEnd);

      var track = document.createElement("div");
      track.className = "relative h-12 rounded-xl border border-outline-variant/30 bg-surface-container-high/40 overflow-visible touch-none";
      track.setAttribute("data-timeline-track", weekday);

      var lane = document.createElement("div");
      lane.className = "absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 rounded-lg border border-outline-variant/25 bg-base-200/80";

      var ticks = document.createElement("div");
      ticks.className = "absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none";

      var previewLayer = document.createElement("div");
      previewLayer.className = "absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none";
      var preview = document.createElement("div");
      preview.className = PREVIEW_BLOCK_CLASS;
      var previewLabel = document.createElement("div");
      previewLabel.className = PREVIEW_LABEL_CLASS;
      preview.appendChild(previewLabel);
      previewLayer.appendChild(preview);

      var blocksLayer = document.createElement("div");
      blocksLayer.className = "absolute inset-x-1 top-1/2 h-8 -translate-y-1/2";

      track.appendChild(lane);
      track.appendChild(ticks);
      track.appendChild(previewLayer);
      track.appendChild(blocksLayer);
      wrapper.appendChild(axis);
      wrapper.appendChild(track);
      shell.appendChild(wrapper);
      container.appendChild(shell);

      this.days[weekday].dom = {
        dayRow: container.closest("[data-availability-row]"),
        track: track,
        ticks: ticks,
        preview: preview,
        previewLabel: previewLabel,
        blocksLayer: blocksLayer,
      };

      this.renderTicks(weekday);
      this.bindTrackEvents(weekday);
    }
  };

  WeeklyAvailabilityGrid.prototype.renderTicks = function (weekday) {
    var day = this.days[weekday];
    if (!day || !day.dom || !day.dom.ticks) return;

    day.dom.ticks.innerHTML = "";
    for (var step = 0; step <= TOTAL_STEPS; step++) {
      if (step % 2 !== 0) continue;

      var tick = document.createElement("span");
      var strong = step % 4 === 0;
      tick.className = "absolute top-0 h-8 w-px " + (strong ? "bg-outline-variant/45" : "bg-outline-variant/25");
      tick.style.left = percent(step) + "%";
      day.dom.ticks.appendChild(tick);
    }
  };

  WeeklyAvailabilityGrid.prototype.bindTrackEvents = function (weekday) {
    var self = this;
    var day = this.days[weekday];
    if (!day || !day.dom || !day.dom.track) return;

    day.dom.track.addEventListener("pointermove", function (event) {
      if (self.dragState) return;
      self.updatePreviewFromPointer(weekday, event);
    });

    day.dom.track.addEventListener("pointerleave", function () {
      if (self.dragState) return;
      self.clearPreview(weekday);
    });

    day.dom.track.addEventListener("pointerdown", function (event) {
      if (event.button !== 0 || self.dragState) return;
      if (event.target.closest("[data-block-id]")) return;
      self.insertFromPointer(weekday, event);
    });
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
          self.days[key].previewStart = null;

          if (!toggle.checked && self.dragState && self.dragState.weekday === key) {
            self.dragState = null;
            document.body.classList.remove("select-none");
          }

          self.renderDay(key);
        });
      })(WEEKDAYS[i].key);
    }
  };

  WeeklyAvailabilityGrid.prototype.bindGlobalDragging = function () {
    var self = this;

    document.addEventListener("pointermove", function (event) {
      self.handleGlobalPointerMove(event);
    });

    document.addEventListener("pointerup", function (event) {
      self.handleGlobalPointerUp(event);
    });

    document.addEventListener("pointercancel", function () {
      self.dragState = null;
      document.body.classList.remove("select-none");
      self.renderAll();
    });
  };

  WeeklyAvailabilityGrid.prototype.renderAll = function () {
    for (var i = 0; i < WEEKDAYS.length; i++) {
      this.renderDay(WEEKDAYS[i].key);
    }
  };

  WeeklyAvailabilityGrid.prototype.renderDay = function (weekday) {
    var day = this.days[weekday];
    if (!day || !day.dom) return;

    if (day.dom.dayRow) {
      day.dom.dayRow.classList.toggle("opacity-50", !day.enabled);
    }

    if (day.dom.track) {
      day.dom.track.classList.toggle("pointer-events-none", !day.enabled);
      day.dom.track.classList.toggle("opacity-60", !day.enabled);
    }

    this.renderRanges(weekday);
    this.renderPreview(weekday);
    this.renderRangeSummary(weekday);
  };

  WeeklyAvailabilityGrid.prototype.renderRanges = function (weekday) {
    var self = this;
    var day = this.days[weekday];
    if (!day || !day.dom || !day.dom.blocksLayer) return;

    day.dom.blocksLayer.innerHTML = "";

    var sorted = day.ranges.slice().sort(function (a, b) {
      return a.start - b.start;
    });

    for (var i = 0; i < sorted.length; i++) {
      var range = sorted[i];
      var block = document.createElement("div");
      block.className = PLACED_BLOCK_CLASS;
      block.style.left = percent(range.start) + "%";
      block.style.width = percent(range.end - range.start) + "%";
      block.setAttribute("data-block-id", String(range.id));

      var centerLabel = document.createElement("span");
      centerLabel.className = "absolute inset-0 flex items-center justify-center text-[10px] font-bold pointer-events-none";
      centerLabel.textContent = formatRangeLabel(range.start, range.end);
      block.appendChild(centerLabel);

      var leftHandle = document.createElement("button");
      leftHandle.type = "button";
      leftHandle.className = HANDLE_CLASS;
      leftHandle.style.left = "0%";
      leftHandle.setAttribute("aria-label", "Resize start");
      var leftBar = document.createElement("span");
      leftBar.className = HANDLE_BAR_CLASS;
      var leftPill = document.createElement("span");
      leftPill.className = HANDLE_LABEL_CLASS;
      leftPill.textContent = formatBoundaryLabel(range.start, false);
      leftHandle.appendChild(leftBar);
      leftHandle.appendChild(leftPill);

      var rightHandle = document.createElement("button");
      rightHandle.type = "button";
      rightHandle.className = HANDLE_CLASS;
      rightHandle.style.left = "100%";
      rightHandle.setAttribute("aria-label", "Resize end");
      var rightBar = document.createElement("span");
      rightBar.className = HANDLE_BAR_CLASS;
      var rightPill = document.createElement("span");
      rightPill.className = HANDLE_LABEL_CLASS;
      rightPill.textContent = formatBoundaryLabel(range.end, true);
      rightHandle.appendChild(rightBar);
      rightHandle.appendChild(rightPill);

      var deleteButton = document.createElement("button");
      deleteButton.type = "button";
      deleteButton.className = DELETE_CLASS;
      deleteButton.setAttribute("aria-label", self.deleteText);
      deleteButton.textContent = self.deleteText;

      leftHandle.addEventListener("pointerdown", self.handleResizeStart.bind(self, weekday, range.id, "left"));
      rightHandle.addEventListener("pointerdown", self.handleResizeStart.bind(self, weekday, range.id, "right"));
      deleteButton.addEventListener("click", self.handleDelete.bind(self, weekday, range.id));
      block.addEventListener("pointerdown", self.handleDragStart.bind(self, weekday, range.id));

      block.appendChild(leftHandle);
      block.appendChild(rightHandle);
      block.appendChild(deleteButton);
      day.dom.blocksLayer.appendChild(block);
    }
  };

  WeeklyAvailabilityGrid.prototype.renderPreview = function (weekday) {
    var day = this.days[weekday];
    if (!day || !day.dom || !day.dom.preview) return;

    var preview = day.dom.preview;
    if (!day.enabled || day.previewStart === null || this.dragState) {
      preview.classList.add("hidden");
      return;
    }

    var start = day.previewStart;
    var end = start + this.minSteps;
    preview.style.left = percent(start) + "%";
    preview.style.width = percent(end - start) + "%";
    day.dom.previewLabel.textContent = formatRangeLabel(start, end);
    preview.classList.remove("hidden");
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
      return a.start - b.start;
    });
    var parts = sorted.map(function (range) {
      return formatRangeLabel(range.start, range.end);
    });
    summaryEl.textContent = parts.join("  ·  ");
    summaryEl.className = "mt-2 text-xs font-bold text-primary";
  };

  WeeklyAvailabilityGrid.prototype.updatePreviewFromPointer = function (weekday, event) {
    var day = this.days[weekday];
    if (!day || !day.enabled || !day.dom || !day.dom.track) {
      this.clearPreview(weekday);
      return;
    }

    var pointerStep = pointerToStep(day.dom.track, event);
    if (pointerStep === null) {
      this.clearPreview(weekday);
      return;
    }

    var desiredStart = Math.round(pointerStep - this.minSteps / 2);
    var validStarts = this.validInsertStarts(weekday);
    var snappedStart = nearestStep(validStarts, desiredStart);

    if (snappedStart === null) {
      this.clearPreview(weekday);
      return;
    }

    day.previewStart = snappedStart;
    this.renderPreview(weekday);
  };

  WeeklyAvailabilityGrid.prototype.insertFromPointer = function (weekday, event) {
    var day = this.days[weekday];
    if (!day || !day.enabled) return;

    this.updatePreviewFromPointer(weekday, event);
    if (day.previewStart === null) return;

    var start = day.previewStart;
    var end = start + this.minSteps;
    if (!this.canPlaceRange(weekday, start, end, null)) {
      this.clearPreview(weekday);
      return;
    }

    day.ranges.push({ id: day.nextId++, start: start, end: end });
    day.ranges.sort(function (a, b) {
      return a.start - b.start;
    });

    this.clearPreview(weekday);
    this.renderDay(weekday);
    event.preventDefault();
  };

  WeeklyAvailabilityGrid.prototype.clearPreview = function (weekday) {
    var day = this.days[weekday];
    if (!day) return;
    day.previewStart = null;
    this.renderPreview(weekday);
  };

  WeeklyAvailabilityGrid.prototype.validInsertStarts = function (weekday) {
    var starts = [];
    for (var start = 0; start <= TOTAL_STEPS - this.minSteps; start++) {
      if (this.canPlaceRange(weekday, start, start + this.minSteps, null)) {
        starts.push(start);
      }
    }
    return starts;
  };

  WeeklyAvailabilityGrid.prototype.handleDelete = function (weekday, rangeId, event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    var day = this.days[weekday];
    if (!day) return;
    day.ranges = day.ranges.filter(function (range) {
      return range.id !== rangeId;
    });
    this.renderDay(weekday);
  };

  WeeklyAvailabilityGrid.prototype.handleDragStart = function (weekday, rangeId, event) {
    if (event.button !== 0) return;
    if (event.target.closest("button")) return;

    var day = this.days[weekday];
    if (!day || !day.enabled || !day.dom || !day.dom.track) return;

    var range = this.findRangeById(weekday, rangeId);
    if (!range) return;

    var pointerStep = pointerToStep(day.dom.track, event);
    if (pointerStep === null) return;

    this.dragState = {
      type: "move",
      weekday: weekday,
      id: rangeId,
      pointerId: event.pointerId,
      duration: range.end - range.start,
      offset: pointerStep - range.start,
    };

    document.body.classList.add("select-none");
    event.preventDefault();
    event.stopPropagation();
  };

  WeeklyAvailabilityGrid.prototype.handleResizeStart = function (weekday, rangeId, side, event) {
    if (event.button !== 0) return;

    var day = this.days[weekday];
    if (!day || !day.enabled) return;

    var range = this.findRangeById(weekday, rangeId);
    if (!range) return;

    this.dragState = {
      type: side === "left" ? "resize-left" : "resize-right",
      weekday: weekday,
      id: rangeId,
      pointerId: event.pointerId,
    };

    document.body.classList.add("select-none");
    event.preventDefault();
    event.stopPropagation();
  };

  WeeklyAvailabilityGrid.prototype.handleGlobalPointerMove = function (event) {
    if (!this.dragState) return;
    if (this.dragState.pointerId !== undefined && event.pointerId !== this.dragState.pointerId) {
      return;
    }

    var state = this.dragState;
    var day = this.days[state.weekday];
    if (!day || !day.enabled || !day.dom || !day.dom.track) return;

    var range = this.findRangeById(state.weekday, state.id);
    if (!range) return;

    var pointerStep = pointerToStep(day.dom.track, event);
    if (pointerStep === null) return;

    if (state.type === "move") {
      this.applyMoveDrag(state, range, pointerStep);
    } else if (state.type === "resize-left") {
      this.applyLeftResizeDrag(state, range, pointerStep);
    } else if (state.type === "resize-right") {
      this.applyRightResizeDrag(state, range, pointerStep);
    }

    this.renderDay(state.weekday);
    event.preventDefault();
  };

  WeeklyAvailabilityGrid.prototype.handleGlobalPointerUp = function (event) {
    if (!this.dragState) return;
    if (this.dragState.pointerId !== undefined && event.pointerId !== this.dragState.pointerId) {
      return;
    }

    this.dragState = null;
    document.body.classList.remove("select-none");
  };

  WeeklyAvailabilityGrid.prototype.applyMoveDrag = function (state, range, pointerStep) {
    var desiredStart = Math.round(pointerStep - state.offset);
    var duration = state.duration;

    var starts = [];
    for (var start = 0; start <= TOTAL_STEPS - duration; start++) {
      if (this.canPlaceRange(state.weekday, start, start + duration, state.id)) {
        starts.push(start);
      }
    }

    var snappedStart = nearestStep(starts, desiredStart);
    if (snappedStart === null) return;

    range.start = snappedStart;
    range.end = snappedStart + duration;
  };

  WeeklyAvailabilityGrid.prototype.applyLeftResizeDrag = function (state, range, pointerStep) {
    var desiredStart = Math.round(pointerStep);
    var maxStart = range.end - this.minSteps;
    if (maxStart < 0) return;

    var starts = [];
    for (var start = 0; start <= maxStart; start++) {
      if (this.canPlaceRange(state.weekday, start, range.end, state.id)) {
        starts.push(start);
      }
    }

    var snappedStart = nearestStep(starts, desiredStart);
    if (snappedStart === null) return;
    range.start = snappedStart;
  };

  WeeklyAvailabilityGrid.prototype.applyRightResizeDrag = function (state, range, pointerStep) {
    var desiredEnd = Math.round(pointerStep);
    var minEnd = range.start + this.minSteps;
    if (minEnd > TOTAL_STEPS) return;

    var ends = [];
    for (var end = minEnd; end <= TOTAL_STEPS; end++) {
      if (this.canPlaceRange(state.weekday, range.start, end, state.id)) {
        ends.push(end);
      }
    }

    var snappedEnd = nearestStep(ends, desiredEnd);
    if (snappedEnd === null) return;
    range.end = snappedEnd;
  };

  WeeklyAvailabilityGrid.prototype.findRangeById = function (weekday, rangeId) {
    var day = this.days[weekday];
    if (!day) return null;

    for (var i = 0; i < day.ranges.length; i++) {
      if (day.ranges[i].id === rangeId) {
        return day.ranges[i];
      }
    }

    return null;
  };

  WeeklyAvailabilityGrid.prototype.canPlaceRange = function (weekday, start, end, ignoreId) {
    if (start < 0 || end > TOTAL_STEPS) return false;
    if (end <= start) return false;
    if (end - start < this.minSteps) return false;

    var day = this.days[weekday];
    if (!day) return false;

    for (var i = 0; i < day.ranges.length; i++) {
      var range = day.ranges[i];
      if (ignoreId !== null && range.id === ignoreId) {
        continue;
      }

      if (!(end < range.start || start > range.end)) {
        return false;
      }
    }

    return true;
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
        return a.start - b.start;
      });

      for (var j = 0; j < sorted.length; j++) {
        var range = sorted[j];
        this.hiddenContainer.appendChild(
          hiddenInput(
            "availabilityRanges",
            key +
              "|" +
              serializeStep(range.start, false) +
              "|" +
              serializeStep(range.end, true)
          )
        );
      }
    }
  };

  function parseTimeToStep(value, isEnd) {
    if (!value || typeof value !== "string") return null;

    var match = /^(\d{2}):(\d{2})$/.exec(value.trim());
    if (!match) return null;

    var hour = parseInt(match[1], 10);
    var minute = parseInt(match[2], 10);

    if (isEnd && hour === 23 && minute === 59) {
      return TOTAL_STEPS;
    }

    if (hour < 0 || hour > 23) return null;
    if (minute !== 0 && minute !== 30) return null;

    var total = hour * 60 + minute;
    var step = total / SLOT_STEP_MINUTES;
    if (Math.floor(step) !== step) return null;

    return step;
  }

  function serializeStep(step, isEnd) {
    if (isEnd && step === TOTAL_STEPS) {
      return "23:59";
    }

    var minutes = step * SLOT_STEP_MINUTES;
    var hour = Math.floor(minutes / 60);
    var minute = minutes % 60;
    return padTime(hour, minute);
  }

  function formatRangeLabel(startStep, endStep) {
    return formatBoundaryLabel(startStep, false) + " - " + formatBoundaryLabel(endStep, true);
  }

  function formatBoundaryLabel(step, isEnd) {
    if (isEnd && step === TOTAL_STEPS) {
      return DAY_END_DISPLAY_LABEL;
    }
    return serializeStep(step, false) + "h";
  }

  function pointerToStep(track, event) {
    if (!track) return null;

    var rect = track.getBoundingClientRect();
    if (!rect || rect.width <= 0) return null;

    var x = event.clientX - rect.left;
    var ratio = clamp(x / rect.width, 0, 1);
    return ratio * TOTAL_STEPS;
  }

  function nearestStep(candidates, desired) {
    if (!candidates || candidates.length === 0) return null;

    var best = candidates[0];
    var bestDistance = Math.abs(best - desired);
    for (var i = 1; i < candidates.length; i++) {
      var candidate = candidates[i];
      var distance = Math.abs(candidate - desired);
      if (distance < bestDistance || (distance === bestDistance && candidate < best)) {
        best = candidate;
        bestDistance = distance;
      }
    }

    return best;
  }

  function percent(step) {
    return (step / TOTAL_STEPS) * 100;
  }

  function padTime(hour, minute) {
    return String(hour).padStart(2, "0") + ":" + String(minute).padStart(2, "0");
  }

  function clamp(value, min, max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

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
