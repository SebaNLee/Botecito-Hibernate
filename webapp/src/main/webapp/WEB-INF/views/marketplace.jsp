<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
<c:url var="publishUrl" value="/publish" />
<c:url var="homeUrl" value="/" />
<c:url var="marketplaceUrl" value="/marketplace" />
<spring:message code="filters.location" var="locationLabel" />
<spring:message code="filters.location.placeholder" var="locationPlaceholder" />
<spring:message code="filters.date" var="dateLabel" />
<spring:message code="filters.date.placeholder" var="datePlaceholder" />
<spring:message code="filters.time" var="timeLabel" />
<spring:message code="filters.time.placeholder" var="timePlaceholder" />
<spring:message code="filters.people" var="peopleLabel" />
<spring:message code="filters.people.any" var="peopleAnyPlaceholder" />
<spring:message code="filters.itemType" var="itemTypeLabel" />
<spring:message code="filters.itemType.placeholder" var="itemTypePlaceholder" />
<spring:message code="filters.itemType.panel" var="itemTypePanelCaption" />
<spring:message code="filters.itemType.noMatch" var="itemTypeNoMatchCaption" />
<spring:message code="filters.weight" var="weightLabel" />
<spring:message code="filters.weight.helper" var="weightHelper" />
<spring:message code="filters.difficulty" var="difficultyFilterLabel" />
<spring:message code="filters.difficulty.any" var="difficultyAnyLabel" />
<spring:message code="filters.minAvgRating" var="minAvgRatingFilterLabel" />
<spring:message code="filters.minAvgRating.any" var="minAvgRatingAnyLabel" />
<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="marketplace.filters.apply" var="filtersApplyLabel" />
<spring:message code="marketplace.filters.clear" var="filtersClearLabel" />
<spring:message code="marketplace.field.pageSize" var="pageSizeFieldLabel" />
<spring:message code="reviews.empty.short" var="reviewsEmptyShortLabel" />
<c:url var="clearMarketplaceFiltersUrl" value="/marketplace">
  <c:if test="${marketplaceSearch.sortBy != 'newest'}">
    <c:param name="sortBy" value="${marketplaceSearch.sortBy}" />
  </c:if>
  <c:if test="${marketplaceSearch.pageSize != 12}">
    <c:param name="pageSize" value="${marketplaceSearch.pageSize}" />
  </c:if>
</c:url>
<c:if test="${itemPage.hasPrevious}">
  <c:url var="previousPageUrl" value="/marketplace">
    <c:param name="page" value="${itemPage.previousPage}" />
    <c:param name="sortBy" value="${marketplaceSearch.sortBy}" />
    <c:param name="pageSize" value="${marketplaceSearch.pageSize}" />
    <c:if test="${not empty param.searchQuery}">
      <c:param name="searchQuery" value="${param.searchQuery}" />
    </c:if>
    <c:if test="${not empty param.location}">
      <c:param name="location" value="${param.location}" />
    </c:if>
    <c:if test="${not empty param.itemType}">
      <c:param name="itemType" value="${param.itemType}" />
    </c:if>
    <c:if test="${not empty param.date}">
      <c:param name="date" value="${param.date}" />
    </c:if>
    <c:if test="${not empty param.startTime}">
      <c:param name="startTime" value="${param.startTime}" />
    </c:if>
    <c:if test="${not empty param.endTime}">
      <c:param name="endTime" value="${param.endTime}" />
    </c:if>
    <c:if test="${not empty param.capacity}">
      <c:param name="capacity" value="${param.capacity}" />
    </c:if>
    <c:if test="${not empty param.weight}">
      <c:param name="weight" value="${param.weight}" />
    </c:if>
    <c:if test="${not empty param.difficulty}">
      <c:param name="difficulty" value="${param.difficulty}" />
    </c:if>
    <c:if test="${not empty param.minAvgRating}">
      <c:param name="minAvgRating" value="${param.minAvgRating}" />
    </c:if>
  </c:url>
</c:if>
<c:if test="${itemPage.hasNext}">
  <c:url var="nextPageUrl" value="/marketplace">
    <c:param name="page" value="${itemPage.nextPage}" />
    <c:param name="sortBy" value="${marketplaceSearch.sortBy}" />
    <c:param name="pageSize" value="${marketplaceSearch.pageSize}" />
    <c:if test="${not empty param.searchQuery}">
      <c:param name="searchQuery" value="${param.searchQuery}" />
    </c:if>
    <c:if test="${not empty param.location}">
      <c:param name="location" value="${param.location}" />
    </c:if>
    <c:if test="${not empty param.itemType}">
      <c:param name="itemType" value="${param.itemType}" />
    </c:if>
    <c:if test="${not empty param.date}">
      <c:param name="date" value="${param.date}" />
    </c:if>
    <c:if test="${not empty param.startTime}">
      <c:param name="startTime" value="${param.startTime}" />
    </c:if>
    <c:if test="${not empty param.endTime}">
      <c:param name="endTime" value="${param.endTime}" />
    </c:if>
    <c:if test="${not empty param.capacity}">
      <c:param name="capacity" value="${param.capacity}" />
    </c:if>
    <c:if test="${not empty param.weight}">
      <c:param name="weight" value="${param.weight}" />
    </c:if>
    <c:if test="${not empty param.difficulty}">
      <c:param name="difficulty" value="${param.difficulty}" />
    </c:if>
    <c:if test="${not empty param.minAvgRating}">
      <c:param name="minAvgRating" value="${param.minAvgRating}" />
    </c:if>
  </c:url>
</c:if>
<spring:message code="page.title.marketplace" var="titleMarketplace" />

<paw:layout title="${titleMarketplace} - Botecito" mainClass="pt-24 pb-12 w-full max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-[18rem_minmax(0,1fr)] lg:grid-cols-[20rem_minmax(0,1fr)] gap-8 items-start" scripts="toast,search-filters,date-time">
  <paw:toastNotifier />
  <aside class="relative z-40 w-full md:min-w-0">
    <div class="space-y-6">
      <a href="${homeUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
        <span class="material-symbols-outlined">arrow_back</span>
        <span><spring:message code="marketplace.back.home" /></span>
      </a>

      <paw:sectionCard element="aside" icon="tune">
        <jsp:attribute name="title"><spring:message code="marketplace.filters.title" /></jsp:attribute>
        <jsp:body>
          <form id="marketplace-filters-form" action="${marketplaceUrl}" method="get" class="space-y-6" data-filter-form="marketplace">
            <input type="hidden" name="pageSize" value="${marketplaceSearch.pageSize}" />
            <paw:optionsPicker
                id="marketplace-location"
                name="location"
                label="${locationLabel}"
                value="${marketplaceSearch.location}"
                placeholder="${locationPlaceholder}"
                icon="location_on"
                variant="inline" />

            <div class="grid grid-cols-1 gap-4">
              <paw:datePicker
                  id="marketplace-date"
                  dateFieldName="date"
                  label="${dateLabel}"
                  value="${param.date}"
                  placeholder="${datePlaceholder}"
                  restrictToAvailability="false" />
              <paw:timeRangePicker
                  id="marketplace-time-range"
                  dateInputId="marketplace-date"
                  startTimeFieldName="startTime"
                  endTimeFieldName="endTime"
                  label="${timeLabel}"
                  startValue="${param.startTime}"
                  endValue="${param.endTime}"
                  placeholder="${timePlaceholder}"
                  restrictToAvailability="false" />
            </div>

            <paw:peopleCount
                id="marketplace-capacity"
                name="capacity"
                label="${peopleLabel}"
                value="${param.capacity}"
                allowEmpty="true"
                placeholder="${peopleAnyPlaceholder}"
                min="1"
                max="20" />

            <paw:optionsPicker
                id="marketplace-item-type"
                name="itemType"
                label="${itemTypeLabel}"
                value="${marketplaceSearch.itemType}"
                placeholder="${itemTypePlaceholder}"
                icon="category"
                variant="inline"
                optionsUrl="/item-type-options"
                panelCaption="${itemTypePanelCaption}"
                emptyCaption="${itemTypeNoMatchCaption}" />

            <div class="form-control w-full">
              <label for="marketplace-difficulty" class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant mb-1 block">
                <c:out value="${difficultyFilterLabel}" />
              </label>
              <select id="marketplace-difficulty" name="difficulty" class="select select-bordered w-full font-semibold text-on-surface">
                <option value="" ${empty param.difficulty ? 'selected="selected"' : ''}><c:out value="${difficultyAnyLabel}" /></option>
                <option value="1" ${param.difficulty == '1' ? 'selected="selected"' : ''}><spring:message code="publish.difficulty.1" /></option>
                <option value="2" ${param.difficulty == '2' ? 'selected="selected"' : ''}><spring:message code="publish.difficulty.2" /></option>
                <option value="3" ${param.difficulty == '3' ? 'selected="selected"' : ''}><spring:message code="publish.difficulty.3" /></option>
                <option value="4" ${param.difficulty == '4' ? 'selected="selected"' : ''}><spring:message code="publish.difficulty.4" /></option>
                <option value="5" ${param.difficulty == '5' ? 'selected="selected"' : ''}><spring:message code="publish.difficulty.5" /></option>
              </select>
            </div>

            <div class="pt-4 border-t border-outline-variant/15 space-y-6">
              <paw:weightCapacitySlider
                  id="marketplace-max-weight"
                  name="weight"
                  label="${weightLabel}"
                  value="${param.weight}"
                  min="100"
                  max="2000"
                  step="50"
                  helper="${weightHelper}" />
              <paw:minAvgRatingStarPicker
                  id="marketplace-min-rating"
                  name="minAvgRating"
                  label="${minAvgRatingFilterLabel}"
                  value="${marketplaceSearch.minAvgRating}"
                  clearLabel="${minAvgRatingAnyLabel}" />
            </div>

            <div class="flex flex-col gap-3">
              <paw:button type="submit" color="primary" fullWidth="true" text="${filtersApplyLabel}" />
              <a
                  href="${clearMarketplaceFiltersUrl}"
                  class="btn btn-outline btn-block no-underline"
                  data-clear-marketplace-filters>
                <c:out value="${filtersClearLabel}" />
              </a>
            </div>
          </form>
        </jsp:body>
      </paw:sectionCard>
    </div>
  </aside>

  <section class="relative z-0 min-w-0">
    <div class="mx-auto mb-8 w-full max-w-3xl">
      <paw:searchBar
          formId="marketplace-filters-form"
          name="searchQuery"
          value="${param.searchQuery}"
          placeholder="${searchLabel}"
          ariaLabel="${searchLabel}"
          inputId="marketplace-search-query"
          maxlength="100"
          extraAttributes="data-marketplace-search-input"
          size="lg" />
    </div>

    <div class="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
      <div class="min-w-0">
        <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="marketplace.title" /></h1>
        <p class="text-on-surface-variant mt-2 m-0"><spring:message code="marketplace.results.count" arguments="${itemPage.totalItems}" /></p>
      </div>

      <form
          action="${marketplaceUrl}"
          method="get"
          class="flex items-center gap-3 text-sm font-medium text-on-surface-variant"
          data-marketplace-toolbar-form>
        <input type="hidden" name="page" value="${marketplaceSearch.page}" />
        <input type="hidden" name="searchQuery" value="<c:out value='${param.searchQuery}'/>" data-applied-filter-mirror />
        <input type="hidden" name="location" value="<c:out value='${marketplaceSearch.location}'/>" data-applied-filter-mirror />
        <input type="hidden" name="itemType" value="<c:out value='${marketplaceSearch.itemType}'/>" data-applied-filter-mirror />
        <input type="hidden" name="date" value="<c:out value='${param.date}'/>" data-applied-filter-mirror />
        <input type="hidden" name="startTime" value="<c:out value='${param.startTime}'/>" data-applied-filter-mirror />
        <input type="hidden" name="endTime" value="<c:out value='${param.endTime}'/>" data-applied-filter-mirror />
        <input type="hidden" name="capacity" value="<c:out value='${param.capacity}'/>" data-applied-filter-mirror />
        <input type="hidden" name="weight" value="<c:out value='${param.weight}'/>" data-applied-filter-mirror />
        <input type="hidden" name="difficulty" value="<c:out value='${param.difficulty}'/>" data-applied-filter-mirror />
        <input type="hidden" name="minAvgRating" value="<c:out value='${marketplaceSearch.minAvgRating}'/>" data-applied-filter-mirror />
        <label for="marketplace-sort" class="shrink-0"><spring:message code="marketplace.sort.label" /></label>
        <select id="marketplace-sort" name="sortBy" class="select select-sm font-bold text-primary">
          <option value="newest" ${marketplaceSearch.sortBy == 'newest' ? 'selected="selected"' : ''}><spring:message code="marketplace.sort.newest" /></option>
          <option value="oldest" ${marketplaceSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}><spring:message code="marketplace.sort.oldest" /></option>
          <option value="priceAsc" ${marketplaceSearch.sortBy == 'priceAsc' ? 'selected="selected"' : ''}><spring:message code="marketplace.sort.priceAsc" /></option>
          <option value="priceDesc" ${marketplaceSearch.sortBy == 'priceDesc' ? 'selected="selected"' : ''}><spring:message code="marketplace.sort.priceDesc" /></option>
        </select>
        <label for="marketplace-page-size" class="shrink-0 ml-2"><c:out value="${pageSizeFieldLabel}" /></label>
        <select id="marketplace-page-size" name="pageSize" class="select select-sm w-20 font-bold text-primary">
            <option value="6" ${marketplaceSearch.pageSize == 6 ? 'selected="selected"' : ''}>6</option>
            <option value="12" ${marketplaceSearch.pageSize == 12 ? 'selected="selected"' : ''}>12</option>
            <option value="18" ${marketplaceSearch.pageSize == 18 ? 'selected="selected"' : ''}>18</option>
        </select>
      </form>
    </div>

    <c:if test="${empty itemPage.content}">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body items-center text-center gap-4 p-10">
          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-4xl">travel_explore</span>
          </div>
          <div class="max-w-lg">
            <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><spring:message code="marketplace.empty.title" /></h2>
            <p class="m-0 mt-2 text-on-surface-variant"><spring:message code="marketplace.empty.message" /></p>
          </div>
          <a href="${clearMarketplaceFiltersUrl}" class="btn btn-primary no-underline" data-clear-marketplace-filters>
            <spring:message code="marketplace.empty.clear" />
          </a>
        </div>
      </div>
    </c:if>

    <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
      <c:forEach items="${itemPage.content}" var="item">
        <paw:listingCard item="${item}" />
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
