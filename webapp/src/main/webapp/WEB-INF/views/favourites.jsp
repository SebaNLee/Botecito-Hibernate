<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="favouritesUrl" value="/favourites" />
<spring:message code="page.title.favourites" var="titleFavourites" />
<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="favourites.search.placeholder" var="searchPlaceholder" />
<spring:message code="myBoats.sort.newest" var="sortNewestLabel" />
<spring:message code="myBoats.sort.oldest" var="sortOldestLabel" />
<spring:message code="myBoats.sort.nameAsc" var="sortNameAscLabel" />
<spring:message code="myBoats.sort.nameDesc" var="sortNameDescLabel" />
<spring:message code="myBoats.filter.status.active" var="statusActiveLabel" />
<spring:message code="myBoats.filter.status.inactive" var="statusInactiveLabel" />
<spring:message code="myBoats.filter.location.placeholder" var="locationPlaceholder" />
<spring:message code="marketplace.filters.clear" var="filtersClearLabel" />
<spring:message code="favourites.filter.empty" var="filterEmptyLabel" />
<c:set var="pageSize" value="${favouritesSearch.pageSize != null ? favouritesSearch.pageSize : 12}" />
<c:set var="currentSortBy" value="${empty favouritesSearch.sortBy ? 'newest' : favouritesSearch.sortBy}" />
<c:set var="hasActiveFilters" value="${not empty favouritesSearch.searchQuery or not empty favouritesSearch.status or not empty favouritesSearch.location}" />

<c:url var="clearFiltersUrl" value="/favourites">
  <c:if test="${currentSortBy != 'newest'}">
    <c:param name="sortBy" value="${currentSortBy}" />
  </c:if>
  <c:if test="${pageSize != 12}">
    <c:param name="pageSize" value="${pageSize}" />
  </c:if>
</c:url>

<c:if test="${itemPage.hasPrevious}">
  <c:url var="previousPageUrl" value="/favourites">
    <c:param name="page" value="${itemPage.previousPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty favouritesSearch.searchQuery}"><c:param name="searchQuery" value="${favouritesSearch.searchQuery}" /></c:if>
    <c:if test="${not empty favouritesSearch.status}"><c:param name="status" value="${favouritesSearch.status}" /></c:if>
    <c:if test="${not empty favouritesSearch.location}"><c:param name="location" value="${favouritesSearch.location}" /></c:if>
  </c:url>
</c:if>
<c:if test="${itemPage.hasNext}">
  <c:url var="nextPageUrl" value="/favourites">
    <c:param name="page" value="${itemPage.nextPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty favouritesSearch.searchQuery}"><c:param name="searchQuery" value="${favouritesSearch.searchQuery}" /></c:if>
    <c:if test="${not empty favouritesSearch.status}"><c:param name="status" value="${favouritesSearch.status}" /></c:if>
    <c:if test="${not empty favouritesSearch.location}"><c:param name="location" value="${favouritesSearch.location}" /></c:if>
  </c:url>
</c:if>

<paw:layout title="${titleFavourites} - Botecito" mainClass="pt-24 pb-12 w-full max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-[18rem_minmax(0,1fr)] lg:grid-cols-[20rem_minmax(0,1fr)] gap-8 items-start" scripts="toast,search-filters">
  <paw:toastNotifier />
  <aside class="relative z-40 w-full md:min-w-0">
    <div class="space-y-6">
      <a href="${marketplaceUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
        <span class="material-symbols-outlined">arrow_back</span>
        <span><spring:message code="common.back" /></span>
      </a>

      <paw:sectionCard element="aside" icon="tune">
        <jsp:attribute name="title"><spring:message code="marketplace.filters.title" /></jsp:attribute>
        <jsp:body>
          <form id="favourites-filters-form" action="${favouritesUrl}" method="get" class="space-y-6">
            <input type="hidden" name="page" value="1" />
            <input type="hidden" name="searchQuery" value="${fn:escapeXml(favouritesSearch.searchQuery)}" />

            <div class="form-control w-full">
              <label for="favourites-location" class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant mb-1 block">
                <c:out value="${locationPlaceholder}" />
              </label>
              <select id="favourites-location" name="location" class="select select-bordered w-full font-semibold text-on-surface" onchange="this.form.requestSubmit()">
                <option value="" ${empty favouritesSearch.location ? 'selected="selected"' : ''}><c:out value="${locationPlaceholder}" /></option>
                <c:forEach var="loc" items="${locationOptions}">
                  <option value="${loc.slug}" ${favouritesSearch.location == loc.slug ? 'selected="selected"' : ''}><c:out value="${loc.name}" /></option>
                </c:forEach>
              </select>
            </div>

            <div class="form-control w-full">
              <label for="favourites-status" class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant mb-1 block">
                <spring:message code="myBoats.filter.status.placeholder" />
              </label>
              <select id="favourites-status" name="status" class="select select-bordered w-full font-semibold text-on-surface" onchange="this.form.requestSubmit()">
                <option value="" ${empty favouritesSearch.status ? 'selected="selected"' : ''}><spring:message code="myBoats.filter.status.placeholder" /></option>
                <option value="ACTIVE" ${favouritesSearch.status == 'ACTIVE' ? 'selected="selected"' : ''}><c:out value="${statusActiveLabel}" /></option>
                <option value="INACTIVE" ${favouritesSearch.status == 'INACTIVE' ? 'selected="selected"' : ''}><c:out value="${statusInactiveLabel}" /></option>
              </select>
            </div>

            <div class="form-control w-full">
              <label for="favourites-sort" class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant mb-1 block">
                <spring:message code="marketplace.sort.label" />
              </label>
              <select id="favourites-sort" name="sortBy" class="select select-bordered w-full font-semibold text-on-surface" onchange="this.form.requestSubmit()">
                <option value="newest" ${empty favouritesSearch.sortBy || favouritesSearch.sortBy == 'newest' ? 'selected="selected"' : ''}><c:out value="${sortNewestLabel}" /></option>
                <option value="oldest" ${favouritesSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}><c:out value="${sortOldestLabel}" /></option>
                <option value="nameAsc" ${favouritesSearch.sortBy == 'nameAsc' ? 'selected="selected"' : ''}><c:out value="${sortNameAscLabel}" /></option>
                <option value="nameDesc" ${favouritesSearch.sortBy == 'nameDesc' ? 'selected="selected"' : ''}><c:out value="${sortNameDescLabel}" /></option>
              </select>
            </div>

            <div class="form-control w-full">
              <label for="favourites-page-size" class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant mb-1 block">
                <spring:message code="myBoats.pageSize" />
              </label>
              <select id="favourites-page-size" name="pageSize" class="select select-bordered w-full font-semibold text-on-surface" onchange="this.form.requestSubmit()">
                <option value="6" ${pageSize == 6 ? 'selected="selected"' : ''}>6</option>
                <option value="12" ${pageSize == 12 ? 'selected="selected"' : ''}>12</option>
                <option value="18" ${pageSize == 18 ? 'selected="selected"' : ''}>18</option>
              </select>
            </div>

            <c:if test="${hasActiveFilters}">
              <a href="${clearFiltersUrl}" class="btn btn-outline btn-block no-underline">
                <c:out value="${filtersClearLabel}" />
              </a>
            </c:if>
          </form>
        </jsp:body>
      </paw:sectionCard>
    </div>
  </aside>

  <section class="relative z-0 min-w-0">
    <div class="mx-auto mb-8 w-full max-w-3xl">
      <paw:searchBar
          formId="favourites-filters-form"
          name="searchQuery"
          value="${fn:escapeXml(favouritesSearch.searchQuery)}"
          placeholder="${searchPlaceholder}"
          ariaLabel="${searchLabel}"
          inputId="favourites-search-query"
          size="lg" />
    </div>

    <div class="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
      <div class="min-w-0">
        <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="favourites.title" /></h1>
        <p class="text-on-surface-variant mt-2 m-0">
          <c:choose>
            <c:when test="${itemsCount == 1}">
              <spring:message code="favourites.results.count.singular" />
            </c:when>
            <c:otherwise>
              <spring:message code="favourites.results.count.plural" arguments="${itemsCount}" />
            </c:otherwise>
          </c:choose>
        </p>
      </div>
    </div>

    <c:if test="${empty items}">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body items-center text-center gap-4 p-10">
          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-4xl">favorite</span>
          </div>
          <div class="max-w-lg">
            <c:choose>
              <c:when test="${hasActiveFilters}">
                <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><c:out value="${filterEmptyLabel}" /></h2>
                <p class="m-0 mt-2 text-on-surface-variant"><spring:message code="favourites.empty.filtered.message" /></p>
              </c:when>
              <c:otherwise>
                <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><spring:message code="favourites.empty.title" /></h2>
                <p class="m-0 mt-2 text-on-surface-variant"><spring:message code="favourites.empty.message" /></p>
              </c:otherwise>
            </c:choose>
          </div>
          <c:choose>
            <c:when test="${hasActiveFilters}">
              <a href="${clearFiltersUrl}" class="btn btn-outline no-underline">
                <c:out value="${filtersClearLabel}" />
              </a>
            </c:when>
            <c:otherwise>
              <a href="${marketplaceUrl}" class="btn btn-primary no-underline">
                <spring:message code="favourites.empty.browse" />
              </a>
            </c:otherwise>
          </c:choose>
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
      <nav class="mt-10 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
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
  </section>
</paw:layout>
