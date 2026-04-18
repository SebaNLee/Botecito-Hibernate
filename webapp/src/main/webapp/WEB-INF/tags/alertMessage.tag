<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="message" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="resolvedType" value="${not empty type ? type : 'info'}" />
<c:set var="extraClass" value="${not empty cssClass ? cssClass : ''}" />

<c:choose>
  <c:when test="${resolvedType == 'error'}">
    <c:set var="variantClass" value="alert-error" />
    <c:set var="defaultIcon" value="error" />
  </c:when>
  <c:when test="${resolvedType == 'success'}">
    <c:set var="variantClass" value="alert-success" />
    <c:set var="defaultIcon" value="check_circle" />
  </c:when>
  <c:when test="${resolvedType == 'warning'}">
    <c:set var="variantClass" value="alert-warning" />
    <c:set var="defaultIcon" value="warning" />
  </c:when>
  <c:otherwise>
    <c:set var="variantClass" value="alert-info" />
    <c:set var="defaultIcon" value="info" />
  </c:otherwise>
</c:choose>
<c:set var="resolvedIcon" value="${not empty icon ? icon : defaultIcon}" />

<div role="alert" class="alert ${variantClass} alert-soft rounded-xl text-sm items-start ${extraClass}">
  <c:if test="${not empty resolvedIcon}">
    <span class="material-symbols-outlined text-lg shrink-0"><c:out value="${resolvedIcon}" /></span>
  </c:if>
  <span>
    <c:choose>
      <c:when test="${not empty message}"><c:out value="${message}" /></c:when>
      <c:otherwise><jsp:doBody /></c:otherwise>
    </c:choose>
  </span>
</div>
