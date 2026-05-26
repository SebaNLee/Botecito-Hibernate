<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="formId" required="true" type="java.lang.String" %>
<%@ attribute name="name" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="ariaLabel" required="false" %>
<%@ attribute name="inputId" required="false" %>
<%@ attribute name="maxlength" required="false" type="java.lang.Integer" %>
<%@ attribute name="extraAttributes" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="resolvedName" value="${not empty name ? name : 'searchQuery'}" />
<c:set var="resolvedValue" value="${not empty value ? value : ''}" />
<c:set var="resolvedAriaLabel" value="${not empty ariaLabel ? ariaLabel : placeholder}" />
<c:set var="resolvedSize" value="${not empty size ? size : 'lg'}" />
<c:set var="inputSizeClass" value="input-${resolvedSize}" />
<c:set var="resolvedCssClass" value="${not empty cssClass ? cssClass : ''}" />

<label class="input ${inputSizeClass} flex w-full items-center gap-3 rounded-full bg-base-100 shadow-sm pr-1.5 ${resolvedCssClass}">
  <span class="material-symbols-outlined text-outline" aria-hidden="true">search</span>
  <input type="search"
    name="${fn:escapeXml(resolvedName)}"
    form="${fn:escapeXml(formId)}"
    value="${fn:escapeXml(resolvedValue)}"
    <c:if test="${not empty placeholder}">placeholder="${fn:escapeXml(placeholder)}"</c:if>
    <c:if test="${not empty resolvedAriaLabel}">aria-label="${fn:escapeXml(resolvedAriaLabel)}"</c:if>
    <c:if test="${not empty inputId}">id="${fn:escapeXml(inputId)}"</c:if>
    <c:if test="${not empty maxlength}">maxlength="${maxlength}"</c:if>
    ${extraAttributes}
    class="min-w-0 grow border-none bg-transparent p-0 text-on-surface placeholder:text-outline outline-none focus:outline-none focus-visible:outline-none focus:ring-0" />
  <button type="submit" form="${fn:escapeXml(formId)}" class="btn btn-primary btn-sm btn-circle shrink-0" aria-label="${fn:escapeXml(not empty resolvedAriaLabel ? resolvedAriaLabel : 'Search')}">
    <span class="material-symbols-outlined">search</span>
  </button>
</label>
