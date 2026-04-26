<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="homeUrl" value="/" />
<c:url var="marketplaceUrl" value="/marketplace" />
<spring:message code="error.page.documentTitle" arguments="${httpStatus}" var="docTitle" />
<spring:message code="error.page.backHome" var="backHomeLabel" />
<spring:message code="error.page.browseMarketplace" var="marketplaceLabel" />

<paw:layout title="${docTitle}" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-md px-6">
    <div class="card bg-base-100 shadow-sm border border-outline-variant/15">
      <div class="card-body p-8 gap-4 text-center">
        <c:choose>
          <c:when test="${httpStatus == 404}">
            <c:set var="iconName" value="search_off" />
            <c:set var="iconWrapClass" value="bg-info/15 text-info" />
          </c:when>
          <c:when test="${clientSideError}">
            <c:set var="iconName" value="gpp_maybe" />
            <c:set var="iconWrapClass" value="bg-warning/15 text-warning" />
          </c:when>
          <c:otherwise>
            <c:set var="iconName" value="engineering" />
            <c:set var="iconWrapClass" value="bg-error/15 text-error" />
          </c:otherwise>
        </c:choose>
        <div class="w-14 h-14 rounded-full flex items-center justify-center mx-auto ${iconWrapClass}">
          <span class="material-symbols-outlined text-3xl">${iconName}</span>
        </div>
        <div class="flex flex-wrap items-center justify-center gap-2">
          <span class="badge badge-neutral badge-lg font-mono font-semibold">${httpStatus}</span>
        </div>
        <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
          <spring:message code="${errorTitleCode}" />
        </h1>
        <p class="text-on-surface-variant m-0 leading-relaxed">
          <spring:message code="${errorMessageCode}" />
        </p>
        <c:if test="${not empty failedPath}">
          <p class="text-sm text-on-surface-variant/80 m-0 break-all">
            <span class="font-medium"><spring:message code="error.page.requestedPath" /></span>
            <code class="text-xs bg-base-200 px-2 py-1 rounded-md align-middle">${failedPath}</code>
          </p>
        </c:if>
        <div class="card-actions justify-center flex-wrap gap-2 mt-2">
          <paw:button href="${homeUrl}" color="primary" text="${backHomeLabel}" />
          <paw:button href="${marketplaceUrl}" color="outline" text="${marketplaceLabel}" />
        </div>
      </div>
    </div>
  </div>
</paw:layout>
