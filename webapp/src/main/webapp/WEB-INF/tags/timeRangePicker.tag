<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="dateInputId" required="true" %>
<%@ attribute name="startName" required="true" %>
<%@ attribute name="endName" required="true" %>
<%@ attribute name="offeredTimesJson" required="true" %>
<%@ attribute name="occupiedTimesJson" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="startValue" required="false" %>
<%@ attribute name="endValue" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ attribute name="restrictToAvailability" required="false" %>
<%@ attribute name="minimumDurationMinutes" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="filters.time" var="defaultTimeLabel" />
<spring:message code="filters.time.placeholder" var="defaultTimePlaceholder" />
<spring:message code="timePicker.availability" var="timePickerAvailability" />
<spring:message code="timePicker.selectTime" var="timePickerSelectTime" />
<spring:message code="timePicker.close" var="timePickerClose" />
<spring:message code="timePicker.available" var="timePickerAvailable" />
<spring:message code="timePicker.unavailable" var="timePickerUnavailable" />
<spring:message code="timePicker.selected" var="timePickerSelected" />
<spring:message code="timePicker.occupied" var="timePickerOccupied" />
<spring:message code="timePicker.pickDateFirst" var="timePickerPickDateFirst" />
<spring:message code="timePicker.noTimes" var="timePickerNoTimes" />
<spring:message code="timePicker.pickEnd" var="timePickerPickEnd" />
<spring:message code="timePicker.pickStart" var="timePickerPickStart" />
<spring:message code="timePicker.minimumDuration" var="timePickerMinimumDuration" />
<spring:message code="timePicker.from" var="timePickerFrom" />
<spring:message code="timePicker.apply" var="timePickerApply" />
<spring:message code="common.clear" var="clearLabel" />
<c:set var="resolvedLabel" value="${not empty label ? label : defaultTimeLabel}" />
<c:set var="resolvedStartValue" value="${not empty startValue ? startValue : ''}" />
<c:set var="resolvedEndValue" value="${not empty endValue ? endValue : ''}" />
<c:set var="resolvedPlaceholder" value="${not empty placeholder ? placeholder : defaultTimePlaceholder}" />
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'schedule'}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />
<c:set var="resolvedRestrictToAvailability" value="${empty restrictToAvailability ? true : restrictToAvailability}" />
<c:set var="resolvedMinimumDurationMinutes" value="${empty minimumDurationMinutes ? 120 : minimumDurationMinutes}" />

<fieldset
    class="fieldset min-w-0 max-w-full w-full ${resolvedContainerClass}"
    data-time-range-picker
    data-date-input-id="${fn:escapeXml(dateInputId)}"
    data-placeholder="${fn:escapeXml(resolvedPlaceholder)}"
    data-restrict-to-availability="${resolvedRestrictToAvailability}"
    data-minimum-duration-minutes="${resolvedMinimumDurationMinutes}"
    data-offered-times='${fn:escapeXml(offeredTimesJson)}'
    data-occupied-times='${fn:escapeXml(occupiedTimesJson)}'
    data-availability-label="${fn:escapeXml(timePickerAvailability)}"
    data-select-time-label="${fn:escapeXml(timePickerSelectTime)}"
    data-available-label="${fn:escapeXml(timePickerAvailable)}"
    data-unavailable-label="${fn:escapeXml(timePickerUnavailable)}"
    data-selected-label="${fn:escapeXml(timePickerSelected)}"
    data-occupied-label="${fn:escapeXml(timePickerOccupied)}"
    data-pick-date-first-label="${fn:escapeXml(timePickerPickDateFirst)}"
    data-no-times-label="${fn:escapeXml(timePickerNoTimes)}"
    data-pick-end-label="${fn:escapeXml(timePickerPickEnd)}"
    data-pick-start-label="${fn:escapeXml(timePickerPickStart)}"
    data-minimum-duration-label="${fn:escapeXml(timePickerMinimumDuration)}"
    data-from-label="${fn:escapeXml(timePickerFrom)}">
  <input id="${id}-start" name="${startName}" type="hidden" value="${fn:escapeXml(resolvedStartValue)}" data-time-start-input />
  <input id="${id}-end" name="${endName}" type="hidden" value="${fn:escapeXml(resolvedEndValue)}" data-time-end-input />

  <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
    <c:out value="${resolvedLabel}" />
  </legend>
  <div class="relative min-w-0 max-w-full">
    <div
        data-picker-control-row
        class="input cursor-pointer flex h-10 min-h-10 max-h-10 w-full min-w-0 max-w-full items-center gap-1 overflow-x-hidden border-primary/25 px-2 py-0 focus-within:border-primary has-[:focus-visible]:border-primary">
      <button
          type="button"
          class="flex min-h-0 min-w-0 flex-1 items-center gap-2 bg-transparent py-0 pl-0.5 pr-0 text-left text-on-surface outline-none cursor-pointer"
          data-picker-trigger
          aria-expanded="false"
          aria-controls="${id}-panel"
          aria-haspopup="dialog">
        <span class="material-symbols-outlined shrink-0 text-primary text-xl"><c:out value="${resolvedIcon}" /></span>
        <span class="min-w-0 flex-1 truncate text-[0.95rem] font-bold leading-tight text-on-surface" data-time-value><c:out value="${resolvedPlaceholder}" /></span>
      </button>
      <button
          type="button"
          class="btn btn-ghost btn-xs btn-circle shrink-0 cursor-pointer text-primary ${empty resolvedStartValue && empty resolvedEndValue ? '!w-0 !min-w-0 !max-w-0 overflow-hidden !p-0 opacity-0 pointer-events-none !border-0' : ''}"
          aria-label="${fn:escapeXml(clearLabel)}"
          <c:if test="${empty resolvedStartValue && empty resolvedEndValue}">aria-hidden="true" tabindex="-1"</c:if>
          data-picker-trigger-clear>
        <span class="material-symbols-outlined text-base">close</span>
      </button>
      <span
          role="button"
          tabindex="0"
          class="inline-flex shrink-0 cursor-pointer items-center justify-center rounded-md p-1 text-primary transition-transform duration-150 outline-none hover:bg-base-200/50 focus-visible:bg-base-200/50 focus-visible:ring-2 focus-visible:ring-primary/25"
          data-picker-chevron
          aria-label="${fn:escapeXml(timePickerSelectTime)}">
        <span class="material-symbols-outlined text-base leading-none">expand_more</span>
      </span>
    </div>

    <div
        id="${id}-panel"
        class="card bg-base-100 fixed z-[240] hidden flex-col overflow-y-auto overflow-x-hidden px-1 pt-0.5 pb-1 shadow-xl"
        data-panel-width="360"
        data-picker-panel
        hidden>
      <div
          class="mb-1.5 grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] items-start gap-x-4 gap-y-1 border-b border-outline-variant/20 px-0.5 pb-1.5 text-[10px] font-bold uppercase tracking-[0.12em] text-on-surface-variant">
        <div class="flex min-w-0 flex-col gap-1">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2.5 w-2.5 shrink-0 rounded-full bg-primary/15 ring-1 ring-primary/35"></span>
            <c:out value="${timePickerAvailable}" />
          </span>
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2.5 w-2.5 shrink-0 rounded-full bg-base-200 ring-1 ring-base-300/40"></span>
            <c:out value="${timePickerUnavailable}" />
          </span>
        </div>
        <div class="flex min-w-0 flex-col gap-1">
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2.5 w-2.5 shrink-0 rounded-full bg-primary ring-1 ring-primary/30"></span>
            <c:out value="${timePickerSelected}" />
          </span>
          <span class="inline-flex items-center gap-1.5">
            <span class="inline-block h-2.5 w-2.5 shrink-0 rounded-full bg-error/15 ring-1 ring-error/35"></span>
            <c:out value="${timePickerOccupied}" />
          </span>
        </div>
        <div class="flex justify-end self-start">
          <button
              type="button"
              class="btn btn-ghost btn-xs btn-circle shrink-0 text-on-surface-variant"
              data-picker-close
              aria-label="${fn:escapeXml(timePickerClose)}">
            <span class="material-symbols-outlined text-base">close</span>
          </button>
        </div>
      </div>

      <div class="mb-1.5 flex flex-wrap items-baseline justify-between gap-x-2 gap-y-0.5 px-0.5">
        <span class="min-w-0 flex-1 text-[11px] font-semibold normal-case tracking-normal leading-snug text-on-surface-variant" data-time-helper>
          <c:out value="${timePickerPickDateFirst}" />
        </span>
        <span class="max-w-[48%] shrink-0 text-right text-[10px] font-medium normal-case tracking-normal leading-snug text-outline">
          <c:out value="${timePickerMinimumDuration}" />
        </span>
      </div>

      <div
          class="min-h-0 max-h-[min(18rem,50vh)] flex-1 grid grid-cols-3 gap-1.5 overflow-y-auto overflow-x-hidden overscroll-y-contain px-0.5 sm:grid-cols-4 [scrollbar-gutter:stable]"
          data-time-slots
          data-picker-scroll-region></div>

      <div class="mt-1.5 grid grid-cols-2 gap-2 border-t border-outline-variant/20 px-0.5 pt-1.5">
        <button
            type="button"
            class="btn btn-outline btn-sm gap-1 font-semibold"
            data-picker-clear>
          <span class="material-symbols-outlined text-base">ink_eraser</span>
          <c:out value="${clearLabel}" />
        </button>
        <button type="button" class="btn btn-primary btn-sm gap-1.5 font-semibold" data-time-apply>
          <c:out value="${timePickerApply}" />
          <span class="material-symbols-outlined text-base">arrow_forward</span>
        </button>
      </div>
    </div>
  </div>
</fieldset>
