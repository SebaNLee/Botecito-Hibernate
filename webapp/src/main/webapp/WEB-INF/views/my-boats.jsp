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
<spring:message code="settings.publications.viewDetail" var="publicationViewDetailLabel" />
<spring:message code="page.title.myBoats" var="titleMyBoats" />
<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="myBoats.search.placeholder" var="searchPlaceholder" />
<spring:message code="myBoats.sort.newest" var="sortNewestLabel" />
<spring:message code="myBoats.sort.oldest" var="sortOldestLabel" />
<spring:message code="myBoats.sort.nameAsc" var="sortNameAscLabel" />
<spring:message code="myBoats.sort.nameDesc" var="sortNameDescLabel" />
<spring:message code="myBoats.filter.status.active" var="statusActiveLabel" />
<spring:message code="myBoats.filter.status.inactive" var="statusInactiveLabel" />
<spring:message code="myBoats.filter.location.placeholder" var="locationPlaceholder" />
<spring:message code="marketplace.filters.clear" var="filtersClearLabel" />
<spring:message code="myBoats.filter.empty" var="filterEmptyLabel" />
<c:set var="pageSize" value="${myBoatsSearch.pageSize != null ? myBoatsSearch.pageSize : 12}" />
<c:set var="currentSortBy" value="${empty myBoatsSearch.sortBy ? 'newest' : myBoatsSearch.sortBy}" />
<c:set var="hasActiveFilters" value="${not empty myBoatsSearch.searchQuery or not empty myBoatsSearch.status or not empty myBoatsSearch.location}" />

<c:url var="clearFiltersUrl" value="${myBoatsUrl}">
  <c:if test="${currentSortBy != 'newest'}">
    <c:param name="sortBy" value="${currentSortBy}" />
  </c:if>
  <c:if test="${pageSize != 12}">
    <c:param name="pageSize" value="${pageSize}" />
  </c:if>
</c:url>

<c:if test="${itemPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="${myBoatsUrl}">
    <c:param name="page" value="${itemPage.previousPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty myBoatsSearch.searchQuery}"><c:param name="searchQuery" value="${myBoatsSearch.searchQuery}" /></c:if>
    <c:if test="${not empty myBoatsSearch.status}"><c:param name="status" value="${myBoatsSearch.status}" /></c:if>
    <c:if test="${not empty myBoatsSearch.location}"><c:param name="location" value="${myBoatsSearch.location}" /></c:if>
  </c:url>
  <c:url var="nextPageUrl" value="${myBoatsUrl}">
    <c:param name="page" value="${itemPage.nextPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty myBoatsSearch.searchQuery}"><c:param name="searchQuery" value="${myBoatsSearch.searchQuery}" /></c:if>
    <c:if test="${not empty myBoatsSearch.status}"><c:param name="status" value="${myBoatsSearch.status}" /></c:if>
    <c:if test="${not empty myBoatsSearch.location}"><c:param name="location" value="${myBoatsSearch.location}" /></c:if>
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
      <div class="flex flex-wrap items-center justify-between gap-2 text-sm font-medium text-on-surface-variant">
        <div class="w-[70%]">
          <paw:searchBar
              formId="my-boats-filters-form"
              name="searchQuery"
              value="${fn:escapeXml(myBoatsSearch.searchQuery)}"
              placeholder="${searchPlaceholder}"
              ariaLabel="${searchLabel}"
              maxlength="100"
              size="sm" />
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <select id="my-boats-location" name="location" class="select select-xs w-28 font-bold text-primary" onchange="this.form.requestSubmit()">
            <option value="" disabled selected hidden><c:out value="${locationPlaceholder}" /></option>
            <c:forEach var="loc" items="${locationOptions}">
              <option value="${loc.slug}" ${myBoatsSearch.location == loc.slug ? 'selected="selected"' : ''}><c:out value="${loc.name}" /></option>
            </c:forEach>
          </select>

          <select id="my-boats-status" name="status" class="select select-xs w-24 font-bold text-primary" onchange="this.form.requestSubmit()">
            <option value="" disabled selected hidden><spring:message code="myBoats.filter.status.placeholder" /></option>
            <option value="ACTIVE" ${myBoatsSearch.status == 'ACTIVE' ? 'selected="selected"' : ''}><c:out value="${statusActiveLabel}" /></option>
            <option value="INACTIVE" ${myBoatsSearch.status == 'INACTIVE' ? 'selected="selected"' : ''}><c:out value="${statusInactiveLabel}" /></option>
          </select>

          <select id="my-boats-sort" name="sortBy" class="select select-xs w-28 font-bold text-primary" onchange="this.form.requestSubmit()">
            <option value="newest" ${empty myBoatsSearch.sortBy || myBoatsSearch.sortBy == 'newest' ? 'selected="selected"' : ''}><c:out value="${sortNewestLabel}" /></option>
            <option value="oldest" ${myBoatsSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}><c:out value="${sortOldestLabel}" /></option>
            <option value="nameAsc" ${myBoatsSearch.sortBy == 'nameAsc' ? 'selected="selected"' : ''}><c:out value="${sortNameAscLabel}" /></option>
            <option value="nameDesc" ${myBoatsSearch.sortBy == 'nameDesc' ? 'selected="selected"' : ''}><c:out value="${sortNameDescLabel}" /></option>
          </select>

          <select id="my-boats-page-size" name="pageSize" class="select select-xs w-16 font-bold text-primary" onchange="this.form.requestSubmit()">
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
              <c:url var="manageAvailabilityItemUrl" value="/my-boats/${item.id}/availability">
                <c:param name="return" value="/my-boats" />
              </c:url>
              <c:url var="disableItemUrl" value="/my-boats/${item.id}/disable" />
              <c:url var="enableItemUrl" value="/my-boats/${item.id}/enable" />
              <c:url var="deleteItemUrl" value="/my-boats/${item.id}/delete" />
              <c:set var="publicationImageUrl" value="${imageUrlsByItemId[item.id]}" />
              <c:set var="detailsModalId" value="publication-details-modal-${item.id}" />
              <c:set var="deleteModalId" value="delete-publication-modal-${item.id}" />
              <button type="button" class="flex h-full w-full max-w-sm flex-col gap-2 rounded-xl bg-base-200 p-2 text-left transition hover:bg-base-300 sm:p-3 ${itemActive ? '' : 'opacity-75'}" onclick="document.getElementById('${detailsModalId}').showModal()">
                <div class="h-24 w-full shrink-0 overflow-hidden rounded-lg bg-base-100 sm:h-32">
                  <img src="${publicationImageUrl}" alt="${version.title}" class="h-full w-full object-cover" loading="lazy" />
                </div>
                <div class="flex min-w-0 flex-1 flex-col gap-1">
                  <div class="flex min-w-0 items-start gap-1.5">
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
              </button>
              <paw:detailsModal id="${detailsModalId}" title="${version.title}">
                <div class="overflow-hidden rounded-lg bg-base-100">
                  <img src="${publicationImageUrl}" alt="${version.title}" class="h-56 w-full object-cover" loading="lazy" />
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <span class="badge ${itemActive ? 'badge-success' : 'badge-ghost'} font-bold">
                    <spring:message code="${itemActive ? 'settings.publications.status.active' : 'settings.publications.status.inactive'}" />
                  </span>
                </div>
                <c:if test="${not empty version.location}">
                  <p class="m-0 flex items-center gap-1.5 text-sm text-on-surface-variant">
                    <span class="material-symbols-outlined text-base leading-none text-primary">location_on</span>
                    <span class="break-words"><c:out value="${version.location.name}" /></span>
                  </p>
                </c:if>
                <div class="grid grid-cols-2 gap-2">
                  <div class="rounded-lg bg-base-100 p-3 space-y-1">
                    <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="item.capacityPeople" /></p>
                    <p class="m-0 flex items-center gap-1 text-sm font-bold text-on-surface">
                      <span class="material-symbols-outlined text-base leading-none text-primary">groups</span>
                      <spring:message code="marketplace.card.people" arguments="${version.capacity}" />
                    </p>
                  </div>
                  <div class="rounded-lg bg-base-100 p-3 space-y-1">
                    <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="settings.publications.pricePerHour.label" /></p>
                    <p class="m-0 text-sm font-bold">
                      $<fmt:formatNumber value="${version.price}" type="number" groupingUsed="true" maxFractionDigits="0" />
                    </p>
                  </div>
                </div>
                <div class="flex flex-wrap gap-2 border-t border-outline-variant/20 pt-4">
                  <a href="${itemDetailUrl}" class="btn btn-outline btn-sm no-underline">
                    <span class="material-symbols-outlined text-base">open_in_new</span>
                    <spring:message code="common.viewListing" />
                  </a>
                  <a href="${editItemUrl}" class="btn btn-outline btn-sm no-underline">
                    <span class="material-symbols-outlined text-base">edit</span>
                    <c:out value="${editLabel}" />
                  </a>
                  <a href="${manageAvailabilityItemUrl}" class="btn btn-outline btn-sm no-underline">
                    <span class="material-symbols-outlined text-base">event_available</span>
                    <c:out value="${manageAvailabilityLabel}" />
                  </a>
                  <c:choose>
                    <c:when test="${itemActive}">
                      <form action="${disableItemUrl}" method="post" class="m-0">
                        <button type="submit" class="btn btn-outline btn-sm">
                          <span class="material-symbols-outlined text-base">visibility_off</span>
                          <c:out value="${disableLabel}" />
                        </button>
                      </form>
                    </c:when>
                    <c:otherwise>
                      <form action="${enableItemUrl}" method="post" class="m-0">
                        <button type="submit" class="btn btn-outline btn-sm">
                          <span class="material-symbols-outlined text-base">visibility</span>
                          <c:out value="${enableLabel}" />
                        </button>
                      </form>
                    </c:otherwise>
                  </c:choose>
                  <button type="button" class="btn btn-outline btn-sm text-error" onclick="document.getElementById('${deleteModalId}').showModal()">
                    <span class="material-symbols-outlined text-base">delete</span>
                    <c:out value="${deleteLabel}" />
                  </button>
                </div>
              </paw:detailsModal>
              <paw:confirmModal id="${deleteModalId}" title="${deleteConfirmTitle}" message="${deleteConfirmMessage}" confirmText="${deleteConfirmConfirm}" cancelText="${deleteConfirmCancel}" confirmColor="danger" icon="delete_forever">
                <form action="${deleteItemUrl}" method="post" class="m-0">
                  <paw:button type="submit" color="danger" cssClass="w-full sm:w-auto" text="${deleteConfirmConfirm}" />
                </form>
              </paw:confirmModal>
            </c:forEach>
          </div>
        </c:when>
        <c:otherwise>
          <div class="mx-1 rounded-xl bg-base-200 px-4 py-6 text-center sm:mx-3">
            <c:choose>
              <c:when test="${hasActiveFilters}">
                <p class="m-0 text-sm text-on-surface-variant"><c:out value="${filterEmptyLabel}" /></p>
                <a href="${clearFiltersUrl}" class="btn btn-outline btn-sm mt-4 no-underline">
                  <c:out value="${filtersClearLabel}" />
                </a>
              </c:when>
              <c:otherwise>
                <p class="m-0 text-sm text-on-surface-variant"><spring:message code="settings.publications.empty" /></p>
              </c:otherwise>
            </c:choose>
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
