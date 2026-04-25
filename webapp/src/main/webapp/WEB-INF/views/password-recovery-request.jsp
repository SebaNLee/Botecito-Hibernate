<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="passwordRecoveryUrl" value="/password-recovery" />
<c:url var="loginUrl" value="/login" />
<spring:message code="passwordRecovery.request.email.label" var="emailLabel" />
<spring:message code="passwordRecovery.request.email.placeholder" var="emailPlaceholder" />
<spring:message code="passwordRecovery.request.submit" var="submitLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-md px-6">
    <div class="card bg-base-100 shadow-sm border border-outline-variant/20">
      <div class="card-body p-8 gap-6">
        <div class="text-center space-y-2">
          <div class="w-14 h-14 bg-primary/10 text-primary rounded-full flex items-center justify-center mx-auto">
            <span class="material-symbols-outlined text-3xl">mail</span>
          </div>
          <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
            <spring:message code="passwordRecovery.request.heading" />
          </h1>
          <p class="m-0 text-sm text-on-surface-variant">
            <spring:message code="passwordRecovery.request.description" />
          </p>
        </div>

        <c:if test="${recoverySent}">
          <paw:alertMessage type="success"><spring:message code="passwordRecovery.request.sent" /></paw:alertMessage>
        </c:if>

        <form:form action="${passwordRecoveryUrl}" method="post" modelAttribute="passwordRecoveryRequestForm" class="space-y-4">
          <paw:formField path="email" type="email" label="${emailLabel}" placeholder="${emailPlaceholder}" />
          <paw:button type="submit" fullWidth="true" color="primary" text="${submitLabel}" />
        </form:form>

        <p class="text-center text-sm text-on-surface-variant m-0">
          <a href="${loginUrl}" class="link link-hover text-primary font-semibold no-underline">
            <spring:message code="passwordRecovery.request.backToLogin" />
          </a>
        </p>
      </div>
    </div>
  </div>
</paw:layout>
