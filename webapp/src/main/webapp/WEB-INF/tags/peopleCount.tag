<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="min" required="false" %>
<%@ attribute name="max" required="false" %>
<%@ attribute name="step" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="variant" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="allowEmpty" required="false" %>
<%@ attribute name="required" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ attribute name="errorPath" required="false" %>
<%@ attribute name="hostAccent" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="filters.people" var="defaultPeopleLabel" />
<spring:message code="filters.people.placeholder" var="defaultPeoplePlaceholder" />
<spring:message code="peopleCount.decrement" var="peopleDecrementLabel" />
<spring:message code="peopleCount.increment" var="peopleIncrementLabel" />
<spring:message code="peopleCount.clear" var="peopleClearLabel" />
<spring:message code="peopleCount.value" var="peopleValueLabel" />
<c:set var="resolvedLabel" value="${not empty label ? label : defaultPeopleLabel}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedMin" value="${not empty min ? min : '1'}" />
<c:set var="resolvedMax" value="${not empty max ? max : '20'}" />
<c:set var="resolvedStep" value="${not empty step ? step : '1'}" />
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'groups'}" />
<c:set var="resolvedVariant" value="${not empty variant ? variant : 'default'}" />
<c:set var="resolvedPlaceholder" value="${not empty placeholder ? placeholder : defaultPeoplePlaceholder}" />
<c:set var="resolvedAllowEmpty" value="${empty allowEmpty ? false : allowEmpty}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />
<c:set var="peopleClearInitiallyHidden" value="${empty resolvedValue}" />
<c:set var="pcText" value="${hostAccent ne null and hostAccent ? 'text-secondary' : 'text-primary'}" />

<c:choose>
  <c:when test="${resolvedVariant == 'inline'}">
    <fieldset
        class="fieldset w-full ${resolvedContainerClass}"
        data-people-count
        data-min="${resolvedMin}"
        data-max="${resolvedMax}"
        data-step="${resolvedStep}"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}"
        data-allow-empty="${resolvedAllowEmpty}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-people-input />
      <legend class="fieldset-legend block text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
        <c:out value="${resolvedLabel}" />
        <c:if test="${required}"><span class="text-error" aria-hidden="true">*</span></c:if>
      </legend>
      <div class="min-w-0 flex flex-col gap-2">
        <div class="input input-bordered input-sm flex h-10 min-h-10 max-h-10 w-full min-w-0 items-center gap-1 overflow-hidden px-2 py-0">
          <span class="material-symbols-outlined shrink-0 ${pcText} text-xl"><c:out value="${resolvedIcon}" /></span>
          <button
              type="button"
              class="btn btn-ghost btn-xs btn-circle shrink-0 ${pcText}"
              aria-label="${fn:escapeXml(peopleDecrementLabel)}"
              data-people-decrement>
            <span class="material-symbols-outlined text-base">remove</span>
          </button>
          <div class="flex min-w-0 flex-1 items-center justify-center gap-1">
            <input
                class="w-full min-w-0 appearance-none bg-transparent border-none p-0 text-center text-[0.95rem] font-bold tabular-nums text-on-surface placeholder:text-on-surface/60 outline-none focus:outline-none focus:ring-0 shadow-none"
                type="text"
                inputmode="numeric"
                autocomplete="off"
                spellcheck="false"
                placeholder="${fn:escapeXml(resolvedPlaceholder)}"
                aria-label="${fn:escapeXml(peopleValueLabel)}"
                data-people-field />
            <button
                type="button"
                class="btn btn-ghost btn-xs btn-circle ${pcText} ${peopleClearInitiallyHidden ? 'hidden' : ''}"
                aria-label="${fn:escapeXml(peopleClearLabel)}"
                data-people-clear>
              <span class="material-symbols-outlined text-base">close</span>
            </button>
          </div>
          <button
              type="button"
              class="btn btn-ghost btn-xs btn-circle shrink-0 ${pcText}"
              aria-label="${fn:escapeXml(peopleIncrementLabel)}"
              data-people-increment>
            <span class="material-symbols-outlined text-base">add</span>
          </button>
        </div>
      </div>
    </fieldset>
  </c:when>
  <c:otherwise>
    <fieldset
        class="fieldset w-full ${resolvedContainerClass}"
        data-people-count
        data-min="${resolvedMin}"
        data-max="${resolvedMax}"
        data-step="${resolvedStep}"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}"
        data-allow-empty="${resolvedAllowEmpty}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-people-input />
      <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
        <c:out value="${resolvedLabel}" />
        <c:if test="${required}"><span class="text-error" aria-hidden="true">*</span></c:if>
      </legend>
      <div class="input input-bordered input-sm flex h-10 min-h-10 max-h-10 w-full min-w-0 items-center gap-1 overflow-hidden px-2 py-0">
        <span class="material-symbols-outlined shrink-0 ${pcText} text-xl"><c:out value="${resolvedIcon}" /></span>
        <button
            type="button"
            class="btn btn-ghost btn-xs btn-circle shrink-0 ${pcText}"
            aria-label="${fn:escapeXml(peopleDecrementLabel)}"
            data-people-decrement>
          <span class="material-symbols-outlined text-base">remove</span>
        </button>
        <div class="flex min-w-0 flex-1 items-center justify-center gap-1">
          <input
              class="w-full min-w-0 appearance-none bg-transparent border-none p-0 text-center text-base font-bold tabular-nums text-on-surface placeholder:text-on-surface/60 outline-none focus:outline-none focus:ring-0 shadow-none"
              type="text"
              inputmode="numeric"
              autocomplete="off"
              spellcheck="false"
              placeholder="${fn:escapeXml(resolvedPlaceholder)}"
              aria-label="${fn:escapeXml(peopleValueLabel)}"
              data-people-field />
          <button
              type="button"
              class="btn btn-ghost btn-xs btn-circle ${pcText} ${peopleClearInitiallyHidden ? 'hidden' : ''}"
              aria-label="${fn:escapeXml(peopleClearLabel)}"
              data-people-clear>
            <span class="material-symbols-outlined text-base">close</span>
          </button>
        </div>
        <button
            type="button"
            class="btn btn-ghost btn-xs btn-circle shrink-0 ${pcText}"
            aria-label="${fn:escapeXml(peopleIncrementLabel)}"
            data-people-increment>
          <span class="material-symbols-outlined text-base">add</span>
        </button>
      </div>

      <c:if test="${not empty errorPath}">
        <form:errors path="${errorPath}" cssClass="text-error text-xs mt-1" element="p" />
      </c:if>
    </fieldset>
  </c:otherwise>
</c:choose>
