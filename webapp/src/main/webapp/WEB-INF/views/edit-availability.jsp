<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="myBoatsUrl" value="/my-boats" />
<c:url var="editDetailsUrl" value="/edit/${itemId}/details" />
<c:url var="editAvailabilityUrl" value="/edit/${itemId}/availability" />
<spring:message code="publish.availability.noRanges" var="publishNoRangesLabel" />
<spring:message code="publish.step2.deleteRange" var="publishDeleteRangeLabel" />
<spring:message code="publish.availability.day.empty.client" var="publishMissingRangeLabel" />
<spring:message code="publish.actions.continueImages" var="publishContinueImagesLabel" />

<spring:message code="page.title.edit" var="titleEdit" />
<paw:layout
  title="${titleEdit} - Botecito"
  mainClass="pt-24 pb-14 w-full max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent"
  scripts="edit-wizard,weekly-availability">
  <div
    data-edit-wizard-root="step2"
    data-edit-details-url="<c:out value='${editDetailsUrl}' />"
    data-my-boats-url="<c:out value='${myBoatsUrl}' />"
    data-item-id="<c:out value='${itemId}' />"
    class="w-full"
  >
  <div class="mb-8">
    <a href="<c:out value='${editDetailsUrl}' />" class="link link-hover inline-flex items-center gap-2 text-secondary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span><spring:message code="editPublication.step2.progress" /></span>
      <span><spring:message code="editPublication.step2.badge" /></span>
    </div>
    <progress class="progress progress-secondary mt-3 w-full" value="33" max="100"></progress>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"><spring:message code="editPublication.step2.title" /></h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0"><spring:message code="editPublication.step2.subtitle" /></p>
  </div>

  <form:form action="${editAvailabilityUrl}" method="post" modelAttribute="publishForm" class="space-y-8">
    <div data-edit-wizard-step1-hidden="true"></div>
    <paw:sectionCard icon="schedule" hostAccent="true">
      <jsp:attribute name="title"><spring:message code="publish.step2.section.slots" /></jsp:attribute>
      <jsp:body>
        <c:if test="${param.availabilityAction == 'invalidMethod'}">
          <paw:alertMessage type="error" cssClass="mb-4">
            <c:out value="${publishMissingRangeLabel}" />
          </paw:alertMessage>
        </c:if>
        <div
            data-weekly-availability-grid
            data-min-duration="120"
            data-no-ranges-text="<c:out value='${publishNoRangesLabel}'/>"
            data-delete-text="<c:out value='${publishDeleteRangeLabel}'/>"
            data-missing-range-text="<c:out value='${publishMissingRangeLabel}'/>">

          <c:forEach var="range" items="${publishForm.availabilityRanges}">
            <span class="hidden"
                  data-existing-slot
                  data-weekday="<c:out value='${range.weekday}'/>"
                  data-start="<c:out value='${range.startTime}'/>"
                  data-end="<c:out value='${range.endTime}'/>"></span>
          </c:forEach>

          <p class="text-xs text-outline m-0"><spring:message code="publish.step2.instructions" /></p>

          <form:errors cssClass="mt-2" element="div" />
          <spring:hasBindErrors name="publishForm">
            <c:forEach var="error" items="${errors.globalErrors}">
              <paw:alertMessage type="error" cssClass="mt-2">
                <spring:message code="${error.codes[0]}" arguments="${error.arguments}" />
              </paw:alertMessage>
            </c:forEach>
          </spring:hasBindErrors>
          <div role="alert" class="alert alert-error alert-soft rounded-xl text-sm items-start mt-2 hidden" data-availability-client-alert>
            <span class="material-symbols-outlined text-lg shrink-0">error</span>
            <span><c:out value="${publishMissingRangeLabel}" /></span>
          </div>

          <div class="space-y-4 mt-6">
            <c:forEach var="day" items="${['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']}">
              <c:set var="dayLower" value="${fn:toLowerCase(day)}" />
              <c:set var="dayId" value="${dayLower}Enabled" />
              <div class="rounded-xl border border-outline-variant/25 bg-base-200/60 p-4" data-availability-row="<c:out value='${day}' />">
                <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="<c:out value='${dayId}' />">
                  <input type="checkbox" id="<c:out value='${dayId}' />" name="enabledDays" value="<c:out value='${day}' />" class="checkbox checkbox-secondary checkbox-sm" data-day-toggle="<c:out value='${day}' />" <c:if test="${enabledWeekdays[day]}">checked="checked"</c:if> />
                  <spring:message code="weekday.${dayLower}" />
                </label>
                <div data-day-slots="<c:out value='${day}' />">
                  <div class="mt-2">
                    <div class="relative pt-1 pb-8">
                      <div class="absolute inset-x-0 bottom-0 flex items-center justify-between text-[11px] font-bold text-outline">
                        <span>00:00h</span>
                        <span>23:30h</span>
                      </div>
                      <div class="relative h-12 rounded-xl border border-outline-variant/30 bg-surface-container-high/40 overflow-visible touch-none" data-timeline-track="<c:out value='${day}' />">
                        <div class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 rounded-lg border border-outline-variant/25 bg-base-200/80" data-timeline-hover-zone></div>
                        <div class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none" data-timeline-ticks>
                          <c:forEach var="tickHour" begin="0" end="23">
                            <c:set var="tickStep" value="${tickHour * 2}" />
                            <c:set var="tickLeft" value="${(tickStep * 100.0) / 47}" />
                            <c:set var="tickClass" value="bg-outline-variant/25" />
                            <c:if test="${tickStep % 4 == 0}">
                              <c:set var="tickClass" value="bg-outline-variant/45" />
                            </c:if>
                            <span class="absolute top-0 h-8 w-px <c:out value='${tickClass}' />" data-tick-left-pct="<c:out value='${tickLeft}' />"></span>
                          </c:forEach>
                        </div>
                        <div class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2 pointer-events-none">
                          <div data-timeline-preview class="absolute top-0 h-8 rounded-md border border-secondary/60 bg-secondary/20 hidden pointer-events-none">
                            <div data-timeline-preview-label class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-secondary/35 bg-surface px-2 py-0.5 text-[10px] font-bold text-secondary"></div>
                          </div>
                        </div>
                        <div class="absolute inset-x-1 top-1/2 h-8 -translate-y-1/2" data-timeline-blocks></div>
                      </div>
                    </div>
                  </div>
                </div>
                <div data-day-summary="<c:out value='${day}' />"></div>
                <p class="mt-2 hidden text-xs font-bold text-error" data-day-error="<c:out value='${day}' />"></p>
              </div>
            </c:forEach>
          </div>

          <template data-availability-block-template>
            <div data-role="availability-block" class="absolute top-0 h-8 rounded-md bg-gradient-to-r from-secondary to-secondary-container text-on-secondary shadow-[0_8px_16px_rgba(174,49,35,0.28)] cursor-grab active:cursor-grabbing">
              <button type="button" data-role="left-handle" class="absolute top-0 h-8 w-4 -translate-x-1/2 cursor-ew-resize touch-none" style="left:0%">
                <span class="absolute left-1/2 top-0 h-8 w-[3px] -translate-x-1/2 rounded-full bg-on-secondary"></span>
                <span data-role="left-label" class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-outline-variant/30 bg-surface px-2 py-0.5 text-[10px] font-bold text-on-surface"></span>
              </button>
              <button type="button" data-role="right-handle" class="absolute top-0 h-8 w-4 -translate-x-1/2 cursor-ew-resize touch-none" style="left:100%">
                <span class="absolute left-1/2 top-0 h-8 w-[3px] -translate-x-1/2 rounded-full bg-on-secondary"></span>
                <span data-role="right-label" class="absolute -top-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-outline-variant/30 bg-surface px-2 py-0.5 text-[10px] font-bold text-on-surface"></span>
              </button>
              <button type="button" data-role="delete-button" class="absolute -bottom-7 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full border border-error/40 bg-error/15 px-2 py-0.5 text-[10px] font-bold text-error hover:bg-error/25 transition-colors"></button>
            </div>
          </template>

          <div data-availability-hidden-inputs></div>
        </div>
      </jsp:body>
    </paw:sectionCard>

    <div class="flex flex-col sm:flex-row sm:justify-end items-center gap-4 pt-2">
      <paw:button type="submit" color="secondary" size="lg" icon="arrow_forward" iconTrailing="true" cssClass="w-full sm:w-auto" text="${publishContinueImagesLabel}" />
    </div>
  </form:form>
  </div>
</paw:layout>
