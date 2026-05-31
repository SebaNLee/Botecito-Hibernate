<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="favouritesUrl" value="/favourites" />
<spring:message code="page.title.favourites" var="titleFavourites" />

<c:if test="${itemPage.hasPrevious}">
  <c:url var="previousPageUrl" value="/favourites">
    <c:param name="page" value="${itemPage.previousPage}" />
    <c:param name="pageSize" value="${pageSize}" />
  </c:url>
</c:if>
<c:if test="${itemPage.hasNext}">
  <c:url var="nextPageUrl" value="/favourites">
    <c:param name="page" value="${itemPage.nextPage}" />
    <c:param name="pageSize" value="${pageSize}" />
  </c:url>
</c:if>

<paw:layout title="${titleFavourites} - Botecito" mainClass="pt-24 pb-12 w-full max-w-7xl mx-auto px-6 flex flex-col gap-8" scripts="toast">
  <paw:toastNotifier />
  <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
    <div class="min-w-0">
      <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="favourites.title" /></h1>
      <p class="text-on-surface-variant mt-2 m-0"><spring:message code="favourites.results.count" arguments="${itemsCount}" /></p>
    </div>
    <a href="${marketplaceUrl}" class="btn btn-outline no-underline gap-2">
      <span class="material-symbols-outlined text-base">travel_explore</span>
      <spring:message code="favourites.browse" />
    </a>
  </div>

  <c:if test="${empty items}">
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body items-center text-center gap-4 p-10">
        <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
          <span class="material-symbols-outlined text-4xl">favorite</span>
        </div>
        <div class="max-w-lg">
          <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><spring:message code="favourites.empty.title" /></h2>
          <p class="m-0 mt-2 text-on-surface-variant"><spring:message code="favourites.empty.message" /></p>
        </div>
        <a href="${marketplaceUrl}" class="btn btn-primary no-underline">
          <spring:message code="favourites.empty.browse" />
        </a>
      </div>
    </div>
  </c:if>

  <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
    <c:forEach items="${items}" var="item">
      <paw:listingCard
          item="${item}"
          coverSrc="${imageUrlsByItemId[item.id]}"
          returnTo="${favouritesReturnPath}"
          favourite="${favouriteByItemId[item.id]}"
          canFavourite="${canFavouriteByItemId[item.id]}"
          favouriteReturn="${favouritesReturnPath}" />
    </c:forEach>
  </div>

  <c:if test="${itemPage.totalPages > 1}">
    <nav class="mt-2 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
      <c:choose>
        <c:when test="${itemPage.hasPrevious}">
          <a href="${previousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
            <span class="material-symbols-outlined text-sm">arrow_back</span>
            <spring:message code="marketplace.pagination.previous" />
          </a>
        </c:when>
        <c:otherwise>
          <span class="btn btn-outline btn-sm btn-disabled gap-2">
            <span class="material-symbols-outlined text-sm">arrow_back</span>
            <spring:message code="marketplace.pagination.previous" />
          </span>
        </c:otherwise>
      </c:choose>
      <span>
        <spring:message code="marketplace.pagination.page" arguments="${itemPage.page},${itemPage.totalPages}" />
      </span>
      <c:choose>
        <c:when test="${itemPage.hasNext}">
          <a href="${nextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
            <spring:message code="marketplace.pagination.next" />
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </a>
        </c:when>
        <c:otherwise>
          <span class="btn btn-outline btn-sm btn-disabled gap-2">
            <spring:message code="marketplace.pagination.next" />
            <span class="material-symbols-outlined text-sm">arrow_forward</span>
          </span>
        </c:otherwise>
      </c:choose>
    </nav>
  </c:if>
</paw:layout>
