<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %> <%@ taglib
prefix="spring" uri="http://www.springframework.org/tags" %> <%@ taglib
prefix="paw" tagdir="/WEB-INF/tags" %> <%@ page contentType="text/html;
charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="pageUrl" value="/my-boats/${item.id}/availability" />
<c:url var="saveUrl" value="/my-boats/${item.id}/availability/save" />
<c:url var="myBoatsUrl" value="/my-boats" />

<spring:message code="manageAvailability.title" var="pageTitle" />
<spring:message
  code="manageAvailability.back.myBoats"
  var="manageAvailabilityBackMyBoatsLabel"
/>
<spring:message code="manageAvailability.subtitle" var="pageSubtitle" />
<spring:message code="manageAvailability.calendar.title" var="calendarTitle" />
<spring:message code="manageAvailability.calendar.hint" var="calendarHint" />
<spring:message
  code="manageAvailability.calendar.datePickerLabel"
  var="datePickerLabel"
/>
<spring:message code="manageAvailability.slots.title" var="slotsTitle" />
<spring:message code="manageAvailability.slots.empty" var="slotsEmpty" />
<spring:message code="manageAvailability.slots.noDate" var="slotsNoDate" />
<spring:message
  code="manageAvailability.legend.available"
  var="legendAvailable"
/>
<spring:message code="manageAvailability.legend.booked" var="legendBooked" />
<spring:message
  code="manageAvailability.legend.selfBlocked"
  var="legendSelfBlocked"
/>
<spring:message
  code="manageAvailability.timeline.instructions"
  var="timelineInstructions"
/>
<spring:message code="manageAvailability.save" var="saveChangesLabel" />
<spring:message
  code="manageAvailability.unsavedConfirm"
  var="unsavedConfirmLabel"
/>
<spring:message code="publish.step2.deleteRange" var="deleteRangeLabel" />

<spring:message
  code="page.title.manageAvailability"
  var="titleManageAvailability"
/>
<paw:layout
  title="${titleManageAvailability} - Botecito"
  mainClass="pt-24 pb-14 max-w-4xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent"
  scripts="toast,date-time,manage-availability"
>
  <paw:toastNotifier />

  <div class="mb-8">
    <a
      href="<c:out value='${myBoatsUrl}' />"
      data-nav-filter-page="myBoats"
      class="link link-hover inline-flex items-center gap-2 text-secondary font-bold font-headline no-underline w-fit"
    >
      <span class="material-symbols-outlined">arrow_back</span>
      <span><c:out value="${manageAvailabilityBackMyBoatsLabel}" /></span>
    </a>
  </div>

  <div class="mb-8">
    <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">
      <c:out value="${pageTitle}" />
    </h1>
    <p class="text-on-surface-variant mt-2 m-0 text-lg">
      <c:out value="${item.latestVersion.title}" />
    </p>
    <p class="text-on-surface-variant mt-1 m-0 text-sm">
      <c:out value="${pageSubtitle}" />
    </p>
  </div>

  <paw:sectionCard icon="calendar_month" hostAccent="true">
    <jsp:attribute name="title"
      ><c:out value="${calendarTitle}"
    /></jsp:attribute>
    <jsp:body>
      <p class="text-sm text-on-surface-variant m-0 mb-4">
        <c:out value="${calendarHint}" />
      </p>

      <form
        method="get"
        action="<c:out value='${pageUrl}' />"
        data-manage-availability-date-form
        class="mb-6"
      >
        <paw:datePicker
          id="manageAvailabilityDate"
          dateFieldName="date"
          offeredDates="${offeredDates}"
          label="${datePickerLabel}"
          value="${selectedDate}"
          anchorTodayIso="${manageAvailabilityTodayIso}"
          anchorMaxDateIso="${manageAvailabilityMaxDateIso}"
        />
        <noscript>
          <div class="mt-3">
            <paw:button
              type="submit"
              color="secondary"
              size="sm"
              text="${calendarTitle}"
            />
          </div>
        </noscript>
      </form>

      <c:choose>
        <c:when test="${empty selectedDate}">
          <p class="text-sm text-on-surface-variant m-0">
            <c:out value="${slotsNoDate}" />
          </p>
        </c:when>
        <c:otherwise>
          <div
            class="mb-4 flex flex-wrap gap-4 text-xs text-on-surface-variant"
          >
            <span class="inline-flex items-center gap-2">
              <span
                class="inline-block h-3 w-8 rounded bg-primary/20 border border-primary/30"
              ></span>
              <c:out value="${legendAvailable}" />
            </span>
            <span class="inline-flex items-center gap-2">
              <span
                class="inline-block h-3 w-8 rounded bg-error/25 border border-error/35"
              ></span>
              <c:out value="${legendBooked}" />
            </span>
            <span class="inline-flex items-center gap-2">
              <span
                class="inline-block h-3 w-8 rounded bg-gradient-to-r from-warning to-warning/80"
              ></span>
              <c:out value="${legendSelfBlocked}" />
            </span>
          </div>

          <h3 class="text-base font-bold m-0 mb-3">
            <c:out value="${slotsTitle}" /> &mdash;
            <c:out value="${selectedDate}" />
          </h3>

          <c:choose>
            <c:when test="${not hasTimelineAvailability}">
              <p class="text-sm text-on-surface-variant m-0">
                <c:out value="${slotsEmpty}" />
              </p>
            </c:when>
            <c:otherwise>
              <div
                class="space-y-4"
                data-manage-availability-timeline
                data-selected-date="<c:out value='${selectedDate}'/>"
                data-min-duration="120"
                data-min-separation="30"
                data-delete-text="<c:out value='${deleteRangeLabel}'/>"
                data-unsaved-confirm="<c:out value='${unsavedConfirmLabel}'/>"
              >
                <c:forEach var="range" items="${timelineAvailableRanges}">
                  <span
                    class="hidden"
                    data-timeline-available-range
                    data-start="<c:out value='${range.startTime}'/>"
                    data-end="<c:out value='${range.endTime}'/>"
                  ></span>
                </c:forEach>
                <c:forEach var="range" items="${timelineBookedRanges}">
                  <span
                    class="hidden"
                    data-timeline-booked-range
                    data-start="<c:out value='${range.startTime}'/>"
                    data-end="<c:out value='${range.endTime}'/>"
                  ></span>
                </c:forEach>
                <c:forEach var="block" items="${timelineSelfBlocks}">
                  <span
                    class="hidden"
                    data-timeline-self-block
                    data-id="<c:out value='${block.id}'/>"
                    data-start="<c:out value='${block.startTime}'/>"
                    data-end="<c:out value='${block.endTime}'/>"
                  ></span>
                </c:forEach>

                <p class="text-xs text-outline m-0">
                  <c:out value="${timelineInstructions}" />
                </p>

                <div class="relative pt-1 pb-8">
                  <div
                    class="absolute inset-x-0 bottom-0 flex items-center justify-between text-[11px] font-bold text-outline"
                  >
                    <span>00:00h</span>
                    <span>23:30h</span>
                  </div>
                  <div
                    class="relative h-12 rounded-xl border border-outline-variant/30 bg-surface-container-high/40 overflow-visible touch-none"
                    data-timeline-track
                  >
                    <div
                      class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 rounded-lg border border-outline-variant/25 bg-base-200/80"
                      data-timeline-hover-zone
                    ></div>
                    <div
                      class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none"
                      data-timeline-available
                    ></div>
                    <div
                      class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none"
                      data-timeline-booked
                    ></div>
                    <div
                      class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none"
                      data-timeline-ticks
                    >
                      <c:forEach var="tickHour" begin="0" end="23">
                        <c:set var="tickStep" value="${tickHour * 2}" />
                        <c:set
                          var="tickLeft"
                          value="${(tickStep * 100.0) / 47}"
                        />
                        <c:set var="tickClass" value="bg-outline-variant/25" />
                        <c:if test="${tickStep % 4 == 0}">
                          <c:set
                            var="tickClass"
                            value="bg-outline-variant/45"
                          />
                        </c:if>
                        <span
                          class="absolute top-0 h-8 w-px <c:out value='${tickClass}' />"
                          data-tick-left-pct="<c:out value='${tickLeft}' />"
                        ></span>
                      </c:forEach>
                    </div>
                    <div
                      class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none"
                    >
                      <div
                        data-timeline-preview
                        class="absolute top-0 h-8 rounded-md border border-warning/60 bg-warning/20 hidden pointer-events-none"
                      >
                        <div
                          data-timeline-preview-label
                          class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-warning/35 bg-surface px-2 py-0.5 text-[10px] font-bold text-warning"
                        ></div>
                      </div>
                    </div>
                    <div
                      class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2"
                      data-timeline-blocks
                    ></div>
                  </div>
                </div>

                <template data-availability-block-template>
                  <div
                    data-role="availability-block"
                    class="absolute top-0 h-8 rounded-md bg-gradient-to-r from-secondary to-secondary-container text-on-secondary shadow-[0_8px_16px_rgba(174,49,35,0.28)] cursor-grab active:cursor-grabbing"
                  >
                    <button
                      type="button"
                      data-role="left-handle"
                      class="absolute top-0 h-8 w-4 -translate-x-1/2 cursor-ew-resize touch-none"
                      style="left: 0%"
                    >
                      <span
                        class="absolute left-1/2 top-0 h-8 w-[3px] -translate-x-1/2 rounded-full bg-on-secondary"
                      ></span>
                      <span
                        data-role="left-label"
                        class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-outline-variant/30 bg-surface px-2 py-0.5 text-[10px] font-bold text-on-surface"
                      ></span>
                    </button>
                    <button
                      type="button"
                      data-role="right-handle"
                      class="absolute top-0 h-8 w-4 -translate-x-1/2 cursor-ew-resize touch-none"
                      style="left: 100%"
                    >
                      <span
                        class="absolute left-1/2 top-0 h-8 w-[3px] -translate-x-1/2 rounded-full bg-on-secondary"
                      ></span>
                      <span
                        data-role="right-label"
                        class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-outline-variant/30 bg-surface px-2 py-0.5 text-[10px] font-bold text-on-surface"
                      ></span>
                    </button>
                    <button
                      type="button"
                      data-role="delete-button"
                      class="absolute -bottom-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-error/40 bg-error/15 px-2 py-0.5 text-[10px] font-bold text-error hover:bg-error/25 transition-colors"
                    ></button>
                  </div>
                </template>

                <form
                  action="<c:out value='${saveUrl}' />"
                  method="post"
                  data-timeline-save-form
                  class="mt-6 flex justify-end"
                >
                  <input
                    type="hidden"
                    name="date"
                    value="<c:out value='${selectedDate}' />"
                  />
                  <div data-timeline-save-hidden-fields></div>
                  <paw:button
                    type="submit"
                    color="secondary"
                    size="md"
                    text="${saveChangesLabel}"
                    disabled="${true}"
                  />
                </form>
              </div>
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>
    </jsp:body>
  </paw:sectionCard>
</paw:layout>
