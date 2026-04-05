<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="mainClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<c:set var="resolvedMainClass" value="${not empty mainClass ? mainClass : 'relative min-h-screen flex flex-col pt-20'}" />

<!DOCTYPE html>
<html class="light" lang="es">
  <head>
    <meta charset="utf-8"/>
    <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
    <title><c:out value="${title}" /></title>
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
    <link rel="stylesheet" href="<c:url value='/css/main.css' />" />
  </head>
  <body class="bg-background text-on-background font-body antialiased min-h-screen">
    <paw:siteHeader />
    <main class="${resolvedMainClass}">
      <jsp:doBody />
    </main>
    <paw:siteFooter />
    <script src="<c:url value='/js/date-time-picker.js' />"></script>
  </body>
</html>
