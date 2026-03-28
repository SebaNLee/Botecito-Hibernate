<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Request Result</title>
    <link rel="stylesheet" href="<c:url value='/css/components.css' />" />
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>
    <main class="mx-auto flex min-h-screen max-w-3xl items-center justify-center px-6 py-12">
      <section class="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-8 shadow-lg">
        <div class="mb-6 inline-flex rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-700">
          Request action
        </div>
        <paw:heading level="2" text="${actionTitle}" cssClass="mb-3" />
        <p class="m-0 text-base text-slate-600">
          <c:out value="${actionMessage}" />
        </p>
      </section>
    </main>
  </body>
</html>
