<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
<c:url var="myBoatsUrl" value="/my-boats" />
<c:url var="publishUrl" value="/publish" />
<spring:message code="nav.publishCta" var="publishCtaLabel" />
<spring:message code="profile.publications.edit" var="editLabel" />
<spring:message code="profile.publications.manageAvailability" var="manageAvailabilityLabel" />
<spring:message code="profile.publications.enable" var="enableLabel" />
<spring:message code="profile.publications.disable" var="disableLabel" />
<spring:message code="profile.publications.delete" var="deleteLabel" />
<spring:message code="profile.publications.delete.confirm.title" var="deleteConfirmTitle" />
<spring:message code="profile.publications.delete.confirm.message" var="deleteConfirmMessage" />
<spring:message code="profile.publications.delete.confirm.confirm" var="deleteConfirmConfirm" />
<spring:message code="profile.publications.delete.confirm.cancel" var="deleteConfirmCancel" />
<spring:message code="profile.publications.viewDetail" var="publicationViewDetailLabel" />


<paw:layout title="Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6" scripts="toast,publish-wizard,edit-wizard">
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
        <form action="${myBoatsUrl}" method="get" class="flex items-center justify-end gap-3 text-sm font-medium text-on-surface-variant">
          <input type="hidden" name="page" value="1" />
          <label for="my-boats-page-size" class="shrink-0"><spring:message code="myBoats.pageSize" /></label>
          <select id="my-boats-page-size" name="pageSize" class="select select-sm w-20 font-bold text-primary" onchange="this.form.submit()">
            <option value="6" ${param.pageSize == 6 ? 'selected="selected"' : ''}>6</option>
            <option value="12" ${empty param.pageSize || param.pageSize == 12 ? 'selected="selected"' : ''}>12</option>
            <option value="18" ${param.pageSize == 18 ? 'selected="selected"' : ''}>18</option>
          </select>
        </form>
      </div>

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
                            <spring:message code="${itemActive ? 'profile.publications.status.active' : 'profile.publications.status.inactive'}" />
                          </span>
                        </div>
                        <p class="m-0 mt-auto text-[11px] font-bold text-on-surface sm:text-xs">
                          $<fmt:formatNumber value="${version.price}" type="number" groupingUsed="true" maxFractionDigits="0" />
                          <span class="font-normal text-on-surface-variant"> · <spring:message code="marketplace.card.perHour" /></span>
                        </p>
                      </div>
                    </button>
                    <paw:detailsModal id="${detailsModalId}" title="${version.title}">
                      <div class="overflow-hidden rounded-lg bg-base-100">
                        <img src="${publicationImageUrl}" alt="${version.title}" class="h-56 w-full object-cover" loading="lazy" />
                      </div>
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="badge ${itemActive ? 'badge-success' : 'badge-ghost'} font-bold">
                          <spring:message code="${itemActive ? 'profile.publications.status.active' : 'profile.publications.status.inactive'}" />
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
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.publications.pricePerHour.label" /></p>
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
                  <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.publications.empty" /></p>
                </div>
              </c:otherwise>
            </c:choose>


      <c:if test="${itemPage.totalPages > 1}">
        <c:url var="previousPageUrl" value="/my-boats">
          <c:param name="page" value="${itemPage.previousPage}" />
          <c:param name="pageSize" value="${itemPage.pageSize}" />
        </c:url>
        <c:url var="nextPageUrl" value="/my-boats">
          <c:param name="page" value="${itemPage.nextPage}" />
          <c:param name="pageSize" value="${itemPage.pageSize}" />
        </c:url>
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


  </section>
</paw:layout>
