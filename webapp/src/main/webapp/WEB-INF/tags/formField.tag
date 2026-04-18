<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="inputType" value="${not empty type ? type : 'text'}" />
<c:set var="inputCssClass" value="input w-full ${not empty cssClass ? cssClass : ''}" />
<c:set var="inputErrorCssClass" value="${inputCssClass} input-error" />
<c:set var="fieldContainerClass" value="fieldset ${not empty containerClass ? containerClass : ''}" />

<fieldset class="${fieldContainerClass}">
  <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
    <c:out value="${label}" />
  </legend>
  <c:choose>
    <c:when test="${inputType == 'password'}">
      <form:password
          id="${path}"
          path="${path}"
          cssClass="${inputCssClass}"
          cssErrorClass="${inputErrorCssClass}"
          placeholder="${placeholder}"
      />
    </c:when>
    <c:otherwise>
      <form:input
          id="${path}"
          path="${path}"
          type="${inputType}"
          cssClass="${inputCssClass}"
          cssErrorClass="${inputErrorCssClass}"
          placeholder="${placeholder}"
      />
    </c:otherwise>
  </c:choose>
  <form:errors path="${path}" cssClass="text-error text-xs mt-1" element="p" />
</fieldset>
