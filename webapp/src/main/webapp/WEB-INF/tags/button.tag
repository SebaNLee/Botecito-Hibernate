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
<c:set var="classes" value="btn btn-${btnColor} btn-${btnSize} ${btnCssClass}" />

<button type="button"
        class="${classes}"
        <c:if test="${btnDisabled}">disabled</c:if>
>
    <c:out value="${text}" />
</button>
