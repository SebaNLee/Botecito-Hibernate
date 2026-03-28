<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Mail Test UI</title>
    <link rel="stylesheet" href="<c:url value='/css/components.css' />" />
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>
    <main class="mx-auto flex min-h-screen max-w-3xl items-center justify-center px-6 py-12">
      <section class="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-8 shadow-lg">
        <div class="mb-8">
          <div class="mb-3 inline-flex rounded-full bg-sky-100 px-3 py-1 text-sm font-semibold text-sky-700">
            Test flow
          </div>
          <paw:heading level="2" text="Email Confirmation Test" cssClass="mb-3" />
          <p class="m-0 text-base text-slate-600">
            Enter an email and press the button to test the Gmail confirmation flow.
          </p>
        </div>

        <form
          method="post"
          action="<c:url value='/test-mail' />"
          class="space-y-6"
        >
          <div>
            <label
              for="email"
              class="mb-2 block text-sm font-semibold uppercase tracking-[0.12em] text-slate-700"
            >
              Email
            </label>
            <input
              id="email"
              name="email"
              type="email"
              value="<c:out value='${email}' />"
              placeholder="name@example.com"
              class="block w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-100"
              required
            />
            <p class="mt-2 text-sm text-slate-500">
              The test confirmation mail will be sent to this address.
            </p>
          </div>

          <c:choose>
            <c:when test="${not empty mailSuccess}">
              <div class="rounded-xl border border-emerald-200 bg-emerald-50 p-4">
                <p class="m-0 text-sm font-semibold text-emerald-800">Success</p>
                <p class="mt-1 mb-0 text-sm text-emerald-700">
                  <c:out value="${mailSuccess}" />
                </p>
              </div>
            </c:when>
            <c:when test="${not empty mailError}">
              <div class="rounded-xl border border-rose-200 bg-rose-50 p-4">
                <p class="m-0 text-sm font-semibold text-rose-800">Error</p>
                <p class="mt-1 mb-0 text-sm text-rose-700">
                  <c:out value="${mailError}" />
                </p>
              </div>
            </c:when>
            <c:otherwise>
              <div class="rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4">
                <p class="m-0 text-sm font-semibold text-slate-700">Ready to send</p>
                <p class="mt-1 mb-0 text-sm text-slate-500">
                  Submit the form to send a test email through Gmail SMTP.
                </p>
              </div>
            </c:otherwise>
          </c:choose>

          <div class="flex items-center gap-3">
            <button
              type="submit"
              class="btn btn-primary btn-md rounded-xl px-5 py-3"
            >
              Send test email
            </button>
            <span class="text-sm text-slate-500">A POST request will be sent to this page.</span>
          </div>
        </form>
      </section>
    </main>
  </body>
</html>
