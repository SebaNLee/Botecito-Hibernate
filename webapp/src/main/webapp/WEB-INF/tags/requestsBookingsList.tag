<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ attribute name="formAction" required="true" type="java.lang.String" %>
<%@ attribute name="sidebarActive" required="true" type="java.lang.String" %>
<%@ attribute name="listMode" required="true" type="java.lang.String" %>

<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="filters.date" var="dateLabel" />
<spring:message code="filters.date.placeholder" var="datePlaceholder" />
<spring:message code="dashboard.filters.status" var="statusFilterLabel" />
<spring:message code="requests.outgoing.status.any" var="statusAnyPlaceholder" />
<spring:message code="optionsPicker.availableOptions" var="statusPanelCaption" />
<spring:message code="optionsPicker.noMatches" var="statusNoMatchCaption" />
<spring:message code="marketplace.filters.apply" var="filtersApplyLabel" />
<spring:message code="marketplace.filters.clear" var="filtersClearLabel" />
<spring:message code="marketplace.filters.title" var="filtersTitleLabel" />
<spring:message code="marketplace.sort.label" var="sortLabel" />
<spring:message code="bookingSearch.field.pageSize" var="pageSizeFieldLabel" />
<c:set var="pageSize" value="${bookingSearch.pageSize != null ? bookingSearch.pageSize : 12}" />
<c:set var="pageHiddenId" value="requests-${sidebarActive}-page-hidden" />
<c:set var="isIncoming" value="${listMode == 'incoming'}" />

<c:url var="clearFiltersUrl" value="${formAction}">
  <c:if test="${sort != 'newest'}">
    <c:param name="sortBy" value="${sort}" />
  </c:if>
  <c:if test="${pageSize != 12}">
    <c:param name="pageSize" value="${pageSize}" />
  </c:if>
</c:url>

<c:if test="${bookingPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="${formAction}">
    <c:param name="page" value="${bookingPage.previousPage}" />
    <c:param name="sortBy" value="${sort}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty bookingSearch.searchQuery}"><c:param name="searchQuery" value="${bookingSearch.searchQuery}" /></c:if>
    <c:if test="${not empty bookingSearch.date}"><c:param name="date" value="${bookingSearch.date}" /></c:if>
    <c:if test="${not empty bookingSearch.status}"><c:param name="status" value="${bookingSearch.status}" /></c:if>
  </c:url>
  <c:url var="nextPageUrl" value="${formAction}">
    <c:param name="page" value="${bookingPage.nextPage}" />
    <c:param name="sortBy" value="${sort}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty bookingSearch.searchQuery}"><c:param name="searchQuery" value="${bookingSearch.searchQuery}" /></c:if>
    <c:if test="${not empty bookingSearch.date}"><c:param name="date" value="${bookingSearch.date}" /></c:if>
    <c:if test="${not empty bookingSearch.status}"><c:param name="status" value="${bookingSearch.status}" /></c:if>
  </c:url>
</c:if>

<paw:toastNotifier />
<form
    id="requests-${sidebarActive}-filters-form"
    action="${formAction}"
    method="get"
    class="grid grid-cols-1 gap-8 md:grid-cols-[18rem_minmax(0,1fr)] lg:grid-cols-[20rem_minmax(0,1fr)] items-start w-full">
  <div class="relative z-40 w-full min-w-0 space-y-6">
    <paw:requestsSidebar active="${sidebarActive}" />
    <input type="hidden" name="page" value="1" id="${pageHiddenId}" />
    <paw:sectionCard icon="tune">
      <jsp:attribute name="title"><c:out value="${filtersTitleLabel}" /></jsp:attribute>
      <jsp:body>
        <div class="space-y-6">
          <div class="w-full min-w-0">
            <paw:datePicker
                id="requests-${sidebarActive}-date"
                dateFieldName="date"
                label="${dateLabel}"
                value="${fn:escapeXml(bookingSearch.date)}"
                placeholder="${datePlaceholder}"
                restrictToAvailability="false"
                offeredDatesJson="[]"
                occupiedDatesJson="[]" />
          </div>

          <paw:optionsPicker
              id="requests-${sidebarActive}-status"
              name="status"
              label="${statusFilterLabel}"
              value="${fn:escapeXml(bookingSearch.status)}"
              placeholder="${statusAnyPlaceholder}"
              icon="flag"
              variant="inline"
              optionsUrl="/booking-status-options"
              panelCaption="${statusPanelCaption}"
              emptyCaption="${statusNoMatchCaption}"
              containerClass="w-full" />

          <div class="flex flex-col gap-3">
            <paw:button type="submit" color="primary" fullWidth="true" text="${filtersApplyLabel}" />
            <a href="${clearFiltersUrl}" class="btn btn-outline btn-block no-underline">
              <c:out value="${filtersClearLabel}" />
            </a>
          </div>
        </div>
      </jsp:body>
    </paw:sectionCard>
  </div>

  <section class="relative z-0 min-w-0 space-y-6">
    <div id="booking-requests-list" class="scroll-mt-24 space-y-8">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-4">
        <div class="min-w-0 w-full flex-1">
          <label class="input input-lg flex w-full min-w-0 items-center gap-3 rounded-full bg-base-100 shadow-sm">
            <span class="material-symbols-outlined text-outline" aria-hidden="true">search</span>
            <input
                type="search"
                name="searchQuery"
                value="${fn:escapeXml(bookingSearch.searchQuery)}"
                placeholder="${searchLabel}"
                aria-label="${searchLabel}"
                maxlength="100"
                class="min-w-0 grow border-none bg-transparent p-0 text-on-surface placeholder:text-outline outline-none focus:outline-none focus-visible:outline-none focus:ring-0" />
          </label>
        </div>
        <div class="flex shrink-0 flex-nowrap items-center gap-2 text-sm font-medium text-on-surface-variant lg:justify-end">
          <label for="requests-${sidebarActive}-sort" class="shrink-0 whitespace-nowrap"><c:out value="${sortLabel}" /></label>
          <select
              id="requests-${sidebarActive}-sort"
              name="sortBy"
              class="select select-sm w-32 max-w-[40vw] shrink-0 font-bold text-primary sm:max-w-none sm:w-36"
              onchange="document.getElementById('${pageHiddenId}').value='1'; this.form.requestSubmit();">
            <option value="newest" ${empty bookingSearch.sortBy || bookingSearch.sortBy == 'newest' ? 'selected="selected"' : ''}>
              <spring:message code="marketplace.sort.newest" />
            </option>
            <option value="oldest" ${bookingSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}>
              <spring:message code="marketplace.sort.oldest" />
            </option>
            <option value="start_asc" ${bookingSearch.sortBy == 'start_asc' ? 'selected="selected"' : ''}>
              <spring:message code="requests.outgoing.sort.startAsc" />
            </option>
            <option value="start_desc" ${bookingSearch.sortBy == 'start_desc' ? 'selected="selected"' : ''}>
              <spring:message code="requests.outgoing.sort.startDesc" />
            </option>
          </select>
          <label for="requests-${sidebarActive}-page-size" class="shrink-0 ml-2"><c:out value="${pageSizeFieldLabel}" /></label>
          <select
              id="requests-${sidebarActive}-page-size"
              name="pageSize"
              class="select select-sm w-20 font-bold text-primary"
              onchange="document.getElementById('${pageHiddenId}').value='1'; this.form.requestSubmit();">
            <option value="6" ${pageSize == 6 ? 'selected="selected"' : ''}>6</option>
            <option value="12" ${pageSize == 12 ? 'selected="selected"' : ''}>12</option>
            <option value="18" ${pageSize == 18 ? 'selected="selected"' : ''}>18</option>
          </select>
        </div>
      </div>

      <div class="min-w-0">
        <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words md:text-4xl">
          <spring:message code="bookings.title" />
        </h1>
        <p class="text-on-surface-variant mt-2 m-0">
          <c:choose>
            <c:when test="${isIncoming}"><spring:message code="requests.incoming.subtitle" /></c:when>
            <c:otherwise><spring:message code="bookings.subtitle" /></c:otherwise>
          </c:choose>
        </p>
        <h2 class="text-2xl font-extrabold tracking-tight text-on-background m-0 mt-6 break-words md:text-4xl">
          <c:choose>
            <c:when test="${isIncoming}"><spring:message code="profile.bookings.title" /></c:when>
            <c:otherwise><spring:message code="profile.sentBookings.title" /></c:otherwise>
          </c:choose>
        </h2>
        <p class="text-on-surface-variant mt-2 m-0">
          <c:choose>
            <c:when test="${isIncoming}">
              <spring:message code="requests.incoming.resultsCount" arguments="${bookingsCount}" />
            </c:when>
            <c:otherwise>
              <spring:message code="requests.outgoing.resultsCount" arguments="${bookingsCount}" />
            </c:otherwise>
          </c:choose>
        </p>
      </div>

      <c:choose>
        <c:when test="${not empty bookings}">
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 sm:gap-4">
            <c:forEach var="b" items="${bookings}">
              <c:set var="badgeClass" value="badge-ghost" />
              <c:if test="${b.status.name() == 'PENDING'}"><c:set var="badgeClass" value="badge-warning" /></c:if>
              <c:if test="${b.status.name() == 'ACCEPTED' || b.status.name() == 'CONFIRMED'}"><c:set var="badgeClass" value="badge-info" /></c:if>
              <c:if test="${b.status.name() == 'PAID'}"><c:set var="badgeClass" value="badge-success" /></c:if>
              <c:if test="${b.status.name() == 'REJECTED' || b.status.name() == 'CANCELLED' || b.status.name() == 'REFUSED'}"><c:set var="badgeClass" value="badge-error" /></c:if>
              <c:choose>
                <c:when test="${isIncoming}">
                  <spring:message code="requests.incoming.detail.title" arguments="${b.id}" var="detailTitleVar" />
                </c:when>
                <c:otherwise>
                  <spring:message code="requests.outgoing.detail.title" arguments="${b.id}" var="detailTitleVar" />
                </c:otherwise>
              </c:choose>
              <c:set var="detailModalId" value="nuevo-booking-detail-${sidebarActive}-${b.id}" />
              <button type="button" class="flex h-full w-full max-w-sm flex-col gap-2 rounded-xl bg-base-200 p-2 text-left transition hover:bg-base-300 sm:p-3" onclick="document.getElementById('${detailModalId}').showModal()">
                <div class="flex h-24 w-full shrink-0 items-center justify-center overflow-hidden rounded-lg bg-base-100 sm:h-32">
                  <span class="material-symbols-outlined text-4xl text-outline/40" aria-hidden="true">calendar_month</span>
                </div>
                <div class="flex min-w-0 flex-1 flex-col gap-1">
                  <div class="flex min-w-0 items-start gap-1.5">
                    <p class="m-0 min-w-0 flex-1 break-words text-xs font-extrabold text-on-surface line-clamp-2 sm:text-sm">
                      <c:out value="${b.versionTitle}" />
                    </p>
                    <span class="badge ${badgeClass} badge-xs shrink-0 font-bold"><spring:message code="booking.status.${b.status}" /></span>
                  </div>
                  <p class="m-0 truncate text-[10px] text-on-surface-variant sm:text-xs" title="UTC"><c:out value="${b.start}" /> → <c:out value="${b.end}" /></p>
                  <p class="m-0 mt-auto truncate text-[11px] font-semibold text-on-surface-variant sm:text-xs">#<c:out value="${b.id}" /></p>
                </div>
              </button>
              <paw:detailsModal id="${detailModalId}" title="${detailTitleVar}" layout="split">
                <jsp:attribute name="aside">
                  <div class="flex h-40 w-full items-center justify-center overflow-hidden rounded-lg bg-base-100 sm:h-48">
                    <span class="material-symbols-outlined text-5xl text-outline/30" aria-hidden="true">event</span>
                  </div>
                  <div class="flex flex-wrap items-center gap-2">
                    <span class="badge ${badgeClass} font-bold"><spring:message code="booking.status.${b.status}" /></span>
                  </div>
                  <div class="rounded-lg bg-base-100 p-3 space-y-1">
                    <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.bookings.schedule.label" /></p>
                    <p class="m-0 text-xs font-mono text-on-surface-variant">UTC</p>
                    <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${b.start}" /></p>
                    <p class="m-0 text-xs text-on-surface-variant">→ <c:out value="${b.end}" /></p>
                  </div>
                </jsp:attribute>
                <jsp:body>
                  <c:choose>
                    <c:when test="${isIncoming}">
                      <div class="rounded-lg bg-base-100 p-3 space-y-1">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="item.title" /></p>
                        <p class="m-0 text-sm font-bold text-on-surface break-words"><c:out value="${b.versionTitle}" /></p>
                      </div>
                      <div class="rounded-lg bg-base-100 p-3 space-y-1">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="requests.incoming.table.guest" /></p>
                        <p class="m-0 font-mono text-sm font-bold text-on-surface">${b.guestId}</p>
                      </div>
                      <div class="rounded-lg bg-base-100 p-3 space-y-1">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="requests.outgoing.table.version" /></p>
                        <p class="m-0 font-mono text-sm font-bold text-on-surface">${b.versionId}</p>
                      </div>
                    </c:when>
                    <c:otherwise>
                      <div class="rounded-lg bg-base-100 p-3 space-y-1">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="item.title" /></p>
                        <p class="m-0 text-sm font-bold text-on-surface break-words"><c:out value="${b.versionTitle}" /></p>
                      </div>
                      <div class="rounded-lg bg-base-100 p-3 space-y-1">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="requests.outgoing.table.version" /></p>
                        <p class="m-0 font-mono text-sm font-bold text-on-surface">${b.versionId}</p>
                      </div>
                    </c:otherwise>
                  </c:choose>
                  <c:if test="${not empty b.msg}">
                    <div class="rounded-lg bg-base-100 p-3 border-l-4 border-primary">
                      <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="requests.outgoing.table.message" /></p>
                      <p class="m-0 mt-1 break-words text-sm text-on-surface whitespace-pre-line"><c:out value="${b.msg}" /></p>
                    </div>
                  </c:if>
                </jsp:body>
              </paw:detailsModal>
            </c:forEach>
          </div>
        </c:when>
        <c:otherwise>
          <div class="card bg-base-100 shadow-sm">
            <div class="card-body items-center gap-4 p-10 text-center">
              <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                <c:choose>
                  <c:when test="${isIncoming}">
                    <span class="material-symbols-outlined text-4xl" aria-hidden="true">inbox</span>
                  </c:when>
                  <c:otherwise>
                    <span class="material-symbols-outlined text-4xl" aria-hidden="true">send</span>
                  </c:otherwise>
                </c:choose>
              </div>
              <p class="m-0 max-w-lg text-on-surface-variant">
                <c:choose>
                  <c:when test="${isIncoming}"><spring:message code="requests.incoming.empty" /></c:when>
                  <c:otherwise><spring:message code="profile.sentBookings.empty" /></c:otherwise>
                </c:choose>
              </p>
              <a href="${clearFiltersUrl}" class="btn btn-primary btn-sm no-underline">
                <spring:message code="marketplace.filters.clear" />
              </a>
            </div>
          </div>
        </c:otherwise>
      </c:choose>

      <c:if test="${bookingPage.totalPages > 1}">
        <nav class="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
          <c:choose>
            <c:when test="${bookingPage.hasPrevious}">
              <a href="${previousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2"><span class="material-symbols-outlined text-sm">arrow_back</span><spring:message code="marketplace.pagination.previous" /></a>
            </c:when>
            <c:otherwise><span class="btn btn-outline btn-sm btn-disabled gap-2"><span class="material-symbols-outlined text-sm">arrow_back</span><spring:message code="marketplace.pagination.previous" /></span></c:otherwise>
          </c:choose>
          <span><spring:message code="marketplace.pagination.page" arguments="${bookingPage.page},${bookingPage.totalPages}" /></span>
          <c:choose>
            <c:when test="${bookingPage.hasNext}">
              <a href="${nextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2"><spring:message code="marketplace.pagination.next" /><span class="material-symbols-outlined text-sm">arrow_forward</span></a>
            </c:when>
            <c:otherwise><span class="btn btn-outline btn-sm btn-disabled gap-2"><spring:message code="marketplace.pagination.next" /><span class="material-symbols-outlined text-sm">arrow_forward</span></span></c:otherwise>
          </c:choose>
        </nav>
      </c:if>
    </div>
  </section>
</form>
