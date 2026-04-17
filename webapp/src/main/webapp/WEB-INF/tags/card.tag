<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="price" required="false" %>
<%@ attribute name="date" required="false" %>
<%@ attribute name="imgSrc" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="cardCssClass" value="${not empty cssClass ? cssClass : ''}" />

<div class="card bg-base-100 shadow-sm max-w-xs ${cardCssClass}">
    <c:if test="${not empty imgSrc}">
        <figure class="h-44 overflow-hidden">
            <img src="${imgSrc}" alt="<c:out value='${name}' />" class="w-full h-full object-cover" />
        </figure>
    </c:if>
    <div class="card-body p-5 gap-1">
        <h3 class="card-title text-xl font-semibold m-0"><c:out value="${name}" /></h3>
        <c:if test="${not empty price}">
            <p class="text-lg font-bold text-success m-0">$<c:out value="${price}" /></p>
        </c:if>
        <c:if test="${not empty date}">
            <p class="text-sm text-base-content/60 m-0"><c:out value="${date}" /></p>
        </c:if>
    </div>
</div>
