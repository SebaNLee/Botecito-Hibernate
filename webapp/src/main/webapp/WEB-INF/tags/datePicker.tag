<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="offeredDatesJson" required="true" %>
<%@ attribute name="occupiedDatesJson" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ attribute name="restrictToAvailability" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="filters.date" var="defaultDateLabel" />
<spring:message code="filters.date.placeholder" var="defaultDatePlaceholder" />
<spring:message code="datePicker.availability" var="datePickerAvailability" />
<spring:message code="datePicker.selectDate" var="datePickerSelectDate" />
<spring:message code="datePicker.previousMonth" var="datePickerPreviousMonth" />
<spring:message code="datePicker.nextMonth" var="datePickerNextMonth" />
<spring:message code="datePicker.close" var="datePickerClose" />
<spring:message code="datePicker.available" var="datePickerAvailable" />
<spring:message code="datePicker.occupied" var="datePickerOccupied" />
<spring:message code="common.clear" var="clearLabel" />
<c:set var="resolvedLabel" value="${not empty label ? label : defaultDateLabel}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedPlaceholder" value="${not empty placeholder ? placeholder : defaultDatePlaceholder}" />
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'calendar_today'}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />
<c:set var="resolvedRestrictToAvailability" value="${empty restrictToAvailability ? true : restrictToAvailability}" />

<div
    class="relative w-full ${resolvedContainerClass}"
    data-date-picker
    data-placeholder="${fn:escapeXml(resolvedPlaceholder)}"
    data-restrict-to-availability="${resolvedRestrictToAvailability}"
    data-offered-dates='${fn:escapeXml(offeredDatesJson)}'
    data-occupied-dates='${fn:escapeXml(occupiedDatesJson)}'
    data-availability-label="${fn:escapeXml(datePickerAvailability)}"
    data-select-date-label="${fn:escapeXml(datePickerSelectDate)}"
    data-available-label="${fn:escapeXml(datePickerAvailable)}"
    data-occupied-label="${fn:escapeXml(datePickerOccupied)}">
  <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-picker-input />

  <button
      type="button"
      class="w-full flex items-center gap-3 rounded-2xl border-0 bg-transparent px-0 py-0.5 text-left text-on-surface cursor-pointer hover:bg-base-200/60 transition-colors"
      data-picker-trigger
      aria-expanded="false"
      aria-controls="${id}-panel">
    <span class="material-symbols-outlined text-primary"><c:out value="${resolvedIcon}" /></span>
    <span class="min-w-0 flex-1 flex flex-col gap-0.5">
      <span class="text-[10px] font-extrabold tracking-[0.16em] uppercase text-outline"><c:out value="${resolvedLabel}" /></span>
      <span class="text-[0.95rem] leading-[1.35] font-bold text-on-surface" data-picker-value><c:out value="${resolvedPlaceholder}" /></span>
    </span>
    <span class="material-symbols-outlined text-primary">expand_more</span>
  </button>

  <div
      id="${id}-panel"
      class="card bg-base-100 fixed z-[240] hidden flex-col overflow-hidden p-3 shadow-xl"
      data-panel-width="352"
      data-picker-panel
      hidden>
    <div class="mb-2 flex items-start justify-between gap-3">
      <div>
        <div class="text-[11px] font-extrabold tracking-[0.18em] uppercase text-primary"><c:out value="${datePickerAvailability}" /></div>
        <h3 class="mt-0.5 font-headline text-lg font-extrabold text-on-background" data-picker-month-title><c:out value="${datePickerSelectDate}" /></h3>
      </div>
      <div class="flex items-center gap-1">
        <button type="button" class="btn btn-ghost btn-xs btn-circle" data-picker-nav="prev" aria-label="${fn:escapeXml(datePickerPreviousMonth)}">
          <span class="material-symbols-outlined text-base">chevron_left</span>
        </button>
        <button type="button" class="btn btn-ghost btn-xs btn-circle" data-picker-nav="next" aria-label="${fn:escapeXml(datePickerNextMonth)}">
          <span class="material-symbols-outlined text-base">chevron_right</span>
        </button>
        <button type="button" class="btn btn-ghost btn-xs btn-circle" data-picker-close aria-label="${fn:escapeXml(datePickerClose)}">
          <span class="material-symbols-outlined text-base">close</span>
        </button>
      </div>
    </div>

    <div class="mb-2 flex flex-wrap items-center gap-3 text-[10px] font-bold text-on-surface-variant">
      <span class="inline-flex items-center gap-2">
        <span class="inline-block h-3 w-3 rounded-full bg-primary/15 border border-primary/40"></span>
        <c:out value="${datePickerAvailable}" />
      </span>
      <span class="inline-flex items-center gap-2">
        <span class="inline-block h-3 w-3 rounded-full bg-error/15 border border-error/30"></span>
        <c:out value="${datePickerOccupied}" />
      </span>
    </div>

    <div class="hide-scrollbar min-h-0 flex-1 overflow-y-auto pr-1" data-picker-months data-picker-scroll-region></div>

    <div class="mt-2 flex justify-end">
      <button type="button" class="btn btn-ghost btn-xs gap-1 text-on-surface-variant uppercase tracking-[0.12em]" data-picker-clear>
        <span class="material-symbols-outlined text-base">ink_eraser</span>
        <c:out value="${clearLabel}" />
      </button>
    </div>
  </div>
</div>
