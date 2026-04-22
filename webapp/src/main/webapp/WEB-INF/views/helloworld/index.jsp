<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="${not empty pageContext.response.locale.language ? pageContext.response.locale.language : 'es'}">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title><spring:message code="app.title" text="Botecito" /></title>
    <link rel="icon" type="image/png" href="<c:url value='/favicon.png' />" />
    <link rel="apple-touch-icon" href="<c:url value='/favicon.png' />" />
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>

    <h2>
        <c:out value="${message}" />
    </h2>

  </body>
</html>
