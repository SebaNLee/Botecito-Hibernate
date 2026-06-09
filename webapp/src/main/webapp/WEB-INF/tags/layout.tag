<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="mainClass" required="false" %>
<%@ attribute name="headerCtaMessageCode" required="false" %>
<%@ attribute name="headerCtaHref" required="false" %>
<%@ attribute name="headerCtaVariant" required="false" %>
<%@ attribute name="scripts" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<fmt:setLocale value="${pageContext.response.locale}" scope="request" />
<c:set var="resolvedMainClass" value="${not empty mainClass ? mainClass : 'relative min-h-screen flex flex-col pt-20'}" />
<c:set var="resolvedLang" value="${not empty pageContext.response.locale.language ? pageContext.response.locale.language : 'es'}" />
<c:set var="mainElementClass" value="${resolvedMainClass} flex-1" />

<!DOCTYPE html>
<html class="light" lang="<c:out value='${resolvedLang}' />" data-theme="botecito">
  <head>
    <meta charset="utf-8"/>
    <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
    <title><c:out value="${title}" /></title>
    <link rel="icon" type="image/png" href="<c:url value='/img/favicon.png' />" />
    <link rel="apple-touch-icon" href="<c:url value='/img/favicon.png' />" />
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
    <link rel="stylesheet" href="<c:url value='/css/main.css' />" />
  </head>
  <body class="bg-background text-on-background font-body antialiased min-h-screen flex flex-col">
    <paw:siteHeader
      ctaMessageCode="${headerCtaMessageCode}"
      ctaHref="${headerCtaHref}"
      ctaVariant="${headerCtaVariant}" />
    <main class="<c:out value='${mainElementClass}' />">
      <jsp:doBody />
    </main>
    <paw:siteFooter />
    <paw:pageScripts bundles="${scripts}" />
  </body>
</html>
