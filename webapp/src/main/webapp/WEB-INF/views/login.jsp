<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="loginUrl" value="/login" />
<c:url var="registerUrl" value="/register" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-md px-6">
    <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
      <div class="text-center space-y-2">
        <div class="w-14 h-14 bg-primary/10 text-primary rounded-full flex items-center justify-center mx-auto">
          <span class="material-symbols-outlined text-3xl">lock</span>
        </div>
        <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
          <spring:message code="login.heading" />
        </h1>
      </div>

      <c:if test="${loginError}">
        <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700 flex items-start gap-3">
          <span class="material-symbols-outlined text-red-500 text-lg mt-0.5">error</span>
          <span><spring:message code="login.error" /></span>
        </div>
      </c:if>

      <c:if test="${logoutSuccess}">
        <div class="rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm font-medium text-green-700 flex items-start gap-3">
          <span class="material-symbols-outlined text-green-500 text-lg mt-0.5">check_circle</span>
          <span><spring:message code="login.logout" /></span>
        </div>
      </c:if>

      <c:if test="${registeredSuccess}">
        <div class="rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm font-medium text-green-700 flex items-start gap-3">
          <span class="material-symbols-outlined text-green-500 text-lg mt-0.5">check_circle</span>
          <span><spring:message code="login.registered" /></span>
        </div>
      </c:if>

      <c:if test="${legacyTokenError}">
        <div class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800 flex items-start gap-3">
          <span class="material-symbols-outlined text-amber-600 text-lg mt-0.5">warning</span>
          <span><spring:message code="login.legacyToken" /></span>
        </div>
      </c:if>

      <form action="${loginUrl}" method="post" class="space-y-5">
        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="j_username">
            <spring:message code="login.email.label" />
          </label>
          <input
            id="j_username"
            name="j_username"
            type="email"
            class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
            placeholder="<spring:message code='login.email.placeholder' />"
            required />
        </div>

        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="j_password">
            <spring:message code="login.password.label" />
          </label>
          <input
            id="j_password"
            name="j_password"
            type="password"
            class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
            placeholder="<spring:message code='login.password.placeholder' />"
            required />
        </div>

        <div class="flex items-center gap-2">
          <input type="checkbox" name="j_rememberme" id="j_rememberme" class="rounded text-primary focus:ring-primary/20" />
          <label for="j_rememberme" class="text-sm text-on-surface-variant">
            <spring:message code="login.rememberMe" />
          </label>
        </div>

        <button
          type="submit"
          class="w-full px-6 py-3 bg-primary text-on-primary font-bold rounded-xl shadow-lg shadow-primary/20 hover:bg-primary-container transition-all active:scale-[0.98] border-none cursor-pointer text-sm">
          <spring:message code="login.submit" />
        </button>
      </form>

      <p class="text-center text-sm text-on-surface-variant m-0">
        <spring:message code="login.noAccount" />
        <a href="${registerUrl}" class="text-primary font-semibold hover:opacity-80 transition-opacity no-underline ml-1">
          <spring:message code="login.signUpLink" />
        </a>
      </p>
    </div>
  </div>
</paw:layout>
