<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="profileUrl" value="/profile" />
<c:url var="pageUrl" value="/profile/item/${item.id}/availability" />
<c:url var="disableUrl" value="/profile/item/${item.id}/availability/disable" />
<c:url var="enableUrl" value="/profile/item/${item.id}/availability/enable" />

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

<paw:layout
    title="Botecito"
    mainClass="pt-24 pb-14 max-w-4xl mx-auto px-6"
    headerCtaMessageCode="nav.rent"
    headerCtaHref="/marketplace"
    headerCtaVariant="rent">

  <div class="mb-8">
    <a href="${profileUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
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

  <c:if test="${param.availabilityAction == 'disabled'}">
    <paw:alertMessage type="success"><spring:message code="manageAvailability.msg.disabled" /></paw:alertMessage>
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

  <paw:sectionCard icon="calendar_month">
    <jsp:attribute name="title"><c:out value="${calendarTitle}" /></jsp:attribute>
    <jsp:body>
      <p class="text-sm text-on-surface-variant m-0 mb-4"><c:out value="${calendarHint}" /></p>

      <form method="get" action="${pageUrl}" data-manage-availability-date-form class="mb-6">
        <paw:datePicker
            id="manageAvailabilityDate"
            name="date"
            offeredDatesJson="${offeredDatesJson}"
            occupiedDatesJson="${disabledDatesJson}"
            label="${datePickerLabel}"
            value="${selectedDate}"
            restrictToAvailability="false" />
        <noscript>
          <div class="mt-3">
            <paw:button type="submit" color="primary" size="sm" text="${calendarTitle}" />
          </div>
        </noscript>
      </form>

      <div class="flex flex-wrap gap-4 text-xs text-on-surface-variant mb-4">
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

      <c:choose>
        <c:when test="${empty selectedDate}">
          <p class="text-sm text-on-surface-variant m-0"><c:out value="${slotsNoDate}" /></p>
        </c:when>
        <c:otherwise>
          <h3 class="text-base font-bold m-0 mb-3">
            <c:out value="${slotsTitle}" /> &mdash; <c:out value="${selectedDate}" />
          </h3>
          <c:choose>
            <c:when test="${empty slots}">
              <p class="text-sm text-on-surface-variant m-0"><c:out value="${slotsEmpty}" /></p>
            </c:when>
            <c:otherwise>
              <div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2">
                <c:forEach var="slot" items="${slots}">
                  <c:set var="disableModalId" value="disable-slot-${slot.modalIdSuffix}" />
                  <c:set var="enableModalId" value="enable-slot-${slot.modalIdSuffix}" />
                  <c:choose>
                    <c:when test="${slot.state == 'AVAILABLE'}">
                      <button
                          type="button"
                          class="min-h-9 rounded-lg border border-transparent bg-surface-container-low text-on-surface text-xs font-bold cursor-pointer hover:-translate-y-0.5 hover:shadow-sm transition-all"
                          onclick="document.getElementById('${disableModalId}').showModal()">
                        <c:out value="${slot.startTime}" />
                      </button>

                      <paw:confirmModal
                          id="${disableModalId}"
                          title="${disableModalTitle}"
                          message=""
                          confirmText="${disableConfirmLabel}"
                          cancelText="${disableCancelLabel}"
                          confirmColor="danger"
                          icon="block">
                        <form action="${disableUrl}" method="post" class="m-0 w-full sm:w-auto">
                          <input type="hidden" name="date" value="${selectedDate}" />
                          <input type="hidden" name="startTime" value="${slot.startTime}" />
                          <input type="hidden" name="endTime" value="${slot.endTime}" />
                          <p class="m-0 mb-3 text-sm text-on-surface-variant">
                            <c:out value="${selectedDate}" /> &middot;
                            <c:out value="${slot.startTime}" /> &ndash; <c:out value="${slot.endTime}" />
                          </p>
                          <p class="m-0 mb-3 text-sm text-on-surface-variant">
                            <c:out value="${disableMessage}" />
                          </p>
                          <paw:button type="submit" color="danger" cssClass="w-full sm:w-auto" text="${disableConfirmLabel}" />
                        </form>
                      </paw:confirmModal>
                    </c:when>

                    <c:when test="${slot.state == 'DISABLED'}">
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
                        <form action="${enableUrl}" method="post" class="m-0 w-full sm:w-auto">
                          <input type="hidden" name="disabledSlotId" value="${slot.disabledSlotId}" />
                          <input type="hidden" name="date" value="${selectedDate}" />
                          <paw:button type="submit" color="success" cssClass="w-full sm:w-auto" text="${enableConfirmLabel}" />
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
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>
    </jsp:body>
  </paw:sectionCard>

  <paw:sectionCard icon="event_busy">
    <jsp:attribute name="title"><c:out value="${listTitle}" /></jsp:attribute>
    <jsp:body>
      <c:choose>
        <c:when test="${empty disabledSlots}">
          <p class="text-sm text-on-surface-variant m-0"><c:out value="${listEmpty}" /></p>
        </c:when>
        <c:otherwise>
          <ul class="m-0 p-0 list-none space-y-2">
            <c:forEach var="disabled" items="${disabledSlots}">
              <li class="flex items-center justify-between gap-3 rounded-lg bg-base-200 px-4 py-2">
                <div class="text-sm">
                  <span class="font-bold"><c:out value="${disabled.slotDate}" /></span>
                  <span class="text-on-surface-variant">
                    &middot; <c:out value="${disabled.startTime}" /> &ndash; <c:out value="${disabled.endTime}" />
                  </span>
                </div>
                <form action="${enableUrl}" method="post" class="m-0">
                  <input type="hidden" name="disabledSlotId" value="${disabled.id}" />
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

  <script src="<c:url value='/js/disabled-slots.js' />"></script>
</paw:layout>
