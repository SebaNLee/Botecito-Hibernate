<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:if test="${itemId != null}">
  <c:url var="itemUrl" value="/item/${itemId}" />
</c:if>
<spring:message code="${actionTitleCode}" arguments="${actionTitleArgs}" var="resolvedActionTitle" />
<spring:message code="${actionMessageCode}" arguments="${actionMessageArgs}" var="resolvedActionMessage" />
<spring:message code="mail.requestResolution.viewItem" var="viewItemLabel" />
<spring:message code="requestAction.pageTitle" var="pageTitle" />

<paw:layout title="${pageTitle}" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-xl px-6">
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-8 gap-4">
        <div class="badge badge-ghost badge-lg font-semibold self-start">
          <spring:message code="requestAction.badge" />
        </div>
        <paw:heading level="2" text="${resolvedActionTitle}" />
        <p class="m-0 text-base text-on-surface-variant">
          <c:out value="${resolvedActionMessage}" />
        </p>
        <c:if test="${itemId != null}">
          <div class="card-actions">
            <paw:button href="${itemUrl}" color="primary" icon="arrow_forward" iconTrailing="true" text="${viewItemLabel}" />
          </div>
        </c:if>
      </div>
    </div>
  </div>
</paw:layout>
