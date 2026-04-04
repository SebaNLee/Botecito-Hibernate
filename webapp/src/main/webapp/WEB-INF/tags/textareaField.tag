<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="textareaCssClass" value="form-textarea ${not empty cssClass ? cssClass : ''}" />

<div>
  <label class="form-label" for="${path}"><c:out value="${label}" /></label>
  <form:textarea
      id="${path}"
      path="${path}"
      cssClass="${textareaCssClass}"
      placeholder="${placeholder}"
  />
  <form:errors path="${path}" cssClass="field-error" />
</div>
