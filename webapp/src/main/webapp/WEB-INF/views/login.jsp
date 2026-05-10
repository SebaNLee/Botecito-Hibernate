<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="loginUrl" value="/login" />
<c:url var="registerUrl" value="/register" />
<c:url var="passwordRecoveryUrl" value="/password-recovery" />
<spring:message code="login.email.placeholder" var="emailPlaceholder" />
<spring:message code="login.password.placeholder" var="passwordPlaceholder" />
<spring:message code="login.email.label" var="emailLabel" />
<spring:message code="login.password.label" var="passwordLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <paw:toastNotifier />
  <div class="w-full max-w-md px-6">
    <div class="card bg-base-100 shadow-sm border border-outline-variant/20">
      <div class="card-body p-8 gap-6">
        <div class="text-center space-y-2">
          <div class="w-14 h-14 bg-primary/10 text-primary rounded-full flex items-center justify-center mx-auto">
            <span class="material-symbols-outlined text-3xl">lock</span>
          </div>
          <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
            <spring:message code="login.heading" />
          </h1>
        </div>

        <c:if test="${loginError}">
          <paw:alertMessage type="error"><spring:message code="login.error" /></paw:alertMessage>
        </c:if>
        <c:if test="${logoutSuccess}">
          <paw:alertMessage type="success"><spring:message code="login.logout" /></paw:alertMessage>
        </c:if>
        <c:if test="${registeredSuccess}">
          <paw:alertMessage type="success"><spring:message code="login.registered" /></paw:alertMessage>
        </c:if>
        <c:if test="${legacyTokenError}">
          <paw:alertMessage type="warning"><spring:message code="login.legacyToken" /></paw:alertMessage>
        </c:if>
        <c:if test="${passwordRecoveredSuccess}">
          <paw:alertMessage type="success"><spring:message code="login.passwordRecovered" /></paw:alertMessage>
        </c:if>

        <form action="${loginUrl}" method="post" class="space-y-4">
          <fieldset class="fieldset">
            <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="j_username">
              <c:out value="${emailLabel}" />
            </legend>
            <input
              id="j_username"
              name="j_username"
              type="email"
              class="input w-full"
              placeholder="${emailPlaceholder}"
              required />
          </fieldset>
          <fieldset class="fieldset">
            <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="j_password">
              <c:out value="${passwordLabel}" />
            </legend>
            <input
              id="j_password"
              name="j_password"
              type="password"
              class="input w-full"
              placeholder="${passwordPlaceholder}"
              required />
          </fieldset>
          <div class="flex items-center gap-2">
            <input type="checkbox" name="j_rememberme" id="j_rememberme" class="checkbox checkbox-primary checkbox-sm" />
            <label for="j_rememberme" class="text-sm text-on-surface-variant cursor-pointer">
              <spring:message code="login.rememberMe" />
            </label>
          </div>
          <spring:message code="login.submit" var="loginSubmitLabel" />
          <paw:button type="submit" fullWidth="true" color="primary" text="${loginSubmitLabel}" />
        </form>

        <div class="text-center">
          <a href="${passwordRecoveryUrl}" class="link link-hover text-sm text-primary font-semibold no-underline">
            <spring:message code="login.forgotPassword" />
          </a>
        </div>

        <p class="text-center text-sm text-on-surface-variant m-0">
          <spring:message code="login.noAccount" />
          <a href="${registerUrl}" class="link link-hover text-primary font-semibold ml-1 no-underline">
            <spring:message code="login.signUpLink" />
          </a>
        </p>
      </div>
    </div>
  </div>
</paw:layout>
