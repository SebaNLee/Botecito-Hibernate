<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="options" required="true" type="java.util.Map" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="selectCssClass" value="form-select ${not empty cssClass ? cssClass : ''}" />

<div>
  <label class="form-label" for="${path}"><c:out value="${label}" /></label>
  <form:select id="${path}" path="${path}" cssClass="${selectCssClass}">
    <c:if test="${not empty placeholder}">
      <form:option value="" label="${placeholder}" />
    </c:if>
    <c:forEach var="entry" items="${options}">
      <form:option value="${entry.key}" label="${entry.value}" />
    </c:forEach>
  </form:select>
  <form:errors path="${path}" cssClass="field-error" />
</div>
