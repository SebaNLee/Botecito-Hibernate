<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Send Request</title>
    <link rel="stylesheet" href="<c:url value='/css/components.css' />" />
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>
    <main class="mx-auto flex min-h-screen max-w-3xl items-center justify-center px-6 py-12">
      <section class="w-full max-w-xl rounded-2xl border border-slate-200 bg-white p-8 shadow-lg">
        <div class="mb-8">
          <div class="mb-3 inline-flex rounded-full bg-sky-100 px-3 py-1 text-sm font-semibold text-sky-700">
            Request flow
          </div>
          <paw:heading level="2" text="Send a Request to Botecito" cssClass="mb-3" />
          <p class="m-0 text-base text-slate-600">
            Submit your name, email, and request details. Botecito will receive an approval email and you will get a response after it is accepted or declined.
          </p>
        </div>

        <form
          method="post"
          action="<c:url value='/test-mail' />"
          class="space-y-6"
        >
          <div>
            <label
              for="requesterName"
              class="mb-2 block text-sm font-semibold uppercase tracking-[0.12em] text-slate-700"
            >
              Name
            </label>
            <input
              id="requesterName"
              name="requesterName"
              type="text"
              value="<c:out value='${requesterName}' />"
              placeholder="Jane Doe"
              class="block w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-100"
              required
            />
            <p class="mt-2 text-sm text-slate-500">
              This name will appear in the request email sent to Botecito.
            </p>
          </div>

          <div>
            <label
              for="requesterEmail"
              class="mb-2 block text-sm font-semibold uppercase tracking-[0.12em] text-slate-700"
            >
              Your email
            </label>
            <input
              id="requesterEmail"
              name="requesterEmail"
              type="email"
              value="<c:out value='${requesterEmail}' />"
              placeholder="name@example.com"
              class="block w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-100"
              required
            />
            <p class="mt-2 text-sm text-slate-500">
              Botecito will send the final accepted or declined response to this address.
            </p>
          </div>

          <div>
            <label
              for="description"
              class="mb-2 block text-sm font-semibold uppercase tracking-[0.12em] text-slate-700"
            >
              Description
            </label>
            <textarea
              id="description"
              name="description"
              rows="6"
              placeholder="Describe the request you want Botecito to review."
              class="block w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-base text-slate-900 outline-none transition focus:border-sky-500 focus:bg-white focus:ring-4 focus:ring-sky-100"
              required
            ><c:out value="${description}" /></textarea>
            <p class="mt-2 text-sm text-slate-500">
              Include the context Botecito needs to approve or decline the request.
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
                  Submitting the form will email Botecito a review request with accept and decline links.
                </p>
              </div>
            </c:otherwise>
          </c:choose>

          <div class="flex items-center gap-3">
            <button
              type="submit"
              class="btn btn-primary btn-md rounded-xl px-5 py-3"
            >
              Send request
            </button>
            <span class="text-sm text-slate-500">A POST request will send the review email to Botecito.</span>
          </div>
        </form>
      </section>
    </main>
  </body>
</html>
