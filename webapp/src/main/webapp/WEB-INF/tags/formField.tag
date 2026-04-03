<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="inputType" value="${not empty type ? type : 'text'}" />
<c:set var="inputCssClass" value="form-control ${not empty cssClass ? cssClass : ''}" />

<div>
  <label class="form-label" for="${path}"><c:out value="${label}" /></label>
  <form:input
      id="${path}"
      path="${path}"
      type="${inputType}"
      cssClass="${inputCssClass}"
      placeholder="${placeholder}"
  />
  <form:errors path="${path}" cssClass="field-error" />
</div>
