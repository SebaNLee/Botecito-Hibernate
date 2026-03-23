<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="price" required="false" %>
<%@ attribute name="date" required="false" %>
<%@ attribute name="imgSrc" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="cardCssClass" value="${not empty cssClass ? cssClass : ''}" />

<div class="max-w-xs overflow-hidden rounded-lg border border-black/15 bg-white shadow ${cardCssClass}">
    <c:if test="${not empty imgSrc}">
        <img src="${imgSrc}" alt="<c:out value='${name}' />" class="block h-44 w-full object-cover" />
    </c:if>
    <div class="px-5 py-4">
        <h3 class="mb-2 text-xl font-semibold"><c:out value="${name}" /></h3>
        <c:if test="${not empty price}">
            <p class="mb-1 text-lg font-bold text-green-600">$<c:out value="${price}" /></p>
        </c:if>
        <c:if test="${not empty date}">
            <p class="text-sm opacity-50"><c:out value="${date}" /></p>
        </c:if>
    </div>
</div>
