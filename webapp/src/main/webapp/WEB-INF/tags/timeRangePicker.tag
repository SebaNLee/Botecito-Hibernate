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

<div
    class="relative w-full ${resolvedContainerClass}"
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

  <button
      type="button"
      class="w-full flex items-center gap-3 rounded-2xl border-0 bg-transparent px-0 py-0.5 text-left text-on-surface cursor-pointer"
      data-picker-trigger
      aria-expanded="false"
      aria-controls="${id}-panel">
    <span class="material-symbols-outlined text-primary"><c:out value="${resolvedIcon}" /></span>
    <span class="min-w-0 flex-1 flex flex-col gap-0.5">
      <span class="text-[10px] font-extrabold tracking-[0.16em] uppercase text-outline"><c:out value="${resolvedLabel}" /></span>
      <span class="text-[0.95rem] leading-[1.35] font-bold text-on-surface" data-time-value><c:out value="${resolvedPlaceholder}" /></span>
    </span>
    <span class="material-symbols-outlined text-primary">expand_more</span>
  </button>

  <div
      id="${id}-panel"
      class="fixed z-[240] hidden flex-col overflow-hidden rounded-[1.25rem] border border-outline-variant/45 bg-white/95 p-3 shadow-[0_24px_64px_rgba(11,28,50,0.16)] backdrop-blur-[18px]"
      data-panel-width="384"
      data-picker-panel
      hidden>
    <div class="mb-2 flex items-start justify-between gap-3">
      <div>
        <div class="text-[11px] font-extrabold tracking-[0.18em] uppercase text-primary"><c:out value="${timePickerAvailability}" /></div>
        <h3 class="mt-0.5 font-headline text-lg font-extrabold text-on-background"><c:out value="${timePickerSelectTime}" /></h3>
      </div>
      <button type="button" class="inline-flex h-7 w-7 items-center justify-center rounded-full border-0 bg-surface-container-low text-on-surface cursor-pointer" data-picker-close aria-label="${fn:escapeXml(timePickerClose)}">
        <span class="material-symbols-outlined text-[18px]">close</span>
      </button>
    </div>

    <div class="mb-2 flex flex-wrap items-center gap-3 text-[10px] font-bold text-on-surface-variant">
      <span class="inline-flex items-center gap-2">
        <span class="inline-block h-3 w-3 rounded-full border border-primary/35 bg-primary/15"></span>
        <c:out value="${timePickerAvailable}" />
      </span>
      <span class="inline-flex items-center gap-2">
        <span class="inline-block h-3 w-3 rounded-full bg-[rgba(212,227,255,0.45)]"></span>
        <c:out value="${timePickerUnavailable}" />
      </span>
      <span class="inline-flex items-center gap-2">
        <span class="inline-block h-3 w-3 rounded-full bg-primary-container"></span>
        <c:out value="${timePickerSelected}" />
      </span>
      <span class="inline-flex items-center gap-2">
        <span class="inline-block h-3 w-3 rounded-full border border-error/25 bg-error/14"></span>
        <c:out value="${timePickerOccupied}" />
      </span>
    </div>

    <div class="mb-1.5 text-[11px] font-semibold text-on-surface-variant" data-time-helper><c:out value="${timePickerPickDateFirst}" /></div>
    <div class="mb-3 text-[10px] font-medium text-outline"><c:out value="${timePickerMinimumDuration}" /></div>
    <div class="hide-scrollbar min-h-0 flex-1 grid grid-cols-4 gap-2 overflow-y-auto" data-time-slots data-picker-scroll-region></div>

    <div class="mt-2 flex items-center justify-between gap-2">
      <button type="button" class="inline-flex items-center gap-2 rounded-full border border-outline-variant bg-transparent px-3 py-1.5 text-[10px] font-extrabold uppercase tracking-[0.12em] text-on-surface cursor-pointer" data-picker-clear>
        <span class="material-symbols-outlined text-[16px]">ink_eraser</span>
        <c:out value="${clearLabel}" />
      </button>
      <button type="button" class="flex min-h-10 flex-1 items-center justify-center gap-2 rounded-full border-0 bg-gradient-to-br from-primary to-primary-container px-4 text-sm font-extrabold text-on-primary shadow-[0_16px_32px_rgba(0,93,167,0.22)] cursor-pointer" data-time-apply>
        <c:out value="${timePickerApply}" />
        <span class="material-symbols-outlined text-base">arrow_forward</span>
      </button>
    </div>
  </div>
</div>
