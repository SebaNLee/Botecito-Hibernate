<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="level" required="true" %>
<%@ attribute name="text" required="true" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="headingCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="headingBaseClass" value="mb-2 mt-0 font-semibold leading-tight" />

<c:choose>
    <c:when test="${level == 1}">
        <c:set var="headingSizeClass" value="text-4xl" />
    </c:when>
    <c:when test="${level == 2}">
        <c:set var="headingSizeClass" value="text-3xl" />
    </c:when>
    <c:when test="${level == 3}">
        <c:set var="headingSizeClass" value="text-2xl" />
    </c:when>
    <c:when test="${level == 4}">
        <c:set var="headingSizeClass" value="text-xl" />
    </c:when>
    <c:when test="${level == 5}">
        <c:set var="headingSizeClass" value="text-lg" />
    </c:when>
    <c:otherwise>
        <c:set var="headingSizeClass" value="text-base" />
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${level == 1}">
        <h1 class="${headingBaseClass} ${headingSizeClass} ${headingCssClass}"><c:out value="${text}" /></h1>
    </c:when>
    <c:when test="${level == 2}">
        <h2 class="${headingBaseClass} ${headingSizeClass} ${headingCssClass}"><c:out value="${text}" /></h2>
    </c:when>
    <c:when test="${level == 3}">
        <h3 class="${headingBaseClass} ${headingSizeClass} ${headingCssClass}"><c:out value="${text}" /></h3>
    </c:when>
    <c:when test="${level == 4}">
        <h4 class="${headingBaseClass} ${headingSizeClass} ${headingCssClass}"><c:out value="${text}" /></h4>
    </c:when>
    <c:when test="${level == 5}">
        <h5 class="${headingBaseClass} ${headingSizeClass} ${headingCssClass}"><c:out value="${text}" /></h5>
    </c:when>
    <c:when test="${level == 6}">
        <h6 class="${headingBaseClass} ${headingSizeClass} ${headingCssClass}"><c:out value="${text}" /></h6>
    </c:when>
    <c:otherwise>
        <h1 class="${headingBaseClass} text-4xl ${headingCssClass}"><c:out value="${text}" /></h1>
    </c:otherwise>
</c:choose>
