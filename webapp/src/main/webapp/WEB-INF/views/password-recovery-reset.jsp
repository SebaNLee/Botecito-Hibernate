<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="loginUrl" value="/login" />
<c:url var="resetUrl" value="/password-recovery/${token}" />
<spring:message code="passwordRecovery.reset.password.label" var="passwordLabel" />
<spring:message code="passwordRecovery.reset.password.placeholder" var="passwordPlaceholder" />
<spring:message code="passwordRecovery.reset.confirmPassword.label" var="confirmPasswordLabel" />
<spring:message code="passwordRecovery.reset.confirmPassword.placeholder" var="confirmPasswordPlaceholder" />
<spring:message code="passwordRecovery.reset.submit" var="submitLabel" />
<spring:message code="page.title.passwordReset" var="titlePasswordReset" />

<paw:layout title="${titlePasswordReset} - Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-md px-6">
    <div class="card bg-base-100 shadow-sm border border-outline-variant/20">
      <div class="card-body p-8 gap-6">
        <div class="text-center space-y-2">
          <div class="w-14 h-14 bg-primary/10 text-primary rounded-full flex items-center justify-center mx-auto">
            <span class="material-symbols-outlined text-3xl">password</span>
          </div>
          <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
            <spring:message code="passwordRecovery.reset.heading" />
          </h1>
        </div>

        <c:choose>
          <c:when test="${tokenValid}">
            <c:if test="${tokenInvalidError}">
              <paw:alertMessage type="error"><spring:message code="passwordRecovery.reset.invalid" /></paw:alertMessage>
            </c:if>
            <form:form action="${resetUrl}" method="post" modelAttribute="passwordResetForm" class="space-y-4">
              <paw:formField path="password" type="password" label="${passwordLabel}" placeholder="${passwordPlaceholder}" maxlength="100" />
              <paw:formField path="confirmPassword" type="password" label="${confirmPasswordLabel}" placeholder="${confirmPasswordPlaceholder}" maxlength="100" />
              <form:errors path="passwordConfirmationValid" cssClass="text-error text-xs mt-1" element="p" />
              <paw:button type="submit" fullWidth="true" color="primary" text="${submitLabel}" />
            </form:form>
          </c:when>
          <c:otherwise>
            <paw:alertMessage type="error"><spring:message code="passwordRecovery.reset.invalid" /></paw:alertMessage>
            <p class="text-center text-sm text-on-surface-variant m-0">
              <a href="${loginUrl}" class="link link-hover text-primary font-semibold no-underline">
                <spring:message code="passwordRecovery.request.backToLogin" />
              </a>
            </p>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>
</paw:layout>
