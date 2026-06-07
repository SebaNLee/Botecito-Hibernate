<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<spring:message code="profile.followers" var="followersLabel" />
<spring:message code="profile.rating.noneShort" var="ratingNoneShortLabel" />
<spring:message code="profile.editMyProfile" var="editMyProfileLabel" />
<spring:message code="profile.listings.empty" var="listingsEmptyLabel" />
<spring:message code="profile.reviews.empty" var="reviewsEmptyLabel" />
<spring:message code="profile.listings.filter.empty.title" var="listingsFilterEmptyTitleLabel" />
<spring:message code="profile.listings.filter.empty.message" var="listingsFilterEmptyMessageLabel" />
<spring:message code="profile.reviews.filter.empty.title" var="reviewsFilterEmptyTitleLabel" />
<spring:message code="profile.reviews.filter.empty.message" var="reviewsFilterEmptyMessageLabel" />
<spring:message code="marketplace.empty.clear" var="filterEmptyClearLabel" />
<spring:message code="subscription.subscribe" var="subscriptionSubscribeLabel" />
<spring:message code="subscription.unsubscribe" var="subscriptionUnsubscribeLabel" />
<spring:message code="marketplace.pagination.previous" var="paginationPreviousLabel" />
<spring:message code="marketplace.pagination.next" var="paginationNextLabel" />
<spring:message code="marketplace.sort.label" var="sortLabel" />
<spring:message code="myBoats.sort.nameAsc" var="sortNameAscLabel" />
<spring:message code="myBoats.sort.nameDesc" var="sortNameDescLabel" />
<spring:message code="myBoats.pageSize" var="pageSizeFieldLabel" />

<c:set var="hasActiveListingsFilters" value="${profileView.sortBy != 'newest' or profileView.pageSize != 12 or profileView.page > 1}" />
<c:set var="hasActiveReviewsFilters" value="${profileView.page > 1}" />
<c:set var="showListingsFilterEmpty" value="${hasValidationErrors or hasActiveListingsFilters or listingsTotal > 0}" />
<c:set var="showReviewsFilterEmpty" value="${hasValidationErrors or hasActiveReviewsFilters or reviewsTotal > 0}" />

<c:url var="settingsUrl" value="/settings" />
<c:url var="clearProfileListingsFiltersUrl" value="/profiles/${user.id}" />
<c:url var="listingsTabUrl" value="/profiles/${user.id}">
  <c:if test="${profileView.sortBy != 'newest'}"><c:param name="sortBy" value="${profileView.sortBy}" /></c:if>
  <c:if test="${profileView.pageSize != 12}"><c:param name="pageSize" value="${profileView.pageSize}" /></c:if>
</c:url>
<c:url var="reviewsTabUrl" value="/profiles/${user.id}">
  <c:param name="tab" value="reviews" />
  <c:if test="${activeTab == 'reviews' && profileView.page > 1}">
    <c:param name="page" value="${profileView.page}" />
  </c:if>
</c:url>
<c:url var="profileListingsUrl" value="/profiles/${user.id}" />
<c:set var="userFirstName" value="${user.firstName != null ? fn:trim(user.firstName) : ''}" />
<c:set var="userLastName" value="${user.lastName != null ? fn:trim(user.lastName) : ''}" />
<c:set var="hasFullName" value="${not empty userFirstName or not empty userLastName}" />
<c:set var="initials" value="" />
<c:if test="${not empty userFirstName}">
  <c:set var="initials" value="${fn:substring(userFirstName, 0, 1)}" />
</c:if>
<c:if test="${not empty userLastName}">
  <c:set var="initials" value="${initials}${fn:substring(userLastName, 0, 1)}" />
</c:if>
<c:if test="${empty initials and not empty user.email}">
  <c:set var="initials" value="${fn:substring(user.email, 0, 1)}" />
</c:if>
<c:set var="initials" value="${fn:toUpperCase(initials)}" />
<spring:message code="page.title.profile" var="titleProfile" />

<paw:layout title="${titleProfile} - Botecito" mainClass="pt-24 pb-10 w-full max-w-7xl mx-auto px-6" scripts="toast">
  <paw:toastNotifier />
  <section class="space-y-6 min-w-0">

    <%-- Header card --%>
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-6">
        <div class="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex min-w-0 items-center gap-4">
            <div class="avatar placeholder shrink-0">
              <div class="bg-primary text-primary-content rounded-full w-16 h-16 flex items-center justify-center">
                <span class="font-bold text-2xl font-headline">${initials}</span>
              </div>
            </div>
            <div class="min-w-0">
              <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0 break-words">
                <c:choose>
                  <c:when test="${hasFullName}">
                    <c:out value="${userFirstName}" />
                    <c:if test="${not empty userFirstName and not empty userLastName}"> </c:if>
                    <c:out value="${userLastName}" />
                  </c:when>
                  <c:otherwise><c:out value="${user.email}" /></c:otherwise>
                </c:choose>
              </h1>
              <div class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm font-semibold text-on-surface-variant">
                <span class="flex items-center gap-1">
                  <span class="material-symbols-outlined text-base">group</span>
                  <spring:message code="profile.followersCount" arguments="${followersCount}" />
                  <span><c:out value="${followersLabel}" /></span>
                </span>
                <span class="flex items-center gap-1">
                  <span class="material-symbols-outlined text-base icon-star-filled">star</span>
                  <c:choose>
                    <c:when test="${averageRating != null}">
                      <span class="font-bold text-on-surface">
                        <fmt:formatNumber value="${averageRating}" type="number" minFractionDigits="1" maxFractionDigits="1" />
                      </span>
                      <span class="text-outline">
                        (<spring:message code="profile.rating.reviewsCount" arguments="${reviewsTotal}" />)
                      </span>
                    </c:when>
                    <c:otherwise>
                      <span><c:out value="${ratingNoneShortLabel}" /></span>
                    </c:otherwise>
                  </c:choose>
                </span>
              </div>
            </div>
          </div>

          <div class="shrink-0 w-full sm:w-auto">
            <c:choose>
              <c:when test="${isSelf}">
                <paw:button href="${settingsUrl}" color="primary" variant="outline" icon="edit"
                            text="${editMyProfileLabel}" cssClass="w-full sm:w-auto" />
              </c:when>
              <c:when test="${isSubscribed}">
                <c:url var="unsubscribeProfileUrl" value="/profiles/${user.id}/unsubscribe" />
                <form action="${unsubscribeProfileUrl}" method="post" class="m-0">
                  <paw:profileViewHiddenFields view="${profileView}" />
                  <paw:button type="submit" color="outline" icon="notifications_off"
                              text="${subscriptionUnsubscribeLabel}" cssClass="w-full sm:w-auto" />
                </form>
              </c:when>
              <c:otherwise>
                <c:url var="subscribeProfileUrl" value="/profiles/${user.id}/subscribe" />
                <form action="${subscribeProfileUrl}" method="post" class="m-0">
                  <paw:profileViewHiddenFields view="${profileView}" />
                  <paw:button type="submit" color="secondary" icon="notifications"
                              text="${subscriptionSubscribeLabel}" cssClass="w-full sm:w-auto" />
                </form>
              </c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>
    </div>

    <%-- Tabs card: Boats / Reviews --%>
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-0">

        <%-- Tab bar --%>
        <div role="tablist" class="flex border-b border-outline-variant/20 px-2 pt-2">
          <a role="tab" href="${listingsTabUrl}"
             class="shrink-0 flex items-center justify-center gap-2 whitespace-nowrap px-6 py-3 text-sm font-bold no-underline border-b-2 -mb-px transition-colors
                    ${activeTab == 'listings' ? 'border-primary text-primary' : 'border-transparent text-on-surface-variant hover:text-on-surface'}">
            <span class="material-symbols-outlined text-base shrink-0">directions_boat</span>
            <c:choose>
              <c:when test="${listingsTotal == 1}">
                <spring:message code="profile.listings.tab.singular" />
              </c:when>
              <c:otherwise>
                <spring:message code="profile.listings.tab.plural" arguments="${listingsTotal}" />
              </c:otherwise>
            </c:choose>
          </a>
          <a role="tab" href="${reviewsTabUrl}"
             class="shrink-0 flex items-center justify-center gap-2 whitespace-nowrap px-6 py-3 text-sm font-bold no-underline border-b-2 -mb-px transition-colors
                    ${activeTab == 'reviews' ? 'border-primary text-primary' : 'border-transparent text-on-surface-variant hover:text-on-surface'}">
            <span class="material-symbols-outlined text-base shrink-0">star</span>
            <c:choose>
              <c:when test="${reviewsTotal == 1}">
                <spring:message code="profile.reviews.tab.singular" />
              </c:when>
              <c:otherwise>
                <spring:message code="profile.reviews.tab.plural" arguments="${reviewsTotal}" />
              </c:otherwise>
            </c:choose>
          </a>
        </div>

        <div class="p-6">
          <c:choose>

            <%-- Listings panel --%>
            <c:when test="${activeTab == 'listings'}">
              <form id="profile-listings-filters-form" action="${profileListingsUrl}" method="get" class="mb-6 w-full">
                <input type="hidden" name="page" value="1" />
                <div class="flex shrink-0 flex-wrap items-center justify-end gap-2 text-sm font-medium text-on-surface-variant">
                  <label for="profile-listings-sort" class="shrink-0 whitespace-nowrap"><c:out value="${sortLabel}" /></label>
                  <select
                      id="profile-listings-sort"
                      name="sortBy"
                      class="select select-sm w-32 max-w-[40vw] shrink-0 font-bold text-primary sm:max-w-none sm:w-36"
                      onchange="this.form.requestSubmit()">
                    <option value="newest" ${profileView.sortBy == 'newest' ? 'selected="selected"' : ''}>
                      <spring:message code="marketplace.sort.newest" />
                    </option>
                    <option value="oldest" ${profileView.sortBy == 'oldest' ? 'selected="selected"' : ''}>
                      <spring:message code="marketplace.sort.oldest" />
                    </option>
                    <option value="nameAsc" ${profileView.sortBy == 'nameAsc' ? 'selected="selected"' : ''}><c:out value="${sortNameAscLabel}" /></option>
                    <option value="nameDesc" ${profileView.sortBy == 'nameDesc' ? 'selected="selected"' : ''}><c:out value="${sortNameDescLabel}" /></option>
                  </select>
                  <label for="profile-listings-page-size" class="shrink-0 ml-2"><c:out value="${pageSizeFieldLabel}" /></label>
                  <select
                      id="profile-listings-page-size"
                      name="pageSize"
                      class="select select-sm w-20 font-bold text-primary"
                      onchange="this.form.requestSubmit()">
                    <option value="6" ${profileView.pageSize == 6 ? 'selected="selected"' : ''}>6</option>
                    <option value="12" ${profileView.pageSize == 12 ? 'selected="selected"' : ''}>12</option>
                    <option value="18" ${profileView.pageSize == 18 ? 'selected="selected"' : ''}>18</option>
                  </select>
                </div>
              </form>

              <c:choose>
                <c:when test="${empty listings}">
                  <c:choose>
                    <c:when test="${showListingsFilterEmpty}">
                      <div class="card bg-base-100 shadow-sm">
                        <div class="card-body items-center gap-4 p-10 text-center">
                          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                            <span class="material-symbols-outlined text-4xl" aria-hidden="true">directions_boat</span>
                          </div>
                          <div class="max-w-lg">
                            <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><c:out value="${listingsFilterEmptyTitleLabel}" /></h2>
                            <p class="m-0 mt-2 text-on-surface-variant"><c:out value="${listingsFilterEmptyMessageLabel}" /></p>
                          </div>
                          <a href="${clearProfileListingsFiltersUrl}" class="btn btn-primary no-underline" data-clear-list-filters>
                            <c:out value="${filterEmptyClearLabel}" />
                          </a>
                        </div>
                      </div>
                    </c:when>
                    <c:otherwise>
                      <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/45 px-4 py-3 text-sm text-on-surface-variant">
                        <c:out value="${listingsEmptyLabel}" />
                      </p>
                    </c:otherwise>
                  </c:choose>
                </c:when>
                <c:otherwise>
                  <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                    <c:forEach items="${listings}" var="item">
                      <paw:listingCard item="${item}" />
                    </c:forEach>
                  </div>
                  <c:if test="${listingsPage.totalPages > 1}">
                    <c:url var="listingsPreviousPageUrl" value="/profiles/${user.id}">
                      <c:param name="page" value="${listingsPage.previousPage}" />
                      <c:param name="sortBy" value="${profileView.sortBy}" />
                      <c:param name="pageSize" value="${listingsPage.pageSize}" />
                    </c:url>
                    <c:url var="listingsNextPageUrl" value="/profiles/${user.id}">
                      <c:param name="page" value="${listingsPage.nextPage}" />
                      <c:param name="sortBy" value="${profileView.sortBy}" />
                      <c:param name="pageSize" value="${listingsPage.pageSize}" />
                    </c:url>
                    <nav class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
                      <c:choose>
                        <c:when test="${listingsPage.hasPrevious}">
                          <a href="${listingsPreviousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
                            <span class="material-symbols-outlined text-sm">arrow_back</span>
                            <c:out value="${paginationPreviousLabel}" />
                          </a>
                        </c:when>
                        <c:otherwise>
                          <span class="btn btn-outline btn-sm btn-disabled gap-2">
                            <span class="material-symbols-outlined text-sm">arrow_back</span>
                            <c:out value="${paginationPreviousLabel}" />
                          </span>
                        </c:otherwise>
                      </c:choose>
                      <span>
                        <spring:message code="marketplace.pagination.page" arguments="${listingsPage.page},${listingsPage.totalPages}" />
                      </span>
                      <c:choose>
                        <c:when test="${listingsPage.hasNext}">
                          <a href="${listingsNextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
                            <c:out value="${paginationNextLabel}" />
                            <span class="material-symbols-outlined text-sm">arrow_forward</span>
                          </a>
                        </c:when>
                        <c:otherwise>
                          <span class="btn btn-outline btn-sm btn-disabled gap-2">
                            <c:out value="${paginationNextLabel}" />
                            <span class="material-symbols-outlined text-sm">arrow_forward</span>
                          </span>
                        </c:otherwise>
                      </c:choose>
                    </nav>
                  </c:if>
                </c:otherwise>
              </c:choose>
            </c:when>

            <%-- Reviews panel --%>
            <c:otherwise>
              <c:choose>
                <c:when test="${empty reviews}">
                  <c:choose>
                    <c:when test="${showReviewsFilterEmpty}">
                      <div class="card bg-base-100 shadow-sm">
                        <div class="card-body items-center gap-4 p-10 text-center">
                          <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                            <span class="material-symbols-outlined text-4xl" aria-hidden="true">star</span>
                          </div>
                          <div class="max-w-lg">
                            <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><c:out value="${reviewsFilterEmptyTitleLabel}" /></h2>
                            <p class="m-0 mt-2 text-on-surface-variant"><c:out value="${reviewsFilterEmptyMessageLabel}" /></p>
                          </div>
                          <a href="${reviewsTabUrl}" class="btn btn-primary no-underline" data-clear-list-filters>
                            <c:out value="${filterEmptyClearLabel}" />
                          </a>
                        </div>
                      </div>
                    </c:when>
                    <c:otherwise>
                      <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/45 px-4 py-3 text-sm text-on-surface-variant">
                        <c:out value="${reviewsEmptyLabel}" />
                      </p>
                    </c:otherwise>
                  </c:choose>
                </c:when>
                <c:otherwise>
                  <div class="flex flex-col gap-3">
                    <c:forEach items="${reviews}" var="review">
                      <paw:reviewCard review="${review}" reviewDate="${reviewDatesById[review.id]}" showReviewer="true" />
                    </c:forEach>
                  </div>
                  <c:if test="${reviewsPage.totalPages > 1}">
                    <c:url var="reviewsPreviousPageUrl" value="/profiles/${user.id}">
                      <c:param name="tab" value="reviews" />
                      <c:param name="page" value="${reviewsPage.previousPage}" />
                    </c:url>
                    <c:url var="reviewsNextPageUrl" value="/profiles/${user.id}">
                      <c:param name="tab" value="reviews" />
                      <c:param name="page" value="${reviewsPage.nextPage}" />
                    </c:url>
                    <nav class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
                      <c:choose>
                        <c:when test="${reviewsPage.hasPrevious}">
                          <a href="${reviewsPreviousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
                            <span class="material-symbols-outlined text-sm">arrow_back</span>
                            <c:out value="${paginationPreviousLabel}" />
                          </a>
                        </c:when>
                        <c:otherwise>
                          <span class="btn btn-outline btn-sm btn-disabled gap-2">
                            <span class="material-symbols-outlined text-sm">arrow_back</span>
                            <c:out value="${paginationPreviousLabel}" />
                          </span>
                        </c:otherwise>
                      </c:choose>
                      <span>
                        <spring:message code="marketplace.pagination.page" arguments="${reviewsPage.page},${reviewsPage.totalPages}" />
                      </span>
                      <c:choose>
                        <c:when test="${reviewsPage.hasNext}">
                          <a href="${reviewsNextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
                            <c:out value="${paginationNextLabel}" />
                            <span class="material-symbols-outlined text-sm">arrow_forward</span>
                          </a>
                        </c:when>
                        <c:otherwise>
                          <span class="btn btn-outline btn-sm btn-disabled gap-2">
                            <c:out value="${paginationNextLabel}" />
                            <span class="material-symbols-outlined text-sm">arrow_forward</span>
                          </span>
                        </c:otherwise>
                      </c:choose>
                    </nav>
                  </c:if>
                </c:otherwise>
              </c:choose>
            </c:otherwise>

          </c:choose>
        </div>
      </div>
    </div>

  </section>
</paw:layout>
