<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="text" required="true" %>
<%@ attribute name="color" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="btnColor" value="${not empty color ? color : 'primary'}" />
<c:set var="btnSize" value="${not empty size ? size : 'md'}" />
<c:set var="btnCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="btnDisabled" value="${disabled ne null ? disabled : false}" />
<c:set var="btnBaseClass" value="inline-block rounded border font-medium leading-6 text-center align-middle transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed" />

<c:choose>
    <c:when test="${btnColor == 'secondary'}">
        <c:set var="btnColorClass" value="text-white bg-gray-600 border-gray-600 hover:bg-gray-700 focus:ring-gray-300" />
    </c:when>
    <c:when test="${btnColor == 'success'}">
        <c:set var="btnColorClass" value="text-white bg-green-600 border-green-600 hover:bg-green-700 focus:ring-green-300" />
    </c:when>
    <c:when test="${btnColor == 'danger'}">
        <c:set var="btnColorClass" value="text-white bg-red-600 border-red-600 hover:bg-red-700 focus:ring-red-300" />
    </c:when>
    <c:when test="${btnColor == 'warning'}">
        <c:set var="btnColorClass" value="text-black bg-amber-400 border-amber-400 hover:bg-amber-500 focus:ring-amber-300" />
    </c:when>
    <c:when test="${btnColor == 'outline'}">
        <c:set var="btnColorClass" value="text-blue-600 bg-transparent border-blue-600 hover:bg-blue-600 hover:text-white focus:ring-blue-300" />
    </c:when>
    <c:otherwise>
        <c:set var="btnColorClass" value="text-white bg-blue-600 border-blue-600 hover:bg-blue-700 focus:ring-blue-300" />
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${btnSize == 'sm'}">
        <c:set var="btnSizeClass" value="px-2 py-1 text-sm" />
    </c:when>
    <c:when test="${btnSize == 'lg'}">
        <c:set var="btnSizeClass" value="px-4 py-2 text-xl" />
    </c:when>
    <c:otherwise>
        <c:set var="btnSizeClass" value="px-3 py-1.5 text-base" />
    </c:otherwise>
</c:choose>

<button type="button"
        class="${btnBaseClass} ${btnColorClass} ${btnSizeClass} ${btnCssClass}"
        <c:if test="${btnDisabled}">disabled</c:if>
>
    <c:out value="${text}" />
</button>
