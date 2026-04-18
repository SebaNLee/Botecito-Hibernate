<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="options" required="true" type="java.util.Map" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="selectCssClass" value="select w-full ${not empty cssClass ? cssClass : ''}" />
<c:set var="selectErrorCssClass" value="${selectCssClass} select-error" />
<c:set var="fieldContainerClass" value="fieldset ${not empty containerClass ? containerClass : ''}" />

<fieldset class="${fieldContainerClass}">
  <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
    <c:out value="${label}" />
  </legend>
  <form:select id="${path}" path="${path}" cssClass="${selectCssClass}" cssErrorClass="${selectErrorCssClass}">
    <c:if test="${not empty placeholder}">
      <form:option value="" label="${placeholder}" />
    </c:if>
    <c:forEach var="entry" items="${options}">
      <form:option value="${entry.key}" label="${entry.value}" />
    </c:forEach>
  </form:select>
  <form:errors path="${path}" cssClass="text-error text-xs mt-1" element="p" />
</fieldset>
