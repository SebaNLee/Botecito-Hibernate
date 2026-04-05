<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="min" required="false" %>
<%@ attribute name="max" required="false" %>
<%@ attribute name="step" required="false" %>
<%@ attribute name="unit" required="false" %>
<%@ attribute name="helper" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="resolvedLabel" value="${not empty label ? label : 'Peso a soportar'}" />
<c:set var="resolvedMin" value="${not empty min ? min : '100'}" />
<c:set var="resolvedMax" value="${not empty max ? max : '2000'}" />
<c:set var="resolvedStep" value="${not empty step ? step : '50'}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedDisplayValue" value="${not empty value ? value : resolvedMin}" />
<c:set var="resolvedUnit" value="${not empty unit ? unit : 'kg'}" />
<c:set var="resolvedHelper" value="${not empty helper ? helper : 'Mostramos botes que soportan este peso o mas.'}" />
<c:set var="resolvedContainerClass" value="${not empty containerClass ? containerClass : ''}" />

<div
    class="space-y-4 ${resolvedContainerClass}"
    data-weight-slider
    data-unit="${fn:escapeXml(resolvedUnit)}">
  <div class="flex items-center justify-between gap-4">
    <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="${id}-range">
      <c:out value="${resolvedLabel}" />
    </label>
    <span class="text-sm font-bold text-primary" data-weight-value>
      <c:out value="${resolvedDisplayValue}" /> <c:out value="${resolvedUnit}" />
    </span>
  </div>

  <input id="${id}" name="${name}" type="hidden" value="${resolvedValue}" data-weight-value-input />

  <input
      id="${id}-range"
      type="range"
      min="${resolvedMin}"
      max="${resolvedMax}"
      step="${resolvedStep}"
      value="${resolvedDisplayValue}"
      class="w-full h-1.5 bg-surface-container-highest rounded-lg appearance-none cursor-pointer accent-primary"
      data-weight-input />

  <div class="flex justify-between text-[10px] font-medium text-outline">
    <span><c:out value="${resolvedMin}" /><c:out value="${resolvedUnit}" /></span>
    <span><c:out value="${resolvedMax}" /><c:out value="${resolvedUnit}" /></span>
  </div>

  <p class="m-0 text-xs leading-relaxed text-on-surface-variant">
    <c:out value="${resolvedHelper}" />
  </p>
</div>
