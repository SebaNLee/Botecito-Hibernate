<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="settingsUrl" value="/settings" />
<c:url var="settingsEditUrl" value="/settings"><c:param name="edit" value="true" /></c:url>
<c:url var="logoutUrl" value="/logout" />
<c:url var="settingsPasswordRecoveryUrl" value="/settings/password-recovery" />
<spring:message code="settings.logout" var="logoutLabel" />
<spring:message code="settings.passwordRecovery.send" var="passwordRecoverySendLabel" />
<spring:message code="settings.edit" var="settingsEditLabel" />
<spring:message code="settings.cancelEdit" var="settingsCancelEditLabel" />
<spring:message code="settings.save" var="settingsSaveLabel" />
<spring:message code="settings.givenName" var="givenNameLabel" />
<spring:message code="settings.lastName" var="lastNameLabel" />
<spring:message code="settings.email" var="emailLabel" />
<spring:message code="settings.phone" var="phoneLabel" />
<spring:message code="settings.paymentAlias" var="paymentAliasLabel" />
<spring:message code="settings.preferredLanguage.es" var="spanishLabel" />
<spring:message code="settings.preferredLanguage.en" var="englishLabel" />
<spring:message code="settings.subscriptions.title" var="subscriptionsTitle" />
<spring:message code="settings.subscriptions.empty" var="subscriptionsEmptyLabel" />
<spring:message code="settings.subscriptions.filter.empty.title" var="filterEmptyTitleLabel" />
<spring:message code="settings.subscriptions.filter.empty.message" var="filterEmptyMessageLabel" />
<spring:message code="marketplace.empty.clear" var="filterEmptyClearLabel" />
<spring:message code="subscription.unsubscribe" var="subscriptionUnsubscribeLabel" />
<spring:message code="settings.viewPublicProfile" var="viewPublicProfileLabel" />

<c:set var="hasActiveSubscriptionsFilters" value="${settingsView.page > 1 or settingsView.pageSize != 6}" />
<c:set var="showSubscriptionsFilterEmpty" value="${hasValidationErrors or hasActiveSubscriptionsFilters or subscriptionsPage.totalItems > 0}" />
<c:url var="clearSubscriptionsFiltersUrl" value="/settings">
  <c:if test="${settingsEdit}">
    <c:param name="edit" value="true" />
  </c:if>
</c:url>

<c:set var="initials" value="" />
<c:if test="${not empty user.firstName}">
  <c:set var="initials" value="${fn:substring(user.firstName, 0, 1)}" />
</c:if>
<c:if test="${not empty user.lastName}">
  <c:set var="initials" value="${initials}${fn:substring(user.lastName, 0, 1)}" />
</c:if>
<c:set var="initials" value="${fn:toUpperCase(initials)}" />
<spring:message code="page.title.settings" var="titleSettings" />

<paw:layout title="${titleSettings} - Botecito" mainClass="pt-24 pb-10 w-full max-w-7xl mx-auto px-6">
  <section class="min-w-0">
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-6 gap-6">
          <div class="flex min-w-0 flex-col gap-4 pb-4 border-b border-outline-variant/20 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex min-w-0 items-center gap-4">
              <div class="avatar placeholder shrink-0">
                <div class="bg-primary text-primary-content rounded-full w-16 h-16 flex items-center justify-center">
                  <span class="font-bold text-2xl font-headline"><c:out value="${initials}" /></span>
                </div>
              </div>
              <div class="min-w-0">
                <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0 break-words">
                  <spring:message code="settings.title" />
                </h1>
                <p class="text-on-surface-variant m-0 mt-1 break-all">
                  <c:out value="${user.email}" />
                </p>
              </div>
            </div>
            <c:if test="${not settingsEdit}">
              <c:url var="myPublicProfileUrl" value="/profiles/${user.id}/listings" />
              <div class="flex shrink-0 flex-col sm:flex-row gap-2 w-full sm:w-auto">
                <paw:button href="${myPublicProfileUrl}" variant="outline" icon="person" text="${viewPublicProfileLabel}" cssClass="w-full sm:w-auto" />
                <paw:button href="${settingsEditUrl}" color="primary" variant="outline" icon="edit" text="${settingsEditLabel}" cssClass="w-full sm:w-auto" />
              </div>
            </c:if>
          </div>

          <c:if test="${param.settingsAction == 'updated'}">
            <paw:alertMessage type="success"><spring:message code="settings.updated" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.settingsAction == 'verificationSent'}">
            <paw:alertMessage type="success"><spring:message code="settings.emailVerification.sent" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.passwordRecovery == 'sent'}">
            <paw:alertMessage type="success"><spring:message code="settings.passwordRecovery.sent" /></paw:alertMessage>
          </c:if>

          <form:form action="${settingsUrl}" method="post" modelAttribute="settingsForm">
            <c:choose>
              <c:when test="${settingsEdit}">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              </c:when>
              <c:otherwise>
                <fieldset disabled class="grid grid-cols-1 md:grid-cols-2 gap-4 border-0 p-0 m-0 min-w-0">
              </c:otherwise>
            </c:choose>
              <paw:formField path="givenName" label="${givenNameLabel}" maxlength="100" required="${settingsEdit}" readonly="${not settingsEdit}" />
              <paw:formField path="lastName" label="${lastNameLabel}" maxlength="100" required="${settingsEdit}" readonly="${not settingsEdit}" />
              <paw:formField path="email" type="email" label="${emailLabel}" maxlength="100" required="${settingsEdit}" readonly="${not settingsEdit}" />
              <paw:formField path="phone" label="${phoneLabel}" maxlength="30" readonly="${not settingsEdit}" />
              <paw:formField path="paymentAlias" label="${paymentAliasLabel}" maxlength="30" readonly="${not settingsEdit}" />

              <fieldset class="fieldset">
                <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
                  <spring:message code="settings.preferredLanguage" />
                  <c:if test="${settingsEdit}"><span class="text-error" aria-hidden="true">*</span></c:if>
                </legend>
                <c:choose>
                  <c:when test="${not settingsEdit}">
                    <c:set var="preferredLanguageSelectClass" value="select w-full select-disabled cursor-default opacity-90" />
                  </c:when>
                  <c:otherwise>
                    <c:set var="preferredLanguageSelectClass" value="select w-full" />
                  </c:otherwise>
                </c:choose>
                <form:select
                    path="preferredLanguage"
                    id="preferredLanguage"
                    disabled="${not settingsEdit}"
                    cssClass="${preferredLanguageSelectClass}"
                    cssErrorClass="select select-error w-full"
                >
                  <form:option value="es" label="${spanishLabel}" />
                  <form:option value="en" label="${englishLabel}" />
                </form:select>
                <form:errors path="preferredLanguage" cssClass="text-error text-xs mt-1" element="p" />
              </fieldset>

              <div class="w-full min-w-0 rounded-xl bg-base-200 p-4">
                <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
                  <spring:message code="settings.memberSince" />
                </p>
                <p class="text-base font-bold text-on-surface mt-1 mb-0">
                  <c:if test="${not empty user.createdAt}">
                    <fmt:parseDate value="${fn:substring(user.createdAt, 0, 16)}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedMemberSince" />
                    <fmt:formatDate value="${parsedMemberSince}" pattern="dd/MM/yyyy" var="formattedMemberSince" />
                  </c:if>
                  <c:out value="${formattedMemberSince}" />
                </p>
              </div>

              <c:if test="${settingsEdit}">
                <div class="flex h-full min-h-0 w-full flex-row flex-wrap items-end justify-end justify-self-end gap-2 sm:gap-3">
                  <paw:button href="${settingsUrl}" variant="outline" text="${settingsCancelEditLabel}" cssClass="shrink-0" />
                  <paw:button type="submit" color="primary" icon="save" text="${settingsSaveLabel}" cssClass="shrink-0" />
                </div>
              </c:if>
            <c:choose>
              <c:when test="${settingsEdit}"></div></c:when>
              <c:otherwise></fieldset></c:otherwise>
            </c:choose>
          </form:form>

          <div class="border-b border-outline-variant/20"></div>

          <div id="settings-subscriptions" class="scroll-mt-24">
          <section class="space-y-4">
            <div class="flex min-w-0 items-center justify-between gap-3">
              <div class="min-w-0">
                <h2 class="text-xl font-extrabold tracking-tight text-on-background m-0 break-words">
                  <c:out value="${subscriptionsTitle}" />
                </h2>
                <p class="text-on-surface-variant mt-2 m-0">
                  <c:choose>
                    <c:when test="${subscriptionsPage.totalItems == 1}">
                      <spring:message code="settings.subscriptions.results.count.singular" />
                    </c:when>
                    <c:otherwise>
                      <spring:message code="settings.subscriptions.results.count.plural" arguments="${subscriptionsPage.totalItems}" />
                    </c:otherwise>
                  </c:choose>
                </p>
              </div>
            </div>
            <c:choose>
              <c:when test="${empty subscriptionsPage.content}">
                <c:choose>
                  <c:when test="${showSubscriptionsFilterEmpty}">
                    <div class="card bg-base-100 shadow-sm">
                      <div class="card-body items-center gap-4 p-10 text-center">
                        <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                          <span class="material-symbols-outlined text-4xl" aria-hidden="true">notifications</span>
                        </div>
                        <div class="max-w-lg">
                          <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><c:out value="${filterEmptyTitleLabel}" /></h2>
                          <p class="m-0 mt-2 text-on-surface-variant"><c:out value="${filterEmptyMessageLabel}" /></p>
                        </div>
                        <a href="<c:out value='${clearSubscriptionsFiltersUrl}' />" class="btn btn-primary no-underline" data-clear-list-filters>
                          <c:out value="${filterEmptyClearLabel}" />
                        </a>
                      </div>
                    </div>
                  </c:when>
                  <c:otherwise>
                    <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/45 px-4 py-3 text-sm text-on-surface-variant">
                      <c:out value="${subscriptionsEmptyLabel}" />
                    </p>
                  </c:otherwise>
                </c:choose>
              </c:when>
              <c:otherwise>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <c:forEach items="${subscriptionsPage.content}" var="subscriptionUser">
                    <c:set var="subscriptionFirstName" value="" />
                    <c:if test="${subscriptionUser.firstName != null}">
                      <c:set var="subscriptionFirstName" value="${subscriptionUser.firstName}" />
                    </c:if>
                    <c:set var="subscriptionLastName" value="" />
                    <c:if test="${subscriptionUser.lastName != null}">
                      <c:set var="subscriptionLastName" value="${subscriptionUser.lastName}" />
                    </c:if>
                    <c:set var="subscriptionFirstNameTrimmed" value="${fn:trim(subscriptionFirstName)}" />
                    <c:set var="subscriptionLastNameTrimmed" value="${fn:trim(subscriptionLastName)}" />
                    <c:url var="subscriptionProfileUrl" value="/profiles/${subscriptionUser.id}/listings" />
                    <div class="rounded-xl bg-base-200 p-4 flex min-w-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div class="min-w-0">
                        <a href="<c:out value='${subscriptionProfileUrl}' />" class="m-0 font-bold text-on-surface break-words no-underline hover:underline">
                          <c:choose>
                            <c:when test="${not empty subscriptionFirstNameTrimmed or not empty subscriptionLastNameTrimmed}">
                              <c:out value="${subscriptionFirstNameTrimmed}" />
                              <c:if test="${not empty subscriptionFirstNameTrimmed and not empty subscriptionLastNameTrimmed}"> </c:if>
                              <c:out value="${subscriptionLastNameTrimmed}" />
                            </c:when>
                            <c:otherwise><c:out value="${subscriptionUser.email}" /></c:otherwise>
                          </c:choose>
                        </a>
                        <p class="m-0 mt-1 text-xs text-on-surface-variant break-all"><c:out value="${subscriptionUser.email}" /></p>
                      </div>
                      <c:url var="unsubscribeSettingsUrl" value="/settings/subscriptions/${subscriptionUser.id}/unsubscribe" />
                      <form action="<c:out value='${unsubscribeSettingsUrl}' />" method="post" class="m-0 shrink-0">
                        <paw:settingsViewHiddenFields view="${settingsView}" />
                        <paw:button type="submit" color="outline" icon="notifications_off" text="${subscriptionUnsubscribeLabel}" cssClass="w-full sm:w-auto" />
                      </form>
                    </div>
                  </c:forEach>
                </div>
                <c:url var="subscriptionsPreviousPageUrl" value="/settings">
                  <c:param name="page" value="${subscriptionsPage.previousPage}" />
                  <c:param name="pageSize" value="${subscriptionsPage.pageSize}" />
                  <c:if test="${settingsEdit}">
                    <c:param name="edit" value="true" />
                  </c:if>
                </c:url>
                <c:url var="subscriptionsNextPageUrl" value="/settings">
                  <c:param name="page" value="${subscriptionsPage.nextPage}" />
                  <c:param name="pageSize" value="${subscriptionsPage.pageSize}" />
                  <c:if test="${settingsEdit}">
                    <c:param name="edit" value="true" />
                  </c:if>
                </c:url>
                <paw:pagination
                    currentPage="${subscriptionsPage.page}"
                    totalPages="${subscriptionsPage.totalPages}"
                    hasPrevious="${subscriptionsPage.hasPrevious}"
                    hasNext="${subscriptionsPage.hasNext}"
                    previousPageUrl="${subscriptionsPreviousPageUrl}#settings-subscriptions"
                    nextPageUrl="${subscriptionsNextPageUrl}#settings-subscriptions"
                    navClass="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant" />
              </c:otherwise>
            </c:choose>
          </section>
          </div>

          <div class="border-b border-outline-variant/20"></div>

          <div class="flex flex-wrap items-center gap-3">
            <form action="<c:out value='${settingsPasswordRecoveryUrl}' />" method="post" class="m-0">
              <paw:button type="submit" color="secondary" icon="mail" text="${passwordRecoverySendLabel}" />
            </form>
            <form action="<c:out value='${logoutUrl}' />" method="post" class="m-0">
              <paw:button type="submit" variant="outline" icon="logout" text="${logoutLabel}" />
            </form>
          </div>
      </div>
    </div>
  </section>
</paw:layout>
