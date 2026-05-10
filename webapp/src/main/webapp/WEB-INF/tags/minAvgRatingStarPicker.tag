<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="clearLabel" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="resolvedValue" value="${not empty value ? value : ''}" />

<fieldset
    class="fieldset space-y-2"
    data-min-rating-picker
    aria-label="${fn:escapeXml(label)}">
  <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant mb-1 block">
    <c:out value="${label}" />
  </legend>

  <input
      type="hidden"
      id="${id}"
      name="${name}"
      value="${fn:escapeXml(resolvedValue)}"
      data-min-rating-value-input />

  <div class="flex flex-wrap items-center gap-3">
    <div
        class="flex items-center gap-0.5 select-none"
        role="group"
        data-min-rating-stars
        aria-hidden="true">
      <c:forEach begin="1" end="5" var="star">
        <c:set var="leftStep" value="${star - 0.5}" />
        <span class="relative inline-flex h-9 w-9 shrink-0 items-center justify-center">
          <span
              class="pointer-events-none material-symbols-outlined text-3xl leading-none text-outline opacity-35 transition-colors duration-150"
              data-min-rating-icon="${star}">star</span>
          <button
              type="button"
              tabindex="-1"
              class="absolute inset-y-0 left-0 z-10 w-1/2 cursor-pointer rounded-l-md border-0 bg-transparent p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
              data-min-rating-step="${leftStep}"
              title="${leftStep}"></button>
          <button
              type="button"
              tabindex="-1"
              class="absolute inset-y-0 right-0 z-10 w-1/2 cursor-pointer rounded-r-md border-0 bg-transparent p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
              data-min-rating-step="${star}"
              title="${star}"></button>
        </span>
      </c:forEach>
    </div>

    <button
        type="button"
        class="btn btn-ghost btn-xs font-semibold text-on-surface-variant normal-case"
        data-min-rating-clear>
      <c:out value="${clearLabel}" />
    </button>
  </div>
</fieldset>
