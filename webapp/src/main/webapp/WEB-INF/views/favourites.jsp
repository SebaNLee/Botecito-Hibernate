<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="favouritesUrl" value="/favourites" />
<spring:message code="page.title.favourites" var="titleFavourites" />
<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="favourites.search.placeholder" var="searchPlaceholder" />
<spring:message code="myBoats.sort.nameAsc" var="sortNameAscLabel" />
<spring:message code="myBoats.sort.nameDesc" var="sortNameDescLabel" />
<spring:message code="marketplace.filters.clear" var="filtersClearLabel" />
<spring:message code="marketplace.sort.label" var="sortLabel" />
<spring:message code="myBoats.pageSize" var="pageSizeFieldLabel" />
<spring:message code="favourites.filter.empty.title" var="filterEmptyTitleLabel" />
<spring:message code="favourites.filter.empty.message" var="filterEmptyMessageLabel" />
<c:set var="hasActiveFilters" value="${not empty favouritesSearch.searchQuery}" />

<c:url var="clearFiltersUrl" value="/favourites">
  <c:if test="${favouritesSearch.sortBy != 'newest'}">
    <c:param name="sortBy" value="${favouritesSearch.sortBy}" />
  </c:if>
  <c:if test="${favouritesSearch.pageSize != 12}">
    <c:param name="pageSize" value="${favouritesSearch.pageSize}" />
  </c:if>
</c:url>

<c:if test="${itemPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="/favourites">
    <c:param name="page" value="${itemPage.previousPage}" />
    <c:param name="sortBy" value="${favouritesSearch.sortBy}" />
    <c:param name="pageSize" value="${favouritesSearch.pageSize}" />
    <c:if test="${not empty favouritesSearch.searchQuery}"><c:param name="searchQuery" value="${favouritesSearch.searchQuery}" /></c:if>
  </c:url>
  <c:url var="nextPageUrl" value="/favourites">
    <c:param name="page" value="${itemPage.nextPage}" />
    <c:param name="sortBy" value="${favouritesSearch.sortBy}" />
    <c:param name="pageSize" value="${favouritesSearch.pageSize}" />
    <c:if test="${not empty favouritesSearch.searchQuery}"><c:param name="searchQuery" value="${favouritesSearch.searchQuery}" /></c:if>
  </c:url>
</c:if>

<paw:layout title="${titleFavourites} - Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6" scripts="toast">
  <paw:toastNotifier />
  <section class="min-w-0 space-y-6">
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div class="min-w-0">
        <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="favourites.title" /></h1>
        <p class="text-on-surface-variant mt-2 m-0">
          <c:choose>
            <c:when test="${itemPage.totalItems == 1}">
              <spring:message code="favourites.results.count.singular" />
            </c:when>
            <c:otherwise>
              <spring:message code="favourites.results.count.plural" arguments="${itemPage.totalItems}" />
            </c:otherwise>
          </c:choose>
        </p>
      </div>
    </div>

    <form id="favourites-filters-form" action="${favouritesUrl}" method="get" class="w-full">
      <input type="hidden" name="page" value="1" />
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-4">
        <div class="min-w-0 w-full flex-1">
          <paw:searchBar
              formId="favourites-filters-form"
              name="searchQuery"
              value="${favouritesSearch.searchQuery}"
              placeholder="${searchPlaceholder}"
              ariaLabel="${searchLabel}"
              inputId="favourites-search-query"
              maxlength="100"
              size="lg" />
        </div>
        <div class="flex shrink-0 flex-wrap items-center gap-2 text-sm font-medium text-on-surface-variant lg:flex-nowrap lg:justify-end">
          <label for="favourites-sort" class="shrink-0 whitespace-nowrap"><c:out value="${sortLabel}" /></label>
          <select
              id="favourites-sort"
              name="sortBy"
              class="select select-sm w-32 max-w-[40vw] shrink-0 font-bold text-primary sm:max-w-none sm:w-36"
              onchange="this.form.requestSubmit()">
            <option value="newest" ${favouritesSearch.sortBy == 'newest' ? 'selected="selected"' : ''}>
              <spring:message code="marketplace.sort.newest" />
            </option>
            <option value="oldest" ${favouritesSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}>
              <spring:message code="marketplace.sort.oldest" />
            </option>
            <option value="nameAsc" ${favouritesSearch.sortBy == 'nameAsc' ? 'selected="selected"' : ''}><c:out value="${sortNameAscLabel}" /></option>
            <option value="nameDesc" ${favouritesSearch.sortBy == 'nameDesc' ? 'selected="selected"' : ''}><c:out value="${sortNameDescLabel}" /></option>
          </select>
          <label for="favourites-page-size" class="shrink-0 ml-2"><c:out value="${pageSizeFieldLabel}" /></label>
          <select
              id="favourites-page-size"
              name="pageSize"
              class="select select-sm w-20 font-bold text-primary"
              onchange="this.form.requestSubmit()">
            <option value="6" ${favouritesSearch.pageSize == 6 ? 'selected="selected"' : ''}>6</option>
            <option value="12" ${favouritesSearch.pageSize == 12 ? 'selected="selected"' : ''}>12</option>
            <option value="18" ${favouritesSearch.pageSize == 18 ? 'selected="selected"' : ''}>18</option>
          </select>
        </div>
      </div>
    </form>

    <c:if test="${empty itemPage.content}">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body items-center gap-4 p-10 text-center">
          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-4xl icon-heart-outline" aria-hidden="true">favorite</span>
          </div>
          <div class="max-w-lg">
            <c:choose>
              <c:when test="${hasValidationErrors or hasActiveFilters}">
                <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><c:out value="${filterEmptyTitleLabel}" /></h2>
                <p class="m-0 mt-2 text-on-surface-variant"><c:out value="${filterEmptyMessageLabel}" /></p>
              </c:when>
              <c:otherwise>
                <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><spring:message code="favourites.empty.title" /></h2>
                <p class="m-0 mt-2 text-on-surface-variant"><spring:message code="favourites.empty.message" /></p>
              </c:otherwise>
            </c:choose>
          </div>
          <a href="${clearFiltersUrl}" data-clear-list-filters class="btn btn-primary btn-sm no-underline">
            <c:out value="${filtersClearLabel}" />
          </a>
        </div>
      </div>
    </c:if>

    <c:if test="${not empty itemPage.content}">
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <c:forEach items="${itemPage.content}" var="item">
          <paw:listingCard
              item="${item}"
              favourite="${favouriteByItemId[item.id]}"
              canFavourite="${canFavouriteByItemId[item.id]}"
              favouritesSearch="${favouritesSearch}" />
        </c:forEach>
      </div>
    </c:if>

    <paw:pagination
        currentPage="${itemPage.page}"
        totalPages="${itemPage.totalPages}"
        hasPrevious="${itemPage.hasPrevious}"
        hasNext="${itemPage.hasNext}"
        previousPageUrl="${previousPageUrl}"
        nextPageUrl="${nextPageUrl}" />
  </section>
</paw:layout>
