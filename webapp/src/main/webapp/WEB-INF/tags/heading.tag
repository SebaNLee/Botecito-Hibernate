<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="level" required="true" %>
<%@ attribute name="text" required="true" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="headingClass" value="heading heading-${level} ${not empty cssClass ? cssClass : ''}" />

<c:choose>
    <c:when test="${level == 1}">
        <h1 class="${headingClass}"><c:out value="${text}" /></h1>
    </c:when>
    <c:when test="${level == 2}">
        <h2 class="${headingClass}"><c:out value="${text}" /></h2>
    </c:when>
    <c:when test="${level == 3}">
        <h3 class="${headingClass}"><c:out value="${text}" /></h3>
    </c:when>
    <c:when test="${level == 4}">
        <h4 class="${headingClass}"><c:out value="${text}" /></h4>
    </c:when>
    <c:when test="${level == 5}">
        <h5 class="${headingClass}"><c:out value="${text}" /></h5>
    </c:when>
    <c:when test="${level == 6}">
        <h6 class="${headingClass}"><c:out value="${text}" /></h6>
    </c:when>
    <c:otherwise>
        <h1 class="${headingClass}"><c:out value="${text}" /></h1>
    </c:otherwise>
</c:choose>
