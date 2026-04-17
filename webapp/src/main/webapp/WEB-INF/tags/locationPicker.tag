<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="variant" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ attribute name="errorPath" required="false" %>
<%@ attribute name="optionsUrl" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<spring:message code="filters.location" var="defaultLocationLabel" />
<spring:message code="filters.location.placeholder" var="defaultLocationPlaceholder" />
<spring:message code="locationPicker.clear" var="clearLocationLabel" />
<spring:message code="locationPicker.availableLocations" var="availableLocationsLabel" />
<spring:message code="locationPicker.noMatches" var="noLocationsLabel" />
<c:set var="resolvedLabel" value="${not empty label ? label : defaultLocationLabel}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedPlaceholder" value="${not empty placeholder ? placeholder : defaultLocationPlaceholder}" />
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'location_on'}" />
<c:set var="resolvedVariant" value="${not empty variant ? variant : 'default'}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />
<c:set var="resolvedOptionsUrl" value="${not empty optionsUrl ? optionsUrl : '/location-options'}" />

<c:choose>
  <c:when test="${resolvedVariant == 'inline'}">
    <div
        class="relative w-full ${resolvedContainerClass}"
        data-location-picker
        data-options-url="<c:url value='${resolvedOptionsUrl}' />"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-location-value />
      <div
          class="flex items-center gap-3 rounded-2xl px-0 py-0.5 cursor-text text-on-surface hover:bg-base-200/60 transition-colors"
          data-location-trigger>
        <span class="material-symbols-outlined shrink-0 text-primary"><c:out value="${resolvedIcon}" /></span>
        <div class="min-w-0 flex-1 flex flex-col gap-0.5">
          <label class="block text-[10px] font-extrabold tracking-[0.16em] uppercase text-outline" for="${id}-query">
            <c:out value="${resolvedLabel}" />
          </label>
          <input
              id="${id}-query"
              type="text"
              value=""
              class="w-full appearance-none bg-transparent border-none p-0 text-[0.95rem] leading-[1.35] font-bold text-on-surface placeholder:text-on-surface/60 outline-none focus:outline-none focus-visible:outline-none focus:ring-0 shadow-none"
              placeholder="${fn:escapeXml(resolvedPlaceholder)}"
              autocomplete="off"
              spellcheck="false"
              data-location-query />
        </div>
        <button
            type="button"
            class="btn btn-ghost btn-xs btn-circle opacity-0 pointer-events-none"
            aria-label="${fn:escapeXml(clearLocationLabel)}"
            data-location-clear>
          <span class="material-symbols-outlined text-base">close</span>
        </button>
        <span class="material-symbols-outlined shrink-0 text-primary text-base transition-transform duration-150" data-location-chevron>expand_more</span>
      </div>

      <div
          class="card bg-base-100 absolute left-0 right-0 top-full z-[10020] mt-2 hidden overflow-hidden shadow-xl"
          data-location-panel
          hidden>
        <div class="border-b border-outline-variant/20 px-4 py-3">
          <p class="m-0 text-[10px] font-bold uppercase tracking-[0.16em] text-outline"><c:out value="${availableLocationsLabel}" /></p>
        </div>
        <div class="h-64 overflow-y-auto p-2" data-location-options></div>
        <p class="hidden px-4 py-4 text-sm text-on-surface-variant" data-location-empty>
          <c:out value="${noLocationsLabel}" />
        </p>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <fieldset
        class="fieldset w-full ${resolvedContainerClass}"
        data-location-picker
        data-options-url="<c:url value='${resolvedOptionsUrl}' />"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-location-value />
      <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="${id}-query">
        <c:out value="${resolvedLabel}" />
      </legend>
      <div class="relative">
        <label
            class="input w-full flex items-center gap-2 cursor-text"
            data-location-trigger>
          <span class="material-symbols-outlined shrink-0 text-outline"><c:out value="${resolvedIcon}" /></span>
          <input
              id="${id}-query"
              type="text"
              value=""
              class="grow bg-transparent outline-none border-none focus:outline-none focus:ring-0 shadow-none p-0"
              placeholder="${fn:escapeXml(resolvedPlaceholder)}"
              autocomplete="off"
              spellcheck="false"
              data-location-query />
          <button
              type="button"
              class="btn btn-ghost btn-xs btn-circle opacity-0 pointer-events-none"
              aria-label="${fn:escapeXml(clearLocationLabel)}"
              data-location-clear>
            <span class="material-symbols-outlined text-base">close</span>
          </button>
          <span class="material-symbols-outlined shrink-0 text-outline text-base transition-transform duration-150" data-location-chevron>expand_more</span>
        </label>

        <div
            class="card bg-base-100 absolute left-0 right-0 top-full z-[10020] mt-2 hidden overflow-hidden shadow-xl"
            data-location-panel
            hidden>
          <div class="border-b border-outline-variant/20 px-4 py-3">
            <p class="m-0 text-[10px] font-bold uppercase tracking-[0.16em] text-outline"><c:out value="${availableLocationsLabel}" /></p>
          </div>
          <div class="h-64 overflow-y-auto p-2" data-location-options></div>
          <p class="hidden px-4 py-4 text-sm text-on-surface-variant" data-location-empty>
            <c:out value="${noLocationsLabel}" />
          </p>
        </div>
      </div>

      <c:if test="${not empty errorPath}">
        <form:errors path="${errorPath}" cssClass="validator-hint text-error text-xs mt-1" element="p" />
      </c:if>
    </fieldset>
  </c:otherwise>
</c:choose>
