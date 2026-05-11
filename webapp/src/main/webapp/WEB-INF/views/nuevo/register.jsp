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
<spring:message code="register.paymentAlias.placeholder" var="paymentAliasPlaceholder" />
<spring:message code="register.password.placeholder" var="passwordPlaceholder" />
<spring:message code="register.confirmPassword.placeholder" var="confirmPasswordPlaceholder" />
<spring:message code="register.givenName.label" var="givenNameLabel" />
<spring:message code="register.lastName.label" var="lastNameLabel" />
<spring:message code="register.email.label" var="emailLabel" />
<spring:message code="register.paymentAlias.label" var="paymentAliasLabel" />
<spring:message code="register.password.label" var="passwordLabel" />
<spring:message code="register.confirmPassword.label" var="confirmPasswordLabel" />
<spring:message code="register.submit" var="registerSubmitLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-lg px-6">
    <div class="card bg-base-100 shadow-sm border border-outline-variant/20">
      <div class="card-body p-8 gap-6">
        <div class="text-center space-y-2">
          <div class="w-14 h-14 bg-primary/10 text-primary rounded-full flex items-center justify-center mx-auto">
            <span class="material-symbols-outlined text-3xl">person_add</span>
          </div>
          <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
            <spring:message code="register.heading" />
          </h1>
        </div>

        <spring:hasBindErrors name="registerForm">
          <c:forEach var="error" items="${errors.globalErrors}">
            <paw:alertMessage type="error">
              <spring:message code="${error.codes[0]}" arguments="${error.arguments}" />
            </paw:alertMessage>
          </c:forEach>
        </spring:hasBindErrors>

        <form:form action="${registerUrl}" method="post" modelAttribute="registerForm" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <paw:formField path="givenName" label="${givenNameLabel}" placeholder="${givenNamePlaceholder}" />
            <paw:formField path="lastName" label="${lastNameLabel}" placeholder="${lastNamePlaceholder}" />
          </div>
          <paw:formField path="email" type="email" label="${emailLabel}" placeholder="${emailPlaceholder}" />
          <paw:formField path="paymentAlias" label="${paymentAliasLabel}" placeholder="${paymentAliasPlaceholder}" />
          <paw:formField path="password" type="password" label="${passwordLabel}" placeholder="${passwordPlaceholder}" />
          <paw:formField path="confirmPassword" type="password" label="${confirmPasswordLabel}" placeholder="${confirmPasswordPlaceholder}" />
          <form:errors path="passwordConfirmationValid" cssClass="text-error text-xs mt-1" element="p" />
          <paw:button type="submit" fullWidth="true" color="primary" text="${registerSubmitLabel}" />
        </form:form>

        <p class="text-center text-sm text-on-surface-variant m-0">
          <spring:message code="register.hasAccount" />
          <a href="${loginUrl}" class="link link-hover text-primary font-semibold ml-1 no-underline">
            <spring:message code="register.loginLink" />
          </a>
        </p>
      </div>
    </div>
  </div>
</paw:layout>
