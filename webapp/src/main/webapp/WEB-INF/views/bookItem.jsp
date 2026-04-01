<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<html>
<head>
    <title><spring:message code="booking.title" /></title>
</head>
<body>
    <h2><spring:message code="booking.heading" /></h2>
    <form:form modelAttribute="bookForm" action="${pageContext.request.contextPath}/item/${itemId}/book" method="post">
        
        <label><spring:message code="booking.startTime" />:</label>
        <form:input path="startTime" type="datetime-local" />
        <form:errors path="startTime" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="booking.endTime" />:</label>
        <form:input path="endTime" type="datetime-local" />
        <form:errors path="endTime" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="booking.requestMessage" />:</label>
        <form:textarea path="requestMessage" />
        <form:errors path="requestMessage" cssClass="error" element="div"/>
        <br>

        <button type="submit"><spring:message code="booking.submit" /></button>
    </form:form>
</body>
</html>
