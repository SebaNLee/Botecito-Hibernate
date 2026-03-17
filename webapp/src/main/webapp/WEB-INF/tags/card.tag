<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="price" required="false" %>
<%@ attribute name="date" required="false" %>
<%@ attribute name="imgSrc" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="cardCssClass" value="${not empty cssClass ? cssClass : ''}" />

<div class="card ${cardCssClass}">
    <c:if test="${not empty imgSrc}">
        <img class="card-img" src="${imgSrc}" alt="<c:out value='${name}' />" />
    </c:if>
    <div class="card-body">
        <h3 class="card-title"><c:out value="${name}" /></h3>
        <c:if test="${not empty price}">
            <p class="card-price">$<c:out value="${price}" /></p>
        </c:if>
        <c:if test="${not empty date}">
            <p class="card-date"><c:out value="${date}" /></p>
        </c:if>
    </div>
</div>
