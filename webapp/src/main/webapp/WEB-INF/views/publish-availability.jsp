<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="stepOneUrl" value="/publish" />
<c:url var="stepTwoUrl" value="/publish/availability" />
<c:url var="marketplaceUrl" value="/marketplace" />
<spring:message code="publish.availability.noRanges" var="publishNoRangesLabel" />
<spring:message code="publish.step2.deleteRange" var="publishDeleteRangeLabel" />
<spring:message code="publish.actions.saveDraft" var="publishSaveDraftLabel" />
<spring:message code="publish.actions.continueContact" var="publishContinueContactLabel" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">
  <div class="mb-8">
    <a href="${stepOneUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span><spring:message code="publish.step2.progress" /></span>
      <span><spring:message code="publish.step2.badge" /></span>
    </div>
    <progress class="progress progress-primary mt-3 w-full" value="66" max="100"></progress>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"><spring:message code="publish.step2.title" /></h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0"><spring:message code="publish.step2.subtitle" /></p>
  </div>

  <form:form action="${stepTwoUrl}" method="post" modelAttribute="publishForm" class="space-y-8">
    <paw:sectionCard icon="schedule">
      <jsp:attribute name="title"><spring:message code="publish.step2.section.slots" /></jsp:attribute>
      <jsp:body>
          <div
              data-weekly-availability-grid
              data-existing-slots='${existingSlotsJson}'
              data-min-duration="120"
              data-no-ranges-text="${publishNoRangesLabel}"
              data-delete-text="${publishDeleteRangeLabel}">

          <div class="flex flex-wrap items-center gap-4 text-[10px] font-bold text-on-surface-variant">
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-3 w-3 rounded bg-primary/15 border border-primary/40"></span>
              <spring:message code="publish.step2.legend.available" />
            </span>
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-3 w-3 rounded bg-primary"></span>
              <spring:message code="publish.step2.legend.selected" />
            </span>
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-3 w-3 rounded bg-primary/80"></span>
              <spring:message code="publish.step2.legend.start" />
            </span>
            <span class="inline-flex items-center gap-1.5">
              <span class="inline-block h-3 w-3 rounded bg-error/20 border border-error/40"></span>
              <spring:message code="publish.step2.legend.unavailable" />
            </span>
          </div>
          <p class="text-xs text-outline m-0"><spring:message code="publish.step2.instructions" /></p>

          <form:errors path="availabilityByWeekday" cssClass="mt-2" element="div" />
          <spring:hasBindErrors name="publishForm">
            <c:forEach var="error" items="${errors.globalErrors}">
              <paw:alertMessage type="error" cssClass="mt-2">
                <spring:message code="${error.codes[0]}" arguments="${error.arguments}" />
              </paw:alertMessage>
            </c:forEach>
          </spring:hasBindErrors>

          <div class="space-y-4 mt-6">
            <c:forEach var="day" items="${['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']}">
              <c:set var="dayLower" value="${fn:toLowerCase(day)}" />
              <c:set var="dayId" value="${dayLower}Enabled" />
              <div class="rounded-xl border border-outline-variant/25 bg-base-200/60 p-4" data-availability-row>
                <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="${dayId}">
                  <input type="checkbox" id="${dayId}" name="enabledDays" value="${day}" class="checkbox checkbox-primary checkbox-sm" data-day-toggle="${day}" <c:if test="${enabledWeekdays[day]}">checked="checked"</c:if> />
                  <spring:message code="weekday.${dayLower}" />
                </label>
                <div data-day-slots="${day}"></div>
                <div data-day-summary="${day}"></div>
              </div>
            </c:forEach>
          </div>

          <div data-availability-hidden-inputs></div>
        </div>
      </jsp:body>
    </paw:sectionCard>

    <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-2">
      <paw:button href="${marketplaceUrl}" color="ghost" size="lg" cssClass="w-full sm:w-auto" text="${publishSaveDraftLabel}" />
      <paw:button type="submit" color="secondary" size="lg" icon="arrow_forward" iconTrailing="true" cssClass="w-full sm:w-auto" text="${publishContinueContactLabel}" />
    </div>
  </form:form>
</paw:layout>
