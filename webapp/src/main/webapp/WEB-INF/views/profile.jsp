<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="profileUrl" value="/profile" />
<c:url var="profileEditUrl" value="/profile"><c:param name="edit" value="true" /></c:url>
<c:url var="logoutUrl" value="/logout" />
<c:url var="profilePasswordRecoveryUrl" value="/profile/password-recovery" />
<spring:message code="profile.logout" var="logoutLabel" />
<spring:message code="profile.passwordRecovery.send" var="passwordRecoverySendLabel" />
<spring:message code="profile.edit" var="profileEditLabel" />
<spring:message code="profile.cancelEdit" var="profileCancelEditLabel" />
<spring:message code="profile.save" var="profileSaveLabel" />
<spring:message code="profile.givenName" var="givenNameLabel" />
<spring:message code="profile.lastName" var="lastNameLabel" />
<spring:message code="profile.email" var="emailLabel" />
<spring:message code="profile.phone" var="phoneLabel" />
<spring:message code="profile.paymentAlias" var="paymentAliasLabel" />
<spring:message code="profile.preferredLanguage.es" var="spanishLabel" />
<spring:message code="profile.preferredLanguage.en" var="englishLabel" />

<c:set var="initials" value="" />
<c:if test="${not empty user.givenName}">
  <c:set var="initials" value="${fn:substring(user.givenName, 0, 1)}" />
</c:if>
<c:if test="${not empty user.lastName}">
  <c:set var="initials" value="${initials}${fn:substring(user.lastName, 0, 1)}" />
</c:if>
<c:set var="initials" value="${fn:toUpperCase(initials)}" />

<paw:layout title="Botecito" mainClass="pt-24 pb-10 w-full max-w-7xl mx-auto px-6">
  <section class="min-w-0">
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-6 gap-6">
          <div class="flex min-w-0 flex-col gap-4 pb-4 border-b border-outline-variant/20 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex min-w-0 items-center gap-4">
              <div class="avatar placeholder shrink-0">
                <div class="bg-primary text-primary-content rounded-full w-16 h-16 flex items-center justify-center">
                  <span class="font-bold text-2xl font-headline">${initials}</span>
                </div>
              </div>
              <div class="min-w-0">
                <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0 break-words">
                  <spring:message code="profile.title" />
                </h1>
                <p class="text-on-surface-variant m-0 mt-1 break-all">
                  <c:out value="${user.email}" />
                </p>
              </div>
            </div>
            <c:if test="${not profileEdit}">
              <paw:button href="${profileEditUrl}" color="primary" variant="outline" icon="edit" text="${profileEditLabel}" cssClass="shrink-0 w-full sm:w-auto" />
            </c:if>
          </div>

          <c:if test="${param.profileAction == 'updated'}">
            <paw:alertMessage type="success"><spring:message code="profile.updated" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.profileAction == 'verificationSent'}">
            <paw:alertMessage type="success"><spring:message code="profile.emailVerification.sent" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.passwordRecovery == 'sent'}">
            <paw:alertMessage type="success"><spring:message code="profile.passwordRecovery.sent" /></paw:alertMessage>
          </c:if>

          <form:form action="${profileUrl}" method="post" modelAttribute="profileForm" onsubmit="${not profileEdit ? 'return false;' : 'return true;'}">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <paw:formField path="givenName" label="${givenNameLabel}" maxlength="100" required="${profileEdit}" readonly="${not profileEdit}" />
              <paw:formField path="lastName" label="${lastNameLabel}" maxlength="100" required="${profileEdit}" readonly="${not profileEdit}" />
              <paw:formField path="email" type="email" label="${emailLabel}" maxlength="150" required="${profileEdit}" readonly="${not profileEdit}" />
              <paw:formField path="phone" label="${phoneLabel}" maxlength="30" readonly="${not profileEdit}" />
              <paw:formField path="paymentAlias" label="${paymentAliasLabel}" maxlength="120" readonly="${not profileEdit}" />

              <fieldset class="fieldset">
                <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
                  <spring:message code="profile.preferredLanguage" />
                  <c:if test="${profileEdit}"><span class="text-error" aria-hidden="true">*</span></c:if>
                </legend>
                <form:select
                    path="preferredLanguage"
                    id="preferredLanguage"
                    disabled="${not profileEdit}"
                    cssClass="select w-full ${not profileEdit ? 'select-disabled cursor-default opacity-90' : ''}"
                    cssErrorClass="select select-error w-full"
                >
                  <form:option value="es" label="${spanishLabel}" />
                  <form:option value="en" label="${englishLabel}" />
                </form:select>
                <form:errors path="preferredLanguage" cssClass="text-error text-xs mt-1" element="p" />
              </fieldset>

              <div class="w-full min-w-0 rounded-xl bg-base-200 p-4">
                <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
                  <spring:message code="profile.memberSince" />
                </p>
                <p class="text-base font-bold text-on-surface mt-1 mb-0"><c:out value="${memberSinceDisplay}" /></p>
              </div>

              <c:if test="${profileEdit}">
                <div class="flex h-full min-h-0 w-full flex-row flex-wrap items-end justify-end justify-self-end gap-2 sm:gap-3">
                  <paw:button href="${profileUrl}" variant="outline" text="${profileCancelEditLabel}" cssClass="shrink-0" />
                  <paw:button type="submit" color="primary" icon="save" text="${profileSaveLabel}" cssClass="shrink-0" />
                </div>
              </c:if>
            </div>
          </form:form>

          <div class="border-b border-outline-variant/20"></div>

          <div class="flex flex-wrap items-center gap-3">
            <form action="${profilePasswordRecoveryUrl}" method="post" class="m-0">
              <paw:button type="submit" color="secondary" icon="mail" text="${passwordRecoverySendLabel}" />
            </form>
            <form action="${logoutUrl}" method="post" class="m-0">
              <paw:button type="submit" variant="outline" icon="logout" text="${logoutLabel}" />
            </form>
          </div>
      </div>
    </div>
  </section>
</paw:layout>
