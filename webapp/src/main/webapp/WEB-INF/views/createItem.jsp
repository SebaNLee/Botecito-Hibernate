<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<html>
<head>
    <title><spring:message code="item.create.title" /></title>
</head>
<body>
    <h2><spring:message code="item.create.heading" /></h2>
    <form:form modelAttribute="createForm" action="${pageContext.request.contextPath}/item/create" method="post">
        
        <label><spring:message code="item.typeId" />:</label>
        <form:input path="typeId" />
        <form:errors path="typeId" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.title" />:</label>
        <form:input path="title" />
        <form:errors path="title" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.description" />:</label>
        <form:textarea path="description" />
        <form:errors path="description" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.pricePerHour" />:</label>
        <form:input path="pricePerHour" />
        <form:errors path="pricePerHour" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.capacityPeople" />:</label>
        <form:input path="capacityPeople" />
        <form:errors path="capacityPeople" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.maxWeightKg" />:</label>
        <form:input path="maxWeightKg" />
        <form:errors path="maxWeightKg" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.difficultyLevel" />:</label>
        <form:input path="difficultyLevel" />
        <form:errors path="difficultyLevel" cssClass="error" element="div"/>
        <br>

        <label><spring:message code="item.location" />:</label>
        <form:input path="location" />
        <form:errors path="location" cssClass="error" element="div"/>
        <br>

        <button type="submit"><spring:message code="item.create.submit" /></button>
    </form:form>
</body>
</html>
