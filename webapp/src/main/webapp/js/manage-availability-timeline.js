(function () {
  "use strict";

  var MIN_DURATION_MINUTES = 120;
  var MIN_SEPARATION_MINUTES = 30;
  var SLOT_STEP_MINUTES = 30;
  var TOTAL_MINUTES = 23 * 60 + 30;
  var TOTAL_STEPS = TOTAL_MINUTES / SLOT_STEP_MINUTES;
  var DAY_END_DISPLAY_LABEL = "23:30h";

  function ManageAvailabilityTimeline(root) {
    this.root = root;
    this.track = root.querySelector("[data-timeline-track]");
    this.hoverZone = root.querySelector("[data-timeline-hover-zone]");
    this.availableLayer = root.querySelector("[data-timeline-available]");
    this.bookedLayer = root.querySelector("[data-timeline-booked]");
    this.blocksLayer = root.querySelector("[data-timeline-blocks]");
    this.preview = root.querySelector("[data-timeline-preview]");
    this.previewLabel = root.querySelector("[data-timeline-preview-label]");
    this.blockTemplate = root.querySelector("[data-availability-block-template]");
    this.saveForm = root.querySelector("[data-timeline-save-form]");
    this.saveButton = this.saveForm ? this.saveForm.querySelector('button[type="submit"]') : null;
    this.minDuration = parseInt(root.dataset.minDuration || MIN_DURATION_MINUTES, 10);
    this.minSteps = Math.max(1, Math.ceil(this.minDuration / SLOT_STEP_MINUTES));
    this.minSeparationSteps = Math.max(
      1,
      Math.ceil(
        parseInt(root.dataset.minSeparation || MIN_SEPARATION_MINUTES, 10) / SLOT_STEP_MINUTES,
      ),
    );
    this.deleteText = root.dataset.deleteText || "Delete";
    this.unsavedConfirmText =
      root.dataset.unsavedConfirm || "You have unsaved changes. Leave this date without saving?";
    this.selectedDate = root.dataset.selectedDate || "";
    this.previewStart = null;
    this.dragState = null;
    this.saving = false;
    this.nextTempId = -1;
    this.deletedIds = {};

    var timelineJsonEl = document.getElementById("manage-day-timeline-json");
    var timeline = parseTimeline(
      timelineJsonEl ? timelineJsonEl.textContent : root.dataset.timeline,
    );
    this.availableRanges = timeline.availableRanges;
    this.bookedRanges = timeline.bookedRanges;
    this.selfBlocks = timeline.selfBlocks.map(function (block) {
      return {
        id: block.id,
        start: parseTimeToStep(block.startTime, false),
        end: parseTimeToStep(block.endTime, true),
      };
    }).filter(function (block) {
      return block.start !== null && block.end !== null && block.end > block.start;
    });
    this.initialSelfBlocks = this.selfBlocks.map(function (block) {
      return { id: block.id, start: block.start, end: block.end };
    });

    this.applyTimelineTickPositions();
    this.renderBackgroundLayers();
    this.bindTrackEvents();
    this.bindGlobalDragging();
    this.bindSaveForm();
    this.bindUnsavedNavigationGuard();
    this.renderSelfBlocks();
    this.updateSaveButtonState();
  }

  ManageAvailabilityTimeline.prototype.bindTrackEvents = function () {
    var self = this;
    if (!this.track) {
      return;
    }

    this.track.addEventListener("pointermove", function (event) {
      if (self.dragState || self.saving) {
        return;
      }
      self.updatePreviewFromPointer(event);
    });

    this.track.addEventListener("pointerleave", function () {
      if (self.dragState || self.saving) {
        return;
      }
      self.clearPreview();
      self.setTrackCursor("");
    });

    this.track.addEventListener("pointerdown", function (event) {
      if (event.button !== 0 || self.dragState || self.saving) {
        return;
      }
      if (event.target.closest("[data-block-id]")) {
        return;
      }
      self.insertFromPointer(event);
    });
  };

  ManageAvailabilityTimeline.prototype.bindGlobalDragging = function () {
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
    });
  };

  ManageAvailabilityTimeline.prototype.renderBackgroundLayers = function () {
    this.renderRangeLayer(this.availableLayer, this.availableRanges, "bg-primary/20 border border-primary/30");
    this.renderRangeLayer(this.bookedLayer, this.bookedRanges, "bg-error/25 border border-error/35");
  };

  ManageAvailabilityTimeline.prototype.renderRangeLayer = function (layer, ranges, className) {
    if (!layer) {
      return;
    }
    layer.innerHTML = "";
    for (var i = 0; i < ranges.length; i++) {
      var range = ranges[i];
      var start = parseTimeToStep(range.startTime, false);
      var end = parseTimeToStep(range.endTime, true);
      if (start === null || end === null || end <= start) {
        continue;
      }
      var segment = document.createElement("div");
      segment.className = "absolute top-0 h-8 rounded-md pointer-events-none " + className;
      segment.style.left = percent(start) + "%";
      segment.style.width = percent(end - start) + "%";
      layer.appendChild(segment);
    }
  };

  ManageAvailabilityTimeline.prototype.renderSelfBlocks = function () {
    var self = this;
    if (!this.blocksLayer || !this.blockTemplate || !this.blockTemplate.content) {
      return;
    }

    this.blocksLayer.innerHTML = "";
    var sorted = this.selfBlocks.slice().sort(function (a, b) {
      return a.start - b.start;
    });

    for (var i = 0; i < sorted.length; i++) {
      var range = sorted[i];
      var durationSteps = range.end - range.start;
      var blockFragment = this.blockTemplate.content.cloneNode(true);
      var block = blockFragment.querySelector('[data-role="availability-block"]');
      var leftHandle = blockFragment.querySelector('[data-role="left-handle"]');
      var rightHandle = blockFragment.querySelector('[data-role="right-handle"]');
      var leftPill = blockFragment.querySelector('[data-role="left-label"]');
      var rightPill = blockFragment.querySelector('[data-role="right-label"]');
      var deleteButton = blockFragment.querySelector('[data-role="delete-button"]');

      if (!block || !leftHandle || !rightHandle || !leftPill || !rightPill || !deleteButton) {
        continue;
      }

      block.style.left = percent(range.start) + "%";
      block.style.width = percent(range.end - range.start) + "%";
      block.setAttribute("data-block-id", String(range.id));
      block.classList.remove("from-secondary", "to-secondary-container", "text-on-secondary");
      block.classList.add(
        "bg-gradient-to-r",
        "from-warning",
        "to-warning/80",
        "text-warning-content",
        "shadow-[0_8px_16px_rgba(234,179,8,0.28)]",
      );

      leftHandle.setAttribute("aria-label", "Resize start");
      leftPill.textContent = formatBoundaryLabel(range.start, false);
      rightHandle.setAttribute("aria-label", "Resize end");
      rightPill.textContent = formatBoundaryLabel(range.end, true);
      configureHandleLabelPositions(leftPill, rightPill);

      deleteButton.setAttribute("aria-label", self.deleteText);
      deleteButton.textContent = self.deleteText;

      leftHandle.addEventListener("pointerdown", self.handleResizeStart.bind(self, range.id, "left"));
      rightHandle.addEventListener("pointerdown", self.handleResizeStart.bind(self, range.id, "right"));
      deleteButton.addEventListener("click", self.handleDelete.bind(self, range.id));
      block.addEventListener("pointerdown", self.handleDragStart.bind(self, range.id));

      this.blocksLayer.appendChild(blockFragment);
    }
  };

  ManageAvailabilityTimeline.prototype.updatePreviewFromPointer = function (event) {
    if (!this.track) {
      this.clearPreview();
      return;
    }

    if (event.target && event.target.closest("[data-block-id]")) {
      this.clearPreview();
      this.setTrackCursor("");
      return;
    }

    if (!this.pointerInHoverZone(event)) {
      this.clearPreview();
      this.setTrackCursor("");
      return;
    }

    var pointerStep = pointerToStep(this.track, event);
    if (pointerStep === null) {
      this.clearPreview();
      this.setTrackCursor("");
      return;
    }

    if (this.isPointerInBlockedGap(pointerStep)) {
      this.clearPreview();
      this.setTrackCursor("not-allowed");
      return;
    }

    this.setTrackCursor("");
    var desiredStart = Math.round(pointerStep - this.minSteps / 2);
    var snappedStart = nearestStep(this.validInsertStarts(), desiredStart);
    if (snappedStart === null) {
      this.clearPreview();
      return;
    }

    this.previewStart = snappedStart;
    this.renderPreview();
  };

  ManageAvailabilityTimeline.prototype.insertFromPointer = function (event) {
    this.updatePreviewFromPointer(event);
    if (this.previewStart === null) {
      return;
    }

    var start = this.previewStart;
    var end = start + this.minSteps;
    if (!this.canPlaceRange(start, end, null)) {
      this.clearPreview();
      return;
    }

    this.selfBlocks.push({
      id: this.nextTempId,
      start: start,
      end: end,
    });
    this.nextTempId -= 1;
    this.clearPreview();
    this.renderSelfBlocks();
    this.updateSaveButtonState();
    event.preventDefault();
  };

  ManageAvailabilityTimeline.prototype.handleDelete = function (blockId, event) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    if (this.saving) {
      return;
    }
    if (blockId > 0) {
      this.deletedIds[blockId] = true;
    }
    this.selfBlocks = this.selfBlocks.filter(function (block) {
      return block.id !== blockId;
    });
    this.renderSelfBlocks();
    this.updateSaveButtonState();
  };

  ManageAvailabilityTimeline.prototype.handleDragStart = function (blockId, event) {
    if (event.button !== 0 || this.saving) {
      return;
    }
    if (event.target.closest("button")) {
      return;
    }

    var range = this.findBlockById(blockId);
    if (!range || !this.track) {
      return;
    }

    var pointerStep = pointerToStep(this.track, event);
    if (pointerStep === null) {
      return;
    }

    this.dragState = {
      type: "move",
      id: blockId,
      pointerId: event.pointerId,
      duration: range.end - range.start,
      offset: pointerStep - range.start,
      initialStart: range.start,
      initialEnd: range.end,
    };

    document.body.classList.add("select-none");
    event.preventDefault();
    event.stopPropagation();
  };

  ManageAvailabilityTimeline.prototype.handleResizeStart = function (blockId, side, event) {
    if (event.button !== 0 || this.saving) {
      return;
    }

    var range = this.findBlockById(blockId);
    if (!range) {
      return;
    }

    this.dragState = {
      type: side === "left" ? "resize-left" : "resize-right",
      id: blockId,
      pointerId: event.pointerId,
      initialStart: range.start,
      initialEnd: range.end,
    };

    document.body.classList.add("select-none");
    event.preventDefault();
    event.stopPropagation();
  };

  ManageAvailabilityTimeline.prototype.handleGlobalPointerMove = function (event) {
    if (!this.dragState || !this.track) {
      return;
    }
    if (this.dragState.pointerId !== undefined && event.pointerId !== this.dragState.pointerId) {
      return;
    }

    var range = this.findBlockById(this.dragState.id);
    if (!range) {
      return;
    }

    var pointerStep = pointerToStep(this.track, event);
    if (pointerStep === null) {
      return;
    }

    if (this.dragState.type === "move") {
      this.applyMoveDrag(this.dragState, range, pointerStep);
    } else if (this.dragState.type === "resize-left") {
      this.applyLeftResizeDrag(range, pointerStep);
    } else if (this.dragState.type === "resize-right") {
      this.applyRightResizeDrag(range, pointerStep);
    }

    this.renderSelfBlocks();
    event.preventDefault();
  };

  ManageAvailabilityTimeline.prototype.handleGlobalPointerUp = function (event) {
    if (!this.dragState) {
      return;
    }
    if (this.dragState.pointerId !== undefined && event.pointerId !== this.dragState.pointerId) {
      return;
    }

    var state = this.dragState;
    var range = this.findBlockById(state.id);
    this.dragState = null;
    document.body.classList.remove("select-none");

    if (!range || this.saving) {
      this.renderSelfBlocks();
      return;
    }

    if (range.start === state.initialStart && range.end === state.initialEnd) {
      return;
    }

    this.updateSaveButtonState();
  };

  ManageAvailabilityTimeline.prototype.applyMoveDrag = function (state, range, pointerStep) {
    var duration = state.duration;
    var starts = [];
    for (var start = 0; start <= TOTAL_STEPS - duration; start++) {
      if (this.canPlaceRange(start, start + duration, state.id)) {
        starts.push(start);
      }
    }
    var snappedStart = nearestStep(starts, Math.round(pointerStep - state.offset));
    if (snappedStart === null) {
      return;
    }
    range.start = snappedStart;
    range.end = snappedStart + duration;
  };

  ManageAvailabilityTimeline.prototype.applyLeftResizeDrag = function (range, pointerStep) {
    var maxStart = range.end - this.minSteps;
    var starts = [];
    for (var start = 0; start <= maxStart; start++) {
      if (this.canPlaceRange(start, range.end, range.id)) {
        starts.push(start);
      }
    }
    var snappedStart = nearestStep(starts, Math.round(pointerStep));
    if (snappedStart === null) {
      return;
    }
    range.start = snappedStart;
  };

  ManageAvailabilityTimeline.prototype.applyRightResizeDrag = function (range, pointerStep) {
    var minEnd = range.start + this.minSteps;
    var ends = [];
    for (var end = minEnd; end <= TOTAL_STEPS; end++) {
      if (this.canPlaceRange(range.start, end, range.id)) {
        ends.push(end);
      }
    }
    var snappedEnd = nearestStep(ends, Math.round(pointerStep));
    if (snappedEnd === null) {
      return;
    }
    range.end = snappedEnd;
  };

  ManageAvailabilityTimeline.prototype.bindSaveForm = function () {
    var self = this;
    if (!this.saveForm) {
      return;
    }
    this.saveForm.addEventListener("submit", function (event) {
      if (!self.hasUnsavedChanges()) {
        event.preventDefault();
        return;
      }
      self.saving = true;
      self.updateSaveButtonState();
      var changesInput = self.saveForm.querySelector('[name="changesJson"]');
      if (changesInput) {
        changesInput.value = JSON.stringify(self.buildChangesPayload());
      }
    });
  };

  ManageAvailabilityTimeline.prototype.bindUnsavedNavigationGuard = function () {
    var self = this;
    var dateForm = document.querySelector("[data-manage-availability-date-form]");
    if (dateForm) {
      dateForm.addEventListener("submit", function (event) {
        if (self.hasUnsavedChanges()) {
          var confirmed = window.confirm(self.unsavedConfirmText);
          if (!confirmed) {
            event.preventDefault();
          }
        }
      });
    }
    window.addEventListener("beforeunload", function (event) {
      if (self.hasUnsavedChanges()) {
        event.preventDefault();
        event.returnValue = "";
      }
    });
  };

  ManageAvailabilityTimeline.prototype.buildChangesPayload = function () {
    var creates = [];
    var updates = [];
    var deletes = [];
    var initialById = {};
    var i;

    for (i = 0; i < this.initialSelfBlocks.length; i++) {
      initialById[this.initialSelfBlocks[i].id] = this.initialSelfBlocks[i];
    }

    for (var deletedId in this.deletedIds) {
      if (Object.prototype.hasOwnProperty.call(this.deletedIds, deletedId)) {
        deletes.push(parseInt(deletedId, 10));
      }
    }

    for (i = 0; i < this.selfBlocks.length; i++) {
      var block = this.selfBlocks[i];
      var startTime = serializeStep(block.start);
      var endTime = serializeStep(block.end);
      if (block.id < 0) {
        creates.push({ startTime: startTime, endTime: endTime });
      } else {
        var initial = initialById[block.id];
        if (!initial || initial.start !== block.start || initial.end !== block.end) {
          updates.push({ id: block.id, startTime: startTime, endTime: endTime });
        }
      }
    }

    var blocks = this.selfBlocks.map(function (block) {
      return {
        startTime: serializeStep(block.start),
        endTime: serializeStep(block.end),
      };
    });

    return { deletes: deletes, creates: creates, updates: updates, blocks: blocks };
  };

  ManageAvailabilityTimeline.prototype.hasUnsavedChanges = function () {
    var payload = this.buildChangesPayload();
    return payload.deletes.length > 0 || payload.creates.length > 0 || payload.updates.length > 0;
  };

  ManageAvailabilityTimeline.prototype.updateSaveButtonState = function () {
    if (!this.saveButton) {
      return;
    }
    var dirty = this.hasUnsavedChanges();
    this.saveButton.disabled = this.saving || !dirty;
  };

  ManageAvailabilityTimeline.prototype.canPlaceRange = function (start, end, ignoreId) {
    if (start < 0 || end > TOTAL_STEPS || end <= start || end - start < this.minSteps) {
      return false;
    }
    if (!this.rangeWithinAvailable(start, end)) {
      return false;
    }
    if (this.overlapsBookedRange(start, end)) {
      return false;
    }
    for (var i = 0; i < this.selfBlocks.length; i++) {
      var block = this.selfBlocks[i];
      if (ignoreId !== null && block.id === ignoreId) {
        continue;
      }
      if (!(end <= block.start - this.minSeparationSteps || start >= block.end + this.minSeparationSteps)) {
        return false;
      }
    }
    return true;
  };

  ManageAvailabilityTimeline.prototype.rangeWithinAvailable = function (start, end) {
    if (this.availableRanges.length === 0) {
      return false;
    }
    for (var step = start; step < end; step++) {
      if (!this.stepWithinAvailable(step)) {
        return false;
      }
    }
    return true;
  };

  ManageAvailabilityTimeline.prototype.stepWithinAvailable = function (step) {
    for (var i = 0; i < this.availableRanges.length; i++) {
      var range = this.availableRanges[i];
      var rangeStart = parseTimeToStep(range.startTime, false);
      var rangeEnd = parseTimeToStep(range.endTime, true);
      if (rangeStart === null || rangeEnd === null) {
        continue;
      }
      if (step >= rangeStart && step < rangeEnd) {
        return true;
      }
    }
    return false;
  };

  ManageAvailabilityTimeline.prototype.overlapsBookedRange = function (start, end) {
    for (var i = 0; i < this.bookedRanges.length; i++) {
      var range = this.bookedRanges[i];
      var rangeStart = parseTimeToStep(range.startTime, false);
      var rangeEnd = parseTimeToStep(range.endTime, true);
      if (rangeStart === null || rangeEnd === null) {
        continue;
      }
      if (!(end <= rangeStart || start >= rangeEnd)) {
        return true;
      }
    }
    return false;
  };

  ManageAvailabilityTimeline.prototype.validInsertStarts = function () {
    var starts = [];
    for (var start = 0; start <= TOTAL_STEPS - this.minSteps; start++) {
      if (this.canPlaceRange(start, start + this.minSteps, null)) {
        starts.push(start);
      }
    }
    return starts;
  };

  ManageAvailabilityTimeline.prototype.isPointerInBlockedGap = function (pointerStep) {
    if (this.selfBlocks.length === 0) {
      return false;
    }
    var sorted = this.selfBlocks.slice().sort(function (a, b) {
      return a.start - b.start;
    });
    var first = sorted[0];
    if (pointerStep >= 0 && pointerStep < first.start) {
      return this.gapCannotFitBlock(0, first.start - this.minSteps - this.minSeparationSteps);
    }
    for (var i = 1; i < sorted.length; i++) {
      var left = sorted[i - 1];
      var right = sorted[i];
      if (!(pointerStep > left.end && pointerStep < right.start)) {
        continue;
      }
      var minStart = left.end + this.minSeparationSteps;
      var maxStart = right.start - this.minSteps - this.minSeparationSteps;
      return this.gapCannotFitBlock(minStart, maxStart);
    }
    var last = sorted[sorted.length - 1];
    if (pointerStep > last.end && pointerStep <= TOTAL_STEPS) {
      return this.gapCannotFitBlock(
        last.end + this.minSeparationSteps,
        TOTAL_STEPS - this.minSteps,
      );
    }
    return false;
  };

  ManageAvailabilityTimeline.prototype.gapCannotFitBlock = function (minStart, maxStart) {
    return maxStart < minStart;
  };

  ManageAvailabilityTimeline.prototype.gapCanFitBlock = function (minStart, maxStart) {
    if (maxStart < minStart) {
      return false;
    }
    for (var start = minStart; start <= maxStart; start++) {
      if (this.canPlaceRange(start, start + this.minSteps, null)) {
        return true;
      }
    }
    return false;
  };

  ManageAvailabilityTimeline.prototype.findBlockById = function (blockId) {
    for (var i = 0; i < this.selfBlocks.length; i++) {
      if (this.selfBlocks[i].id === blockId) {
        return this.selfBlocks[i];
      }
    }
    return null;
  };

  ManageAvailabilityTimeline.prototype.renderPreview = function () {
    if (!this.preview || !this.previewLabel) {
      return;
    }
    if (this.previewStart === null || this.dragState) {
      this.preview.classList.add("hidden");
      return;
    }
    var start = this.previewStart;
    var end = start + this.minSteps;
    this.preview.style.left = percent(start) + "%";
    this.preview.style.width = percent(end - start) + "%";
    this.previewLabel.textContent = formatRangeLabel(start, end);
    this.preview.classList.remove("hidden");
  };

  ManageAvailabilityTimeline.prototype.clearPreview = function () {
    this.previewStart = null;
    this.renderPreview();
  };

  ManageAvailabilityTimeline.prototype.setTrackCursor = function (cursor) {
    if (this.track) {
      this.track.style.cursor = cursor || "";
    }
  };

  ManageAvailabilityTimeline.prototype.pointerInHoverZone = function (event) {
    if (!this.hoverZone) {
      return false;
    }
    var rect = this.hoverZone.getBoundingClientRect();
    return (
      event.clientX >= rect.left &&
      event.clientX <= rect.right &&
      event.clientY >= rect.top &&
      event.clientY <= rect.bottom
    );
  };

  ManageAvailabilityTimeline.prototype.applyTimelineTickPositions = function () {
    this.root.querySelectorAll("[data-tick-left-pct]").forEach(function (tick) {
      var left = tick.getAttribute("data-tick-left-pct");
      if (left != null && left !== "") {
        tick.style.left = left + "%";
      }
    });
  };

  function parseTimeline(raw) {
    if (!raw) {
      return { availableRanges: [], bookedRanges: [], selfBlocks: [] };
    }
    try {
      var parsed = JSON.parse(raw);
      return {
        availableRanges: parsed.availableRanges || [],
        bookedRanges: parsed.bookedRanges || [],
        selfBlocks: parsed.selfBlocks || [],
      };
    } catch (ignored) {
      return { availableRanges: [], bookedRanges: [], selfBlocks: [] };
    }
  }

  function parseTimeToStep(value, isEnd) {
    if (!value || typeof value !== "string") {
      return null;
    }
    var match = /^(\d{2}):(\d{2})$/.exec(value.trim());
    if (!match) {
      return null;
    }
    var hour = parseInt(match[1], 10);
    var minute = parseInt(match[2], 10);
    if (isEnd && hour === 23 && minute === 59) {
      return TOTAL_STEPS;
    }
    if (hour < 0 || hour > 23 || (minute !== 0 && minute !== 30)) {
      return null;
    }
    var total = hour * 60 + minute;
    var step = total / SLOT_STEP_MINUTES;
    return Math.floor(step) === step ? step : null;
  }

  function serializeStep(step) {
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
    return serializeStep(step) + "h";
  }

  function configureHandleLabelPositions(leftPill, rightPill) {
    leftPill.style.marginLeft = "15px";
    rightPill.style.marginLeft = "-15px";
  }

  function pointerToStep(track, event) {
    var rect = track.getBoundingClientRect();
    if (!rect || rect.width <= 0) {
      return null;
    }
    var ratio = clamp((event.clientX - rect.left) / rect.width, 0, 1);
    return ratio * TOTAL_STEPS;
  }

  function nearestStep(candidates, desired) {
    if (!candidates || candidates.length === 0) {
      return null;
    }
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
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-manage-availability-timeline]").forEach(function (root) {
      new ManageAvailabilityTimeline(root);
    });
  });
})();
