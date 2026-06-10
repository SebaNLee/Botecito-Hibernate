<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="dateFieldName" required="true" %>
<%@ attribute name="offeredDates" required="false" type="java.util.List" %>
<%@ attribute name="occupiedDates" required="false" type="java.util.List" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ attribute name="restrictToAvailability" required="false" %>
<%@ attribute name="restrictDateRange" required="false" %>
<%@ attribute name="anchorTodayIso" required="false" %>
<%@ attribute name="anchorMaxDateIso" required="false" %>
<%@ attribute name="civilCalendar" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="filters.date" var="defaultDateLabel" />
<spring:message code="filters.date.placeholder" var="defaultDatePlaceholder" />
<spring:message code="datePicker.availability" var="datePickerAvailability" />
<spring:message code="datePicker.selectDate" var="datePickerSelectDate" />
<spring:message code="datePicker.previousMonth" var="datePickerPreviousMonth" />
<spring:message code="datePicker.nextMonth" var="datePickerNextMonth" />
<spring:message code="datePicker.available" var="datePickerAvailable" />
<spring:message code="datePicker.occupied" var="datePickerOccupied" />
<spring:message code="timePicker.close" var="datePickerClose" />
<spring:message code="common.clear" var="clearLabel" />
<c:set var="resolvedLabel" value="${not empty label ? label : defaultDateLabel}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedPlaceholder" value="${not empty placeholder ? placeholder : defaultDatePlaceholder}" />
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'calendar_today'}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />
<c:set var="resolvedRestrictToAvailability" value="${empty restrictToAvailability ? true : restrictToAvailability}" />
<c:set var="resolvedRestrictDateRange" value="${empty restrictDateRange ? true : restrictDateRange}" />
<c:set var="resolvedLocale" value="${not empty pageContext.response.locale.language ? pageContext.response.locale.language : 'es'}" />
<c:set var="resolvedAnchorTodayIso" value="${not empty anchorTodayIso ? anchorTodayIso : ''}" />
<c:set var="resolvedAnchorMaxDateIso" value="${not empty anchorMaxDateIso ? anchorMaxDateIso : ''}" />
<c:set var="resolvedCivilCalendar" value="${empty civilCalendar ? 'false' : civilCalendar}" />
<c:set var="pickerClearHiddenClass" value="${empty resolvedValue ? '!w-0 !min-w-0 !max-w-0 overflow-hidden !p-0 opacity-0 pointer-events-none !border-0' : ''}" />

<fieldset
    class="fieldset min-w-0 max-w-full w-full <c:out value='${resolvedContainerClass}'/>"
    data-date-picker
    data-anchor-today-iso="<c:out value='${resolvedAnchorTodayIso}'/>"
    data-anchor-max-date-iso="<c:out value='${resolvedAnchorMaxDateIso}'/>"
    data-civil-calendar="<c:out value='${resolvedCivilCalendar}'/>"
    data-placeholder="<c:out value='${resolvedPlaceholder}'/>"
    data-restrict-to-availability="<c:out value='${resolvedRestrictToAvailability}'/>"
    data-restrict-date-range="<c:out value='${resolvedRestrictDateRange}'/>"
    data-availability-label="<c:out value='${datePickerAvailability}'/>"
    data-select-date-label="<c:out value='${datePickerSelectDate}'/>"
    data-available-label="<c:out value='${datePickerAvailable}'/>"
    data-occupied-label="<c:out value='${datePickerOccupied}'/>">
  <c:forEach var="date" items="${offeredDates}">
    <span class="hidden" data-offered-date="<c:out value='${date}'/>"></span>
  </c:forEach>
  <c:forEach var="date" items="${occupiedDates}">
    <span class="hidden" data-occupied-date="<c:out value='${date}'/>"></span>
  </c:forEach>
  <input id="<c:out value='${id}'/>" name="<c:out value='${dateFieldName}'/>" type="hidden" value="<c:out value='${resolvedValue}'/>" data-picker-input />

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
          aria-controls="<c:out value='${id}'/>-panel"
          aria-haspopup="dialog">
        <span class="material-symbols-outlined shrink-0 text-primary text-xl"><c:out value="${resolvedIcon}" /></span>
        <span class="min-w-0 flex-1 text-sm font-bold leading-tight text-on-surface tabular-nums" data-picker-value><c:out value="${resolvedPlaceholder}" /></span>
      </button>
      <button
          type="button"
          class="btn btn-ghost btn-xs btn-circle shrink-0 cursor-pointer text-primary <c:out value='${pickerClearHiddenClass}'/>"
          aria-label="<c:out value='${clearLabel}'/>"
          <c:if test="${empty resolvedValue}">aria-hidden="true" tabindex="-1"</c:if>
          data-picker-trigger-clear>
        <span class="material-symbols-outlined text-base">close</span>
      </button>
      <span
          role="button"
          tabindex="0"
          class="inline-flex shrink-0 cursor-pointer items-center justify-center rounded-md p-1 text-primary transition-transform duration-150 outline-none hover:bg-base-200/50 focus-visible:bg-base-200/50 focus-visible:ring-2 focus-visible:ring-primary/25"
          data-picker-chevron
          aria-label="<c:out value='${datePickerSelectDate}'/>">
        <span class="material-symbols-outlined text-base leading-none">expand_more</span>
      </span>
    </div>

  <div
      id="<c:out value='${id}'/>-panel"
      class="card bg-base-100 fixed z-[240] hidden flex-col overflow-y-auto overflow-x-hidden px-1 pt-0.5 pb-1 shadow-xl"
      data-panel-width="328"
      data-picker-panel
      hidden>
    <div class="relative mx-auto w-fit max-w-full min-w-0" data-picker-calendar-wrap>
      <button
          type="button"
          class="btn btn-outline btn-xs absolute left-0 top-[0.45rem] z-10 gap-0.5 px-1.5 font-semibold shadow-sm"
          data-picker-clear>
        <span class="material-symbols-outlined text-sm">ink_eraser</span>
        <c:out value="${clearLabel}" />
      </button>
      <button
          type="button"
          class="btn btn-ghost btn-xs btn-circle absolute right-0 top-[0.45rem] z-10 text-on-surface-variant shadow-sm"
          data-picker-close
          aria-label="<c:out value='${datePickerClose}'/>">
        <span class="material-symbols-outlined text-base">close</span>
      </button>
      <calendar-date
          class="cally w-fit max-w-full"
          locale="<c:out value='${resolvedLocale}'/>"
          first-day-of-week="1"
          format-weekday="short"
          page-by="single"
          show-outside-days
          data-picker-calendar>
        <svg
            slot="previous"
            aria-label="<c:out value='${datePickerPreviousMonth}'/>"
            class="size-4 fill-none stroke-current stroke-[1.8]"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
        </svg>
        <svg
            slot="next"
            aria-label="<c:out value='${datePickerNextMonth}'/>"
            class="size-4 fill-none stroke-current stroke-[1.8]"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
        </svg>
        <calendar-month></calendar-month>
      </calendar-date>
    </div>
  </div>
  </div>
</fieldset>
