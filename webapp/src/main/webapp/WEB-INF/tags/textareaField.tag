<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="rows" required="false" %>
<%@ attribute name="maxlength" required="false" %>
<%@ attribute name="required" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="textareaCssClass" value="textarea w-full resize-none ${not empty cssClass ? cssClass : ''}" />
<c:set var="textareaErrorCssClass" value="${textareaCssClass} textarea-error" />
<c:set var="fieldContainerClass" value="fieldset ${not empty containerClass ? containerClass : ''}" />
<c:set var="resolvedRows" value="${not empty rows ? rows : '4'}" />

<fieldset class="${fieldContainerClass}">
  <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
    <c:out value="${label}" />
    <c:if test="${required}"><span class="text-error" aria-hidden="true">*</span></c:if>
  </legend>
  <c:choose>
    <c:when test="${not empty maxlength}">
      <form:textarea
          id="${path}"
          path="${path}"
          rows="${resolvedRows}"
          maxlength="${maxlength}"
          cssClass="${textareaCssClass}"
          cssErrorClass="${textareaErrorCssClass}"
          placeholder="${placeholder}"
      />
    </c:when>
    <c:otherwise>
      <form:textarea
          id="${path}"
          path="${path}"
          rows="${resolvedRows}"
          cssClass="${textareaCssClass}"
          cssErrorClass="${textareaErrorCssClass}"
          placeholder="${placeholder}"
      />
    </c:otherwise>
  </c:choose>
  <form:errors path="${path}" cssClass="text-error text-xs mt-1" element="p" />
</fieldset>
