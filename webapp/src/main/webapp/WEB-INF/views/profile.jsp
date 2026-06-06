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
<spring:message code="subscription.subscribe" var="subscriptionSubscribeLabel" />
<spring:message code="subscription.unsubscribe" var="subscriptionUnsubscribeLabel" />
<spring:message code="marketplace.pagination.previous" var="paginationPreviousLabel" />
<spring:message code="marketplace.pagination.next" var="paginationNextLabel" />

<c:url var="settingsUrl" value="/settings" />
<c:url var="listingsTabUrl" value="/profiles/${user.id}"><c:param name="tab" value="listings" /></c:url>
<c:url var="reviewsTabUrl" value="/profiles/${user.id}"><c:param name="tab" value="reviews" /></c:url>
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

<paw:layout title="${titleProfile} - Botecito" mainClass="pt-24 pb-10 w-full max-w-7xl mx-auto px-6">
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
              <c:choose>
                <c:when test="${empty listings}">
                  <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/45 px-4 py-3 text-sm text-on-surface-variant">
                    <c:out value="${listingsEmptyLabel}" />
                  </p>
                </c:when>
                <c:otherwise>
                  <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                    <c:forEach items="${listings}" var="item">
                      <paw:listingCard item="${item}" coverSrc="${imageUrlsByItemId[item.id]}" />
                    </c:forEach>
                  </div>
                  <c:if test="${listingsPage.totalPages > 1}">
                    <c:url var="listingsPreviousPageUrl" value="/profiles/${user.id}">
                      <c:param name="tab" value="listings" />
                      <c:param name="listingsPage" value="${listingsPage.previousPage}" />
                      <c:param name="listingsPageSize" value="${listingsPage.pageSize}" />
                    </c:url>
                    <c:url var="listingsNextPageUrl" value="/profiles/${user.id}">
                      <c:param name="tab" value="listings" />
                      <c:param name="listingsPage" value="${listingsPage.nextPage}" />
                      <c:param name="listingsPageSize" value="${listingsPage.pageSize}" />
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
                  <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/45 px-4 py-3 text-sm text-on-surface-variant">
                    <c:out value="${reviewsEmptyLabel}" />
                  </p>
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
                      <c:param name="reviewsPage" value="${reviewsPage.previousPage}" />
                      <c:param name="reviewsPageSize" value="${reviewsPage.pageSize}" />
                    </c:url>
                    <c:url var="reviewsNextPageUrl" value="/profiles/${user.id}">
                      <c:param name="tab" value="reviews" />
                      <c:param name="reviewsPage" value="${reviewsPage.nextPage}" />
                      <c:param name="reviewsPageSize" value="${reviewsPage.pageSize}" />
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
