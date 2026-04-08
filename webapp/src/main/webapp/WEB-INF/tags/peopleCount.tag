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
<%@ attribute name="containerClass" required="false" %>
<%@ attribute name="errorPath" required="false" %>
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

<c:choose>
  <c:when test="${resolvedVariant == 'inline'}">
    <div
        class="w-full ${resolvedContainerClass}"
        data-people-count
        data-min="${resolvedMin}"
        data-max="${resolvedMax}"
        data-step="${resolvedStep}"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}"
        data-allow-empty="${resolvedAllowEmpty}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-people-input />
      <div class="flex w-full min-w-0 items-start gap-2.5">
        <span class="material-symbols-outlined mt-1 shrink-0 text-primary"><c:out value="${resolvedIcon}" /></span>
        <div class="min-w-0 flex-1">
          <div class="block text-left text-[10px] font-bold uppercase tracking-wider text-outline">
            <c:out value="${resolvedLabel}" />
          </div>
          <div class="mt-1 flex w-full min-w-0 items-center gap-1.5">
            <button
                type="button"
                class="inline-flex h-6 w-6 shrink-0 items-center justify-center self-center rounded-full bg-surface-container text-primary transition-transform active:scale-95 cursor-pointer border-none"
                aria-label="${fn:escapeXml(peopleDecrementLabel)}"
                data-people-decrement>
              <span class="material-symbols-outlined text-[16px] leading-none">remove</span>
            </button>
            <div class="flex min-h-6 min-w-0 flex-1 items-center justify-center">
              <div class="inline-flex max-w-full min-w-0 items-center justify-center gap-1">
                <input
                    class="min-w-[4rem] max-w-[5.5rem] shrink appearance-none bg-transparent border-none p-0 text-center text-sm font-bold tabular-nums leading-none text-on-surface outline-none focus:outline-none focus-visible:outline-none focus:ring-0 shadow-none"
                    type="text"
                    inputmode="numeric"
                    autocomplete="off"
                    spellcheck="false"
                    placeholder="${fn:escapeXml(resolvedPlaceholder)}"
                    aria-label="${fn:escapeXml(peopleValueLabel)}"
                    data-people-field />
                <button
                    type="button"
                    class="h-6 w-6 shrink-0 items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container hover:text-on-surface cursor-pointer border-none bg-transparent leading-none ${peopleClearInitiallyHidden ? 'hidden' : 'inline-flex'}"
                    aria-label="${fn:escapeXml(peopleClearLabel)}"
                    data-people-clear>
                  <span class="material-symbols-outlined text-[16px] leading-none">close</span>
                </button>
              </div>
            </div>
            <button
                type="button"
                class="inline-flex h-6 w-6 shrink-0 items-center justify-center self-center rounded-full bg-surface-container text-primary transition-transform active:scale-95 cursor-pointer border-none"
                aria-label="${fn:escapeXml(peopleIncrementLabel)}"
                data-people-increment>
              <span class="material-symbols-outlined text-[16px] leading-none">add</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <div
        class="w-full ${resolvedContainerClass}"
        data-people-count
        data-min="${resolvedMin}"
        data-max="${resolvedMax}"
        data-step="${resolvedStep}"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}"
        data-allow-empty="${resolvedAllowEmpty}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-people-input />
      <div class="space-y-2">
        <label class="block text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
          <c:out value="${resolvedLabel}" />
        </label>
        <div class="flex min-h-[2.75rem] w-full min-w-0 items-center gap-2 rounded-xl bg-surface-container-high p-2">
          <button
              type="button"
              class="flex h-10 w-10 shrink-0 items-center justify-center self-center rounded-lg bg-surface-container-lowest text-primary shadow-sm transition-transform active:scale-90 cursor-pointer border-none"
              aria-label="${fn:escapeXml(peopleDecrementLabel)}"
              data-people-decrement>
            <span class="material-symbols-outlined leading-none">remove</span>
          </button>
          <div class="flex min-h-10 min-w-0 flex-1 items-center justify-center">
            <div class="inline-flex max-w-full min-w-0 items-center justify-center gap-1">
              <input
                  class="min-w-[7rem] max-w-[9rem] shrink appearance-none bg-transparent border-none p-0 text-center text-lg font-bold tabular-nums leading-none text-on-surface outline-none focus:outline-none focus-visible:outline-none focus:ring-0 shadow-none"
                  type="text"
                  inputmode="numeric"
                  autocomplete="off"
                  spellcheck="false"
                  placeholder="${fn:escapeXml(resolvedPlaceholder)}"
                  aria-label="${fn:escapeXml(peopleValueLabel)}"
                  data-people-field />
              <button
                  type="button"
                  class="h-10 w-10 shrink-0 items-center justify-center rounded-lg text-outline transition-colors hover:bg-surface-container hover:text-on-surface cursor-pointer border-none bg-transparent leading-none ${peopleClearInitiallyHidden ? 'hidden' : 'inline-flex'}"
                  aria-label="${fn:escapeXml(peopleClearLabel)}"
                  data-people-clear>
                <span class="material-symbols-outlined leading-none">close</span>
              </button>
            </div>
          </div>
          <button
              type="button"
              class="flex h-10 w-10 shrink-0 items-center justify-center self-center rounded-lg bg-surface-container-lowest text-primary shadow-sm transition-transform active:scale-90 cursor-pointer border-none"
              aria-label="${fn:escapeXml(peopleIncrementLabel)}"
              data-people-increment>
            <span class="material-symbols-outlined leading-none">add</span>
          </button>
        </div>
      </div>

      <c:if test="${not empty errorPath}">
        <form:errors
            path="${errorPath}"
            cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
      </c:if>
    </div>
  </c:otherwise>
</c:choose>
