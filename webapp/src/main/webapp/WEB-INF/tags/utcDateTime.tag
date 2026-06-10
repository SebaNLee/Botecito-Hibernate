<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="format" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:if test="${not empty value}">
  <c:set var="utcValue" value="${fn:substring(value, 0, 19)}" />
  <time
      datetime="${utcValue}Z"
      data-utc-datetime="${utcValue}"
      data-utc-format="${empty format ? 'date' : format}"
      <c:if test="${not empty cssClass}">class="<c:out value='${cssClass}' />"</c:if>></time>
</c:if>
