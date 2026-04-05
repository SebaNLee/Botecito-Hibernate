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

<c:set var="resolvedLabel" value="${not empty label ? label : 'Ubicacion'}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedPlaceholder" value="${not empty placeholder ? placeholder : 'Selecciona ubicacion'}" />
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'location_on'}" />
<c:set var="resolvedVariant" value="${not empty variant ? variant : 'default'}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />
<c:set var="resolvedOptionsUrl" value="${not empty optionsUrl ? optionsUrl : '/js/location-options.json'}" />

<c:choose>
  <c:when test="${resolvedVariant == 'inline'}">
    <div
        class="relative w-full ${resolvedContainerClass}"
        data-location-picker
        data-options-url="<c:url value='${resolvedOptionsUrl}' />"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-location-value />
      <div
          class="flex min-h-[3.25rem] w-full items-center gap-2.5 rounded-2xl bg-surface-container-high px-3 py-2.5 transition-all cursor-text shadow-[inset_0_0_0_1px_rgba(113,119,132,0)] hover:shadow-[inset_0_0_0_1px_rgba(113,119,132,0.18)] focus-within:shadow-[inset_0_0_0_2px_rgba(0,93,167,0.2)]"
          data-location-trigger>
        <span class="material-symbols-outlined shrink-0 text-primary"><c:out value="${resolvedIcon}" /></span>
        <div class="min-w-0 flex-1 text-left">
          <label class="block text-[10px] font-bold uppercase tracking-wider text-outline" for="${id}-query">
            <c:out value="${resolvedLabel}" />
          </label>
          <input
              id="${id}-query"
              type="text"
              value="${fn:escapeXml(resolvedValue)}"
              class="w-full appearance-none bg-transparent border-none p-0 text-[0.9rem] text-on-surface placeholder:text-outline-variant font-medium outline-none focus:outline-none focus-visible:outline-none focus:ring-0 shadow-none"
              placeholder="${fn:escapeXml(resolvedPlaceholder)}"
              autocomplete="off"
              spellcheck="false"
              data-location-query />
        </div>
        <button
            type="button"
            class="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container-low hover:text-on-surface cursor-pointer border-none bg-transparent opacity-0 pointer-events-none"
            aria-label="Limpiar ubicacion"
            data-location-clear>
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
        <span class="material-symbols-outlined shrink-0 text-primary text-[18px] transition-transform duration-150" data-location-chevron>expand_more</span>
      </div>

      <div
          class="absolute left-0 right-0 top-full z-[10020] mt-3 hidden overflow-hidden rounded-2xl border border-outline-variant/40 bg-surface-container-lowest shadow-[0_24px_64px_rgba(11,28,50,0.24)]"
          data-location-panel
          hidden>
        <div class="border-b border-outline-variant/15 px-4 py-3">
          <p class="m-0 text-[10px] font-bold uppercase tracking-[0.16em] text-outline">Ubicaciones disponibles</p>
        </div>
        <div class="h-64 overflow-y-auto p-2" data-location-options></div>
        <p class="hidden px-4 py-4 text-sm text-on-surface-variant" data-location-empty>
          No encontramos ubicaciones para esa busqueda.
        </p>
      </div>
    </div>
  </c:when>
  <c:otherwise>
    <div
        class="relative w-full ${resolvedContainerClass}"
        data-location-picker
        data-options-url="<c:url value='${resolvedOptionsUrl}' />"
        data-placeholder="${fn:escapeXml(resolvedPlaceholder)}">
      <input id="${id}" name="${name}" type="hidden" value="${fn:escapeXml(resolvedValue)}" data-location-value />
      <div class="space-y-2">
        <label class="block text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="${id}-query">
          <c:out value="${resolvedLabel}" />
        </label>
        <div
            class="flex min-h-[3.5rem] w-full items-center gap-3 rounded-xl bg-surface-container-high px-4 py-3 transition-all cursor-text shadow-[inset_0_0_0_1px_rgba(113,119,132,0)] hover:shadow-[inset_0_0_0_1px_rgba(113,119,132,0.18)] focus-within:shadow-[inset_0_0_0_2px_rgba(0,93,167,0.2)]"
            data-location-trigger>
          <span class="material-symbols-outlined shrink-0 text-outline"><c:out value="${resolvedIcon}" /></span>
          <input
              id="${id}-query"
              type="text"
              value="${fn:escapeXml(resolvedValue)}"
              class="w-full appearance-none bg-transparent border-none p-0 text-on-surface placeholder:text-outline font-medium outline-none focus:outline-none focus-visible:outline-none focus:ring-0 shadow-none"
              placeholder="${fn:escapeXml(resolvedPlaceholder)}"
              autocomplete="off"
              spellcheck="false"
              data-location-query />
          <button
              type="button"
              class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container hover:text-on-surface cursor-pointer border-none bg-transparent opacity-0 pointer-events-none"
              aria-label="Limpiar ubicacion"
              data-location-clear>
            <span class="material-symbols-outlined text-[18px]">close</span>
          </button>
          <span class="material-symbols-outlined shrink-0 text-outline text-[20px] transition-transform duration-150" data-location-chevron>expand_more</span>
        </div>
      </div>

      <div
          class="absolute left-0 right-0 top-full z-[10020] mt-2 hidden overflow-hidden rounded-2xl border border-outline-variant/40 bg-surface-container-lowest shadow-[0_28px_72px_rgba(11,28,50,0.24)]"
          data-location-panel
          hidden>
        <div class="border-b border-outline-variant/15 px-4 py-3">
          <p class="m-0 text-[10px] font-bold uppercase tracking-[0.16em] text-outline">Ubicaciones disponibles</p>
        </div>
        <div class="h-64 overflow-y-auto p-2" data-location-options></div>
        <p class="hidden px-4 py-4 text-sm text-on-surface-variant" data-location-empty>
          No encontramos ubicaciones para esa busqueda.
        </p>
      </div>

      <c:if test="${not empty errorPath}">
        <form:errors path="${errorPath}" cssClass="text-error text-xs mt-1 block" />
      </c:if>
    </div>
  </c:otherwise>
</c:choose>
