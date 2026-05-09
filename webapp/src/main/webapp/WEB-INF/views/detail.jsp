<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrlFallback" value="/marketplace" />
<spring:message code="detail.page.title" var="detailPageTitle" />
<spring:message code="detail.version.badge" var="currentVersionBadge" />

<paw:layout title="${detailPageTitle}" mainClass="pt-24 pb-12 w-full max-w-4xl mx-auto px-6">
  <paw:toastNotifier />
  <div class="space-y-6">
    <a href="<c:choose><c:when test="${not empty marketplaceBackHref}"><c:out value="${marketplaceBackHref}" /></c:when><c:otherwise>${marketplaceUrlFallback}</c:otherwise></c:choose>" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="detail.back.marketplace" /></span>
    </a>

    <c:choose>
      <c:when test="${not empty resolvedItem}">
        <div class="flex flex-wrap items-center gap-2">
          <span class="badge badge-primary badge-outline"><c:out value="${currentVersionBadge}" /></span>
        </div>
        <article class="card bg-base-100 shadow-sm">
          <div class="card-body gap-4">
            <h1 class="text-3xl font-black text-on-background m-0"><c:out value="${resolvedItem.title}" /></h1>
            <p class="text-on-surface-variant m-0 whitespace-pre-wrap"><c:out value="${resolvedItem.description}" /></p>
            <div class="flex flex-wrap gap-4 text-sm font-semibold text-on-surface-variant">
              <span class="text-primary text-xl font-black">$<fmt:formatNumber value="${resolvedItem.price}" type="number" groupingUsed="true" maxFractionDigits="0" /></span>
              <span class="flex items-center gap-1">
                <span class="material-symbols-outlined text-base text-warning">star</span>
                <c:choose>
                  <c:when test="${resolvedItem.totalReviews > 0}">
                    <fmt:formatNumber value="${resolvedItem.averageRating}" minFractionDigits="1" maxFractionDigits="1" />
                    <span class="text-outline">(<c:out value="${resolvedItem.totalReviews}" />)</span>
                  </c:when>
                  <c:otherwise><spring:message code="reviews.empty.short" /></c:otherwise>
                </c:choose>
              </span>
            </div>
          </div>
        </article>
      </c:when>
      <c:otherwise>
        <div class="alert alert-warning shadow-sm rounded-xl">
          <span class="material-symbols-outlined">search_off</span>
          <span><spring:message code="detail.item.missingBody" /></span>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</paw:layout>
