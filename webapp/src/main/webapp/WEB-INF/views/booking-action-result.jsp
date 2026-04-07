<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:if test="${itemId != null}">
  <c:url var="itemUrl" value="/item/${itemId}" />
</c:if>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title><spring:message code="requestAction.pageTitle" /></title>
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
    <link rel="stylesheet" href="<c:url value='/css/main.css' />" />
  </head>
  <body>
    <main class="mx-auto flex min-h-screen max-w-3xl items-center justify-center px-6 py-12">
      <section class="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-8 shadow-lg">
        <div class="mb-6 inline-flex rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-700">
          <spring:message code="requestAction.badge" />
        </div>
        <paw:heading level="2" text="${actionTitle}" cssClass="mb-3" />
        <p class="m-0 text-base text-slate-600">
          <c:out value="${actionMessage}" />
        </p>
        <c:if test="${itemId != null}">
          <a href="${itemUrl}" class="mt-6 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 font-bold text-on-primary no-underline">
            Check out the publication
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </a>
        </c:if>
      </section>
    </main>
  </body>
</html>
