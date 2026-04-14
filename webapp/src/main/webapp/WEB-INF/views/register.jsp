<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="registerUrl" value="/register" />
<c:url var="loginUrl" value="/login" />
<spring:message code="register.givenName.placeholder" var="givenNamePlaceholder" />
<spring:message code="register.lastName.placeholder" var="lastNamePlaceholder" />
<spring:message code="register.email.placeholder" var="emailPlaceholder" />
<spring:message code="register.password.placeholder" var="passwordPlaceholder" />
<spring:message code="register.confirmPassword.placeholder" var="confirmPasswordPlaceholder" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-lg px-6">
    <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
      <div class="text-center space-y-2">
        <div class="w-14 h-14 bg-primary/10 text-primary rounded-full flex items-center justify-center mx-auto">
          <span class="material-symbols-outlined text-3xl">person_add</span>
        </div>
        <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
          <spring:message code="register.heading" />
        </h1>
      </div>

      <spring:hasBindErrors name="registerForm">
        <c:if test="${not empty errors.globalErrors}">
          <c:forEach var="error" items="${errors.globalErrors}">
            <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700 flex items-start gap-3">
              <span class="material-symbols-outlined text-red-500 text-lg mt-0.5">error</span>
              <span><c:out value="${error.defaultMessage}" /></span>
            </div>
          </c:forEach>
        </c:if>
      </spring:hasBindErrors>

      <form:form action="${registerUrl}" method="post" modelAttribute="registerForm" class="space-y-5">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="givenName">
              <spring:message code="register.givenName.label" />
            </label>
            <form:input
              path="givenName"
              id="givenName"
              cssClass="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
              placeholder="${givenNamePlaceholder}" />
            <form:errors path="givenName" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="lastName">
              <spring:message code="register.lastName.label" />
            </label>
            <form:input
              path="lastName"
              id="lastName"
              cssClass="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
              placeholder="${lastNamePlaceholder}" />
            <form:errors path="lastName" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>
        </div>

        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="email">
            <spring:message code="register.email.label" />
          </label>
          <form:input
            path="email"
            id="email"
            type="email"
            cssClass="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
            placeholder="${emailPlaceholder}" />
          <form:errors path="email" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
        </div>

        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="password">
            <spring:message code="register.password.label" />
          </label>
          <form:password
            path="password"
            id="password"
            cssClass="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
            placeholder="${passwordPlaceholder}" />
          <form:errors path="password" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
        </div>

        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="confirmPassword">
            <spring:message code="register.confirmPassword.label" />
          </label>
          <form:password
            path="confirmPassword"
            id="confirmPassword"
            cssClass="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline"
            placeholder="${confirmPasswordPlaceholder}" />
          <form:errors path="confirmPassword" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
        </div>

        <button
          type="submit"
          class="w-full px-6 py-3 bg-primary text-on-primary font-bold rounded-xl shadow-lg shadow-primary/20 hover:bg-primary-container transition-all active:scale-[0.98] border-none cursor-pointer text-sm">
          <spring:message code="register.submit" />
        </button>
      </form:form>

      <p class="text-center text-sm text-on-surface-variant m-0">
        <spring:message code="register.hasAccount" />
        <a href="${loginUrl}" class="text-primary font-semibold hover:opacity-80 transition-opacity no-underline ml-1">
          <spring:message code="register.loginLink" />
        </a>
      </p>
    </div>
  </div>
</paw:layout>
