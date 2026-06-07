<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
<c:url var="myBoatsUrl" value="/my-boats" />
<c:url var="publishUrl" value="/publish" />
<spring:message code="nav.publishCta" var="publishCtaLabel" />
<spring:message code="settings.publications.edit" var="editLabel" />
<spring:message code="settings.publications.manageAvailability" var="manageAvailabilityLabel" />
<spring:message code="settings.publications.enable" var="enableLabel" />
<spring:message code="settings.publications.disable" var="disableLabel" />
<spring:message code="settings.publications.delete" var="deleteLabel" />
<spring:message code="settings.publications.delete.confirm.title" var="deleteConfirmTitle" />
<spring:message code="settings.publications.delete.confirm.message" var="deleteConfirmMessage" />
<spring:message code="settings.publications.delete.confirm.confirm" var="deleteConfirmConfirm" />
<spring:message code="settings.publications.delete.confirm.cancel" var="deleteConfirmCancel" />
<spring:message code="settings.publications.actions" var="publicationActionsLabel" />
<spring:message code="page.title.myBoats" var="titleMyBoats" />
<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="myBoats.search.placeholder" var="searchPlaceholder" />
<spring:message code="myBoats.sort.nameAsc" var="sortNameAscLabel" />
<spring:message code="myBoats.sort.nameDesc" var="sortNameDescLabel" />
<spring:message code="myBoats.filter.status.placeholder" var="statusFilterLabel" />
<spring:message code="myBoats.filter.status.any" var="statusAnyLabel" />
<spring:message code="myBoats.filter.status.active" var="statusActiveLabel" />
<spring:message code="myBoats.filter.status.inactive" var="statusInactiveLabel" />
<spring:message code="marketplace.filters.clear" var="filtersClearLabel" />
<spring:message code="marketplace.sort.label" var="sortLabel" />
<spring:message code="myBoats.pageSize" var="pageSizeFieldLabel" />
<spring:message code="myBoats.filter.empty.title" var="filterEmptyTitleLabel" />
<spring:message code="myBoats.filter.empty.message" var="filterEmptyMessageLabel" />
<c:set var="pageSize" value="${myBoatsSearch.pageSize != null ? myBoatsSearch.pageSize : 12}" />
<c:set var="currentSortBy" value="${empty myBoatsSearch.sortBy ? 'newest' : myBoatsSearch.sortBy}" />
<c:set var="hasActiveFilters" value="${not empty myBoatsSearch.searchQuery or not empty myBoatsSearch.status}" />

<c:url var="clearFiltersUrl" value="/my-boats">
  <c:if test="${currentSortBy != 'newest'}">
    <c:param name="sortBy" value="${currentSortBy}" />
  </c:if>
  <c:if test="${pageSize != 12}">
    <c:param name="pageSize" value="${pageSize}" />
  </c:if>
</c:url>

<c:if test="${itemPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="/my-boats">
    <c:param name="page" value="${itemPage.previousPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty myBoatsSearch.searchQuery}"><c:param name="searchQuery" value="${myBoatsSearch.searchQuery}" /></c:if>
    <c:if test="${not empty myBoatsSearch.status}"><c:param name="status" value="${myBoatsSearch.status}" /></c:if>
  </c:url>
  <c:url var="nextPageUrl" value="/my-boats">
    <c:param name="page" value="${itemPage.nextPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty myBoatsSearch.searchQuery}"><c:param name="searchQuery" value="${myBoatsSearch.searchQuery}" /></c:if>
    <c:if test="${not empty myBoatsSearch.status}"><c:param name="status" value="${myBoatsSearch.status}" /></c:if>
  </c:url>
</c:if>

<paw:layout title="${titleMyBoats} - Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6" scripts="toast,publish-wizard,edit-wizard">
  <span data-publish-wizard-clear hidden="hidden"></span>
  <span data-edit-wizard-clear hidden="hidden"></span>
  <paw:toastNotifier />
  <section class="min-w-0 space-y-6">
    <div class="flex flex-col">
      <div class="flex items-end justify-between mb-4">
        <div class="min-w-0">
          <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="myBoats.title" /></h1>
          <p class="text-on-surface-variant mt-2 m-0"><spring:message code="myBoats.subtitle" /></p>
        </div>
        <a href="${publishUrl}" class="btn btn-secondary shrink-0 no-underline">
          <span class="material-symbols-outlined text-base">add</span>
          <c:out value="${publishCtaLabel}" />
        </a>
      </div>
    </div>

    <form id="my-boats-filters-form" action="${myBoatsUrl}" method="get" class="w-full">
      <input type="hidden" name="page" value="1" />
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-4">
        <div class="min-w-0 w-full flex-1">
          <paw:searchBar
              formId="my-boats-filters-form"
              name="searchQuery"
              value="${fn:escapeXml(myBoatsSearch.searchQuery)}"
              placeholder="${searchPlaceholder}"
              ariaLabel="${searchLabel}"
              inputId="my-boats-search-query"
              maxlength="100"
              size="lg" />
        </div>
        <div class="flex shrink-0 flex-wrap items-center gap-2 text-sm font-medium text-on-surface-variant lg:flex-nowrap lg:justify-end">
        <label for="my-boats-status" class="shrink-0 whitespace-nowrap"><c:out value="${statusFilterLabel}" /></label>
        <select
            id="my-boats-status"
            name="status"
            class="select select-sm w-28 max-w-[40vw] shrink-0 font-bold text-primary sm:max-w-none"
            onchange="this.form.requestSubmit()">
          <option value="" ${empty myBoatsSearch.status ? 'selected="selected"' : ''}><c:out value="${statusAnyLabel}" /></option>
          <option value="ACTIVE" ${myBoatsSearch.status == 'ACTIVE' ? 'selected="selected"' : ''}><c:out value="${statusActiveLabel}" /></option>
          <option value="INACTIVE" ${myBoatsSearch.status == 'INACTIVE' ? 'selected="selected"' : ''}><c:out value="${statusInactiveLabel}" /></option>
        </select>
        <label for="my-boats-sort" class="shrink-0 whitespace-nowrap ml-2"><c:out value="${sortLabel}" /></label>
        <select
            id="my-boats-sort"
            name="sortBy"
            class="select select-sm w-32 max-w-[40vw] shrink-0 font-bold text-primary sm:max-w-none sm:w-36"
            onchange="this.form.requestSubmit()">
          <option value="newest" ${empty myBoatsSearch.sortBy || myBoatsSearch.sortBy == 'newest' ? 'selected="selected"' : ''}>
            <spring:message code="marketplace.sort.newest" />
          </option>
          <option value="oldest" ${myBoatsSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}>
            <spring:message code="marketplace.sort.oldest" />
          </option>
          <option value="nameAsc" ${myBoatsSearch.sortBy == 'nameAsc' ? 'selected="selected"' : ''}><c:out value="${sortNameAscLabel}" /></option>
          <option value="nameDesc" ${myBoatsSearch.sortBy == 'nameDesc' ? 'selected="selected"' : ''}><c:out value="${sortNameDescLabel}" /></option>
        </select>
        <label for="my-boats-page-size" class="shrink-0 ml-2"><c:out value="${pageSizeFieldLabel}" /></label>
        <select
            id="my-boats-page-size"
            name="pageSize"
            class="select select-sm w-20 font-bold text-primary"
            onchange="this.form.requestSubmit()">
          <option value="6" ${pageSize == 6 ? 'selected="selected"' : ''}>6</option>
          <option value="12" ${pageSize == 12 ? 'selected="selected"' : ''}>12</option>
          <option value="18" ${pageSize == 18 ? 'selected="selected"' : ''}>18</option>
        </select>
        </div>
      </div>
    </form>

    <div id="my-publications" class="scroll-mt-24 space-y-4">
      <c:choose>
        <c:when test="${not empty ownedItems}">
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 md:gap-3">
            <c:forEach var="item" items="${ownedItems}">
              <c:set var="version" value="${item.latestVersion}" />
              <c:set var="itemActive" value="${item.status == 'ACTIVE'}" />
              <c:url var="itemDetailUrl" value="/item/${item.id}" />
              <c:url var="editItemUrl" value="/edit/${item.id}" />
              <c:url var="manageAvailabilityItemUrl" value="/my-boats/${item.id}/availability" />
              <c:url var="disableItemUrl" value="/my-boats/${item.id}/disable" />
              <c:url var="enableItemUrl" value="/my-boats/${item.id}/enable" />
              <c:url var="deleteItemUrl" value="/my-boats/${item.id}/delete" />
              <c:url var="publicationImageUrl" value="/css/boat-placeholder.svg" />
              <c:if test="${not empty version.media}">
                <c:set var="coverMedia" value="${version.media[0]}" />
                <c:if test="${not empty coverMedia.image}">
                  <c:url var="publicationImageUrl" value="/image/${coverMedia.image.id}" />
                </c:if>
              </c:if>
              <c:set var="deleteModalId" value="delete-publication-modal-${item.id}" />
              <c:set var="disableFormId" value="disable-publication-form-${item.id}" />
              <c:set var="enableFormId" value="enable-publication-form-${item.id}" />
              <form id="${disableFormId}" action="${disableItemUrl}" method="post" hidden></form>
              <form id="${enableFormId}" action="${enableItemUrl}" method="post" hidden></form>
              <div class="group relative flex h-full w-full max-w-sm flex-col rounded-xl bg-base-200 transition hover:bg-base-300 ${itemActive ? '' : 'opacity-75'}">
                <a href="${itemDetailUrl}" class="absolute inset-0 z-0 rounded-xl focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary" aria-label="${fn:escapeXml(version.title)}"></a>
                <div class="dropdown dropdown-end absolute right-1.5 top-1.5 z-20 pointer-events-auto">
                  <button type="button" tabindex="0" role="button" class="btn btn-ghost btn-xs btn-circle bg-base-100/90 shadow-sm backdrop-blur-sm border border-outline-variant/20 hover:bg-base-100" aria-label="${fn:escapeXml(publicationActionsLabel)}">
                    <span class="material-symbols-outlined text-base leading-none">more_vert</span>
                  </button>
                  <ul tabindex="0" class="dropdown-content menu z-30 mt-1 w-52 rounded-box border border-outline-variant/20 bg-base-100 p-2 shadow-md">
                    <li>
                      <a href="${editItemUrl}" class="gap-2 no-underline">
                        <span class="material-symbols-outlined text-base">edit</span>
                        <c:out value="${editLabel}" />
                      </a>
                    </li>
                    <li>
                      <a href="${manageAvailabilityItemUrl}" class="gap-2 no-underline">
                        <span class="material-symbols-outlined text-base">event_available</span>
                        <c:out value="${manageAvailabilityLabel}" />
                      </a>
                    </li>
                    <c:choose>
                      <c:when test="${itemActive}">
                        <li>
                          <button type="submit" form="${disableFormId}" class="gap-2">
                            <span class="material-symbols-outlined text-base">visibility_off</span>
                            <c:out value="${disableLabel}" />
                          </button>
                        </li>
                      </c:when>
                      <c:otherwise>
                        <li>
                          <button type="submit" form="${enableFormId}" class="gap-2">
                            <span class="material-symbols-outlined text-base">visibility</span>
                            <c:out value="${enableLabel}" />
                          </button>
                        </li>
                      </c:otherwise>
                    </c:choose>
                    <li>
                      <button type="button" class="gap-2 text-error" onclick="document.getElementById('${deleteModalId}').showModal()">
                        <span class="material-symbols-outlined text-base">delete</span>
                        <c:out value="${deleteLabel}" />
                      </button>
                    </li>
                  </ul>
                </div>
                <div class="pointer-events-none flex flex-col gap-2 p-2 sm:p-3">
                  <div class="h-24 w-full shrink-0 overflow-hidden rounded-lg bg-base-100 sm:h-32">
                    <img src="${publicationImageUrl}" alt="${fn:escapeXml(version.title)}" class="h-full w-full object-cover" loading="lazy" />
                  </div>
                  <div class="flex min-w-0 flex-1 flex-col gap-1">
                    <div class="flex min-w-0 items-start gap-1.5 pr-8">
                      <p class="m-0 min-w-0 flex-1 break-words text-xs font-extrabold text-on-surface line-clamp-2 sm:text-sm">
                        <c:out value="${version.title}" />
                      </p>
                      <span class="badge ${itemActive ? 'badge-success' : 'badge-ghost'} badge-xs shrink-0 font-bold">
                        <spring:message code="${itemActive ? 'settings.publications.status.active' : 'settings.publications.status.inactive'}" />
                      </span>
                    </div>
                    <p class="m-0 mt-auto text-[11px] font-bold text-on-surface sm:text-xs">
                      $<fmt:formatNumber value="${version.price}" type="number" groupingUsed="true" maxFractionDigits="0" />
                      <span class="font-normal text-on-surface-variant"> &middot; <spring:message code="marketplace.card.perHour" /></span>
                    </p>
                  </div>
                </div>
              </div>
              <paw:confirmModal id="${deleteModalId}" title="${deleteConfirmTitle}" message="${deleteConfirmMessage}" confirmText="${deleteConfirmConfirm}" cancelText="${deleteConfirmCancel}" confirmColor="danger" icon="delete_forever">
                <form action="${deleteItemUrl}" method="post" class="m-0">
                  <paw:button type="submit" color="danger" cssClass="w-full sm:w-auto" text="${deleteConfirmConfirm}" />
                </form>
              </paw:confirmModal>
            </c:forEach>
          </div>
        </c:when>
        <c:otherwise>
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body items-center gap-4 p-10 text-center">
              <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                <span class="material-symbols-outlined text-4xl" aria-hidden="true">sailing</span>
              </div>
              <c:choose>
                <c:when test="${hasValidationErrors or hasActiveFilters or itemPage.totalItems > 0}">
                  <div class="max-w-lg">
                    <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><c:out value="${filterEmptyTitleLabel}" /></h2>
                    <p class="m-0 mt-2 text-on-surface-variant"><c:out value="${filterEmptyMessageLabel}" /></p>
                  </div>
                  <a href="${clearFiltersUrl}" data-clear-list-filters class="btn btn-primary btn-sm no-underline">
                    <c:out value="${filtersClearLabel}" />
                  </a>
                </c:when>
                <c:otherwise>
                  <p class="m-0 max-w-lg text-on-surface-variant"><spring:message code="settings.publications.empty" /></p>
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </c:otherwise>
      </c:choose>

      <c:if test="${itemPage.totalPages > 1}">
        <nav class="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
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
    </div>
  </section>
</paw:layout>
