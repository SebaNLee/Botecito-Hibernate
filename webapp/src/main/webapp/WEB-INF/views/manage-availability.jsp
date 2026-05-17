<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="pageUrl" value="/my-boats/${item.id}/availability">
  <c:if test="${not empty manageAvailabilityReturnPath}">
    <c:param name="return" value="${manageAvailabilityReturnPath}" />
  </c:if>
</c:url>
<c:url var="disableUrl" value="/my-boats/${item.id}/availability/disable" />
<c:url var="enableUrl" value="/my-boats/${item.id}/availability/enable" />
<c:url var="manageAvailabilityBackUrl" value="${manageAvailabilityBackPath}" />

<spring:message code="manageAvailability.title" var="pageTitle" />
<spring:message code="manageAvailability.subtitle" var="pageSubtitle" />
<spring:message code="manageAvailability.calendar.title" var="calendarTitle" />
<spring:message code="manageAvailability.calendar.hint" var="calendarHint" />
<spring:message code="manageAvailability.calendar.datePickerLabel" var="datePickerLabel" />
<spring:message code="manageAvailability.slots.title" var="slotsTitle" />
<spring:message code="manageAvailability.slots.empty" var="slotsEmpty" />
<spring:message code="manageAvailability.slots.noDate" var="slotsNoDate" />
<spring:message code="manageAvailability.legend.available" var="legendAvailable" />
<spring:message code="manageAvailability.legend.disabled" var="legendDisabled" />
<spring:message code="manageAvailability.legend.booked" var="legendBooked" />
<spring:message code="manageAvailability.disable.title" var="disableModalTitle" />
<spring:message code="manageAvailability.disable.confirm" var="disableConfirmLabel" />
<spring:message code="manageAvailability.disable.cancel" var="disableCancelLabel" />
<spring:message code="manageAvailability.disable.message" var="disableMessage" />
<spring:message code="manageAvailability.enable.title" var="enableModalTitle" />
<spring:message code="manageAvailability.enable.confirm" var="enableConfirmLabel" />
<spring:message code="manageAvailability.enable.cancel" var="enableCancelLabel" />
<spring:message code="manageAvailability.enable.message" var="enableMessage" />
<spring:message code="manageAvailability.list.title" var="listTitle" />
<spring:message code="manageAvailability.list.empty" var="listEmpty" />
<spring:message code="manageAvailability.list.delete" var="listDeleteLabel" />
<spring:message code="manageAvailability.blockRange.pickStart" var="blockRangePickStart" />
<spring:message code="manageAvailability.blockRange.pickEnd" var="blockRangePickEnd" />
<spring:message code="manageAvailability.blockRange.confirm" var="blockRangeConfirm" />
<spring:message code="manageAvailability.blockRange.clear" var="blockRangeClear" />
<spring:message code="manageAvailability.blockRange.invalid" var="blockRangeInvalid" />

<paw:layout
    title="Botecito"
    mainClass="pt-24 pb-14 max-w-4xl mx-auto px-6"
    headerCtaMessageCode="nav.rent"
    headerCtaHref="/marketplace"
    headerCtaVariant="rent">

  <div class="mb-8">
    <a href="${manageAvailabilityBackUrl}" class="link link-hover inline-flex items-center gap-2 text-secondary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-8">
    <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">
      <c:out value="${pageTitle}" />
    </h1>
    <p class="text-on-surface-variant mt-2 m-0 text-lg">
      <c:out value="${item.title}" />
    </p>
    <p class="text-on-surface-variant mt-1 m-0 text-sm">
      <c:out value="${pageSubtitle}" />
    </p>
  </div>

  <c:if test="${param.availabilityAction == 'blocked'}">
    <paw:alertMessage type="success"><spring:message code="manageAvailability.msg.blocked" /></paw:alertMessage>
  </c:if>
  <c:if test="${param.availabilityAction == 'enabled'}">
    <paw:alertMessage type="success"><spring:message code="manageAvailability.msg.enabled" /></paw:alertMessage>
  </c:if>
  <c:if test="${param.availabilityAction == 'pastDate' || param.availabilityAction == 'invalid'}">
    <paw:alertMessage type="error"><spring:message code="manageAvailability.msg.invalid" /></paw:alertMessage>
  </c:if>
  <c:if test="${param.availabilityAction == 'hasBookings'}">
    <paw:alertMessage type="error"><spring:message code="manageAvailability.msg.hasBookings" /></paw:alertMessage>
  </c:if>
  <c:if test="${param.availabilityAction == 'notFound'}">
    <paw:alertMessage type="warning"><spring:message code="manageAvailability.msg.notFound" /></paw:alertMessage>
  </c:if>

  <paw:sectionCard icon="calendar_month" hostAccent="true">
    <jsp:attribute name="title"><c:out value="${calendarTitle}" /></jsp:attribute>
    <jsp:body>
      <p class="text-sm text-on-surface-variant m-0 mb-4"><c:out value="${calendarHint}" /></p>

      <form method="get" action="${pageUrl}" data-manage-availability-date-form class="mb-6">
        <paw:datePicker
            id="manageAvailabilityDate"
            dateFieldName="date"
            offeredDatesJson="${offeredDatesJson}"
            occupiedDatesJson="${blockedDatesJson}"
            label="${datePickerLabel}"
            value="${selectedDate}"
            restrictToAvailability="false" />
        <noscript>
          <div class="mt-3">
            <paw:button type="submit" color="secondary" size="sm" text="${calendarTitle}" />
          </div>
        </noscript>
      </form>

      <c:choose>
        <c:when test="${empty selectedDate}">
          <p class="text-sm text-on-surface-variant m-0"><c:out value="${slotsNoDate}" /></p>
        </c:when>
        <c:otherwise>
          <div class="mb-4 flex flex-wrap gap-4 text-xs text-on-surface-variant">
            <span class="inline-flex items-center gap-2">
              <span class="inline-block h-3 w-3 rounded bg-surface-container-low border border-outline/20"></span>
              <c:out value="${legendAvailable}" />
            </span>
            <span class="inline-flex items-center gap-2">
              <span class="inline-block h-3 w-3 rounded bg-error/40"></span>
              <c:out value="${legendDisabled}" />
            </span>
            <span class="inline-flex items-center gap-2">
              <span class="inline-block h-3 w-3 rounded bg-surface-container-highest/50"></span>
              <c:out value="${legendBooked}" />
            </span>
          </div>
          <h3 class="text-base font-bold m-0 mb-3">
            <c:out value="${slotsTitle}" /> &mdash; <c:out value="${selectedDate}" />
          </h3>
          <c:choose>
            <c:when test="${empty slots}">
              <p class="text-sm text-on-surface-variant m-0"><c:out value="${slotsEmpty}" /></p>
            </c:when>
            <c:otherwise>
              <div
                  class="space-y-4"
                  data-personal-block-root
                  data-selected-date="${selectedDate}"
                  data-hint-pick-end="<c:out value='${blockRangePickEnd}' />"
                  data-hint-invalid="<c:out value='${blockRangeInvalid}' />">
                <p class="m-0 mb-2 text-xs leading-snug text-on-surface-variant">
                  <c:out value="${blockRangePickStart}" />
                </p>
                <div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2">
                  <c:forEach var="slot" items="${slots}" varStatus="st">
                    <c:set var="enableModalId" value="enable-slot-${slot.modalIdSuffix}" />
                    <c:choose>
                      <c:when test="${slot.state == 'AVAILABLE'}">
                        <button
                            type="button"
                            class="personal-block-slot-btn min-h-9 rounded-lg border border-transparent bg-surface-container-low text-on-surface text-xs font-bold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-sm"
                            data-personal-block-slot
                            data-slot-index="${st.index}"
                            data-slot-start="${slot.startTime}"
                            data-slot-end="${slot.endTime}">
                          <c:out value="${slot.startTime}" />
                        </button>
                      </c:when>

                      <c:when test="${slot.state == 'BLOCKED'}">
                        <button
                            type="button"
                            class="min-h-9 rounded-lg border border-error/30 bg-error/25 text-error text-xs font-bold cursor-pointer hover:bg-error/35 transition-colors"
                            onclick="document.getElementById('${enableModalId}').showModal()">
                          <c:out value="${slot.startTime}" />
                        </button>

                        <paw:confirmModal
                            id="${enableModalId}"
                            title="${enableModalTitle}"
                            message="${enableMessage}"
                            confirmText="${enableConfirmLabel}"
                            cancelText="${enableCancelLabel}"
                            confirmColor="success"
                            icon="event_available">
                          <form action="${enableUrl}" method="post" class="m-0">
                            <c:if test="${not empty manageAvailabilityReturnPath}">
                              <input type="hidden" name="return" value="<c:out value='${manageAvailabilityReturnPath}' />" />
                            </c:if>
                            <input type="hidden" name="blockBookingId" value="${slot.blockBookingId}" />
                            <input type="hidden" name="date" value="${selectedDate}" />
                            <paw:button type="submit" color="success" cssClass="whitespace-nowrap" text="${enableConfirmLabel}" />
                          </form>
                        </paw:confirmModal>
                      </c:when>

                      <c:otherwise>
                        <span class="min-h-9 flex items-center justify-center rounded-lg bg-surface-container-highest/50 text-outline/60 text-xs font-bold cursor-not-allowed">
                          <c:out value="${slot.startTime}" />
                        </span>
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>
                </div>

                <div data-personal-block-toolbar class="hidden rounded-xl border border-outline-variant/25 bg-base-200/60 px-4 py-3">
                  <p class="m-0 text-sm text-on-surface-variant" data-personal-block-hint></p>
                  <div class="mt-3 flex flex-row flex-wrap items-center justify-end gap-2">
                    <button type="button" class="btn btn-ghost btn-sm shrink-0" data-personal-block-clear>
                      <c:out value="${blockRangeClear}" />
                    </button>
                    <button type="button" class="btn btn-primary btn-sm shrink-0" disabled data-personal-block-open>
                      <c:out value="${blockRangeConfirm}" />
                    </button>
                  </div>
                </div>

                <script type="application/json" id="manage-availability-slots-json">${slotsStateJson}</script>
              </div>

              <dialog id="personal-block-confirm-dialog" class="modal">
                <div class="modal-box max-w-lg">
                  <div class="flex items-start gap-4">
                    <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-error/15 text-error">
                      <span class="material-symbols-outlined">block</span>
                    </div>
                    <div class="min-w-0 flex-1 space-y-2">
                      <h3 class="m-0 text-xl font-extrabold tracking-tight">
                        <c:out value="${disableModalTitle}" />
                      </h3>
                      <p id="personal-block-summary" class="m-0 text-sm leading-relaxed text-on-surface-variant"></p>
                      <p class="m-0 text-sm leading-relaxed text-on-surface-variant">
                        <c:out value="${disableMessage}" />
                      </p>
                    </div>
                  </div>
                  <div class="mt-6 flex flex-row flex-nowrap items-center justify-end gap-3 border-t border-outline-variant/20 pt-4">
                    <form method="dialog" class="m-0 shrink-0">
                      <button type="submit" class="btn btn-outline min-h-11 whitespace-nowrap px-5">
                        <c:out value="${disableCancelLabel}" />
                      </button>
                    </form>
                    <form id="personal-block-submit-form" action="${disableUrl}" method="post" class="m-0 shrink-0">
                      <c:if test="${not empty manageAvailabilityReturnPath}">
                        <input type="hidden" name="return" value="<c:out value='${manageAvailabilityReturnPath}' />" />
                      </c:if>
                      <input type="hidden" name="date" value="${selectedDate}" />
                      <input type="hidden" name="startTime" id="personal-block-input-start" value="" />
                      <input type="hidden" name="endTime" id="personal-block-input-end" value="" />
                      <button type="submit" class="btn btn-error min-h-11 whitespace-nowrap px-5">
                        <c:out value="${disableConfirmLabel}" />
                      </button>
                    </form>
                  </div>
                </div>
                <form method="dialog" class="modal-backdrop">
                  <button type="submit" aria-label="<c:out value='${disableCancelLabel}' />">close</button>
                </form>
              </dialog>
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>
    </jsp:body>
  </paw:sectionCard>

  <paw:sectionCard icon="event_busy" hostAccent="true">
    <jsp:attribute name="title"><c:out value="${listTitle}" /></jsp:attribute>
    <jsp:body>
      <c:choose>
        <c:when test="${empty personalBlockRows}">
          <p class="text-sm text-on-surface-variant m-0"><c:out value="${listEmpty}" /></p>
        </c:when>
        <c:otherwise>
          <ul class="m-0 p-0 list-none space-y-2">
            <c:forEach var="row" items="${personalBlockRows}">
              <li class="flex items-center justify-between gap-3 rounded-lg bg-base-200 px-4 py-2">
                <div class="text-sm">
                  <span class="font-bold"><c:out value="${row.dateIso}" /></span>
                  <span class="text-on-surface-variant">
                    &middot; <c:out value="${row.startTime}" /> &ndash; <c:out value="${row.endTime}" />
                  </span>
                </div>
                <form action="${enableUrl}" method="post" class="m-0">
                  <c:if test="${not empty manageAvailabilityReturnPath}">
                    <input type="hidden" name="return" value="<c:out value='${manageAvailabilityReturnPath}' />" />
                  </c:if>
                  <input type="hidden" name="blockBookingId" value="${row.bookingId}" />
                  <input type="hidden" name="date" value="${selectedDate}" />
                  <button type="submit" class="btn btn-sm btn-ghost text-error" aria-label="<c:out value='${listDeleteLabel}' />">
                    <span class="material-symbols-outlined text-base">delete</span>
                    <c:out value="${listDeleteLabel}" />
                  </button>
                </form>
              </li>
            </c:forEach>
          </ul>
        </c:otherwise>
      </c:choose>
    </jsp:body>
  </paw:sectionCard>

  <script src="<c:url value='/js/manage-availability-date.js' />"></script>
  <script src="<c:url value='/js/manage-availability-block-range.js' />"></script>
</paw:layout>
