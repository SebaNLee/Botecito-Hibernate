<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<fmt:setLocale value="es_AR" />
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ attribute name="formActionPath" required="true" type="java.lang.String" %>
<%@ attribute name="sidebarActive" required="true" type="java.lang.String" %>
<%@ attribute name="listMode" required="true" type="java.lang.String" %>

<spring:message code="landing.hero.search" var="searchLabel" />
<spring:message code="filters.date" var="dateLabel" />
<spring:message code="filters.date.placeholder" var="datePlaceholder" />
<spring:message code="bookingSearch.field.status" var="statusFilterLabel" />
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
<c:set var="filterFormId" value="requests-${sidebarActive}-filters-form" />
<spring:message code="requests.action.accept" var="actionAcceptLabel" />
<spring:message code="requests.action.reject" var="actionRejectLabel" />
<spring:message code="requests.action.cancel" var="actionCancelLabel" />
<spring:message code="requests.action.submitPayment" var="actionSubmitPaymentLabel" />
<spring:message code="requests.action.confirmPayment" var="actionConfirmPaymentLabel" />
<spring:message code="requests.action.rejectPayment" var="actionRejectPaymentLabel" />
<spring:message code="requests.detail.timesStoredUtc" var="timesUtcCaption" />
<spring:message code="requests.detail.listingTimezone" var="listingTimezoneLabel" />
<spring:message code="requests.detail.host" var="hostLabel" />
<spring:message code="requests.detail.alias" var="aliasLabel" />
<spring:message code="requests.detail.created" var="createdLabel" />
<spring:message code="requests.detail.updated" var="updatedLabel" />
<spring:message code="payment.guestReply.input.label" var="guestReplyLabel" />
<spring:message code="payment.reply.placeholder" var="guestReplyPlaceholder" />
<spring:message code="payment.refusal.reason.label" var="rejectPaymentReasonLabel" />
<spring:message code="requests.paymentProof.view" var="viewPaymentProofLabel" />
<spring:message code="itemDetail.reviews.leave" var="reviewLeaveLabel" />
<spring:message code="itemDetail.reviews.rating" var="reviewRatingLabel" />
<spring:message code="itemDetail.reviews.comment" var="reviewCommentLabel" />
<spring:message code="settings.reviews.submit" var="reviewSubmitLabel" />
<spring:message code="settings.reviews.target.item" var="reviewItemLabel" />
<spring:message code="settings.reviews.target.user" var="reviewUserLabel" />
<spring:message code="settings.reviews.view" var="reviewViewLabel" />
<spring:message code="settings.reviews.view.item" var="reviewViewItemLabel" />
<spring:message code="settings.reviews.view.user" var="reviewViewUserLabel" />
<spring:message code="settings.reviews.view.owner" var="reviewViewOwnerLabel" />
<spring:message code="settings.reviews.target.owner" var="reviewOwnerLabel" />
<spring:message code="itemDetail.price.total" var="bookingTotalPriceLabel" />

<c:url var="formAction" value="${formActionPath}" />
<c:url var="clearFiltersUrl" value="${formActionPath}">
  <c:if test="${sort != 'newest'}">
    <c:param name="sortBy" value="${sort}" />
  </c:if>
  <c:if test="${pageSize != 12}">
    <c:param name="pageSize" value="${pageSize}" />
  </c:if>
</c:url>

<c:if test="${bookingPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="${formActionPath}">
    <c:param name="page" value="${bookingPage.previousPage}" />
    <c:param name="sortBy" value="${sort}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty bookingSearch.searchQuery}"><c:param name="searchQuery" value="${bookingSearch.searchQuery}" /></c:if>
    <c:if test="${not empty bookingSearch.date}"><c:param name="date" value="${bookingSearch.date}" /></c:if>
    <c:if test="${not empty bookingSearch.status}"><c:param name="status" value="${bookingSearch.status}" /></c:if>
  </c:url>
  <c:url var="nextPageUrl" value="${formActionPath}">
    <c:param name="page" value="${bookingPage.nextPage}" />
    <c:param name="sortBy" value="${sort}" />
    <c:param name="pageSize" value="${pageSize}" />
    <c:if test="${not empty bookingSearch.searchQuery}"><c:param name="searchQuery" value="${bookingSearch.searchQuery}" /></c:if>
    <c:if test="${not empty bookingSearch.date}"><c:param name="date" value="${bookingSearch.date}" /></c:if>
    <c:if test="${not empty bookingSearch.status}"><c:param name="status" value="${bookingSearch.status}" /></c:if>
  </c:url>
</c:if>

<paw:toastNotifier />
<div class="grid grid-cols-1 gap-8 md:grid-cols-[18rem_minmax(0,1fr)] lg:grid-cols-[20rem_minmax(0,1fr)] items-start w-full">
  <form id="${filterFormId}" action="${formAction}" method="get" class="contents">
  <div class="sticky top-24 w-full min-w-0 space-y-6">
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
                value="${bookingSearch.date}"
                placeholder="${datePlaceholder}"
                restrictToAvailability="false" />
          </div>

          <paw:optionsPicker
              id="requests-${sidebarActive}-status"
              name="status"
              label="${statusFilterLabel}"
              value="${bookingSearch.status}"
              placeholder="${statusAnyPlaceholder}"
              icon="flag"
              variant="inline"
              optionsUrl="/booking-status-options"
              panelCaption="${statusPanelCaption}"
              emptyCaption="${statusNoMatchCaption}"
              containerClass="w-full" />

          <div class="flex flex-col gap-3">
            <paw:button type="submit" color="primary" fullWidth="true" text="${filtersApplyLabel}" cssClass="js-apply-list-filters" />
            <a href="${clearFiltersUrl}" data-clear-list-filters class="btn btn-outline btn-block no-underline">
              <c:out value="${filtersClearLabel}" />
            </a>
          </div>
        </div>
      </jsp:body>
    </paw:sectionCard>
  </div>
  </form>

  <section class="relative z-0 min-w-0 space-y-6">
    <div id="booking-requests-list" class="scroll-mt-24 space-y-8">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-4">
        <div class="min-w-0 w-full flex-1">
          <paw:searchBar
              formId="${filterFormId}"
              name="searchQuery"
              value="${bookingSearch.searchQuery}"
              placeholder="${searchLabel}"
              ariaLabel="${searchLabel}"
              maxlength="100"
              size="lg" />
        </div>
        <div class="flex shrink-0 flex-nowrap items-center gap-2 text-sm font-medium text-on-surface-variant lg:justify-end">
          <label for="requests-${sidebarActive}-sort" class="shrink-0 whitespace-nowrap"><c:out value="${sortLabel}" /></label>
          <select
              id="requests-${sidebarActive}-sort"
              name="sortBy"
              form="${filterFormId}"
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
              form="${filterFormId}"
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
          <spring:message code="requests.nav.sectionTitle" />
        </h1>
        <p class="text-on-surface-variant mt-2 m-0">
          <c:choose>
            <c:when test="${isIncoming}"><spring:message code="requests.incoming.subtitle" /></c:when>
            <c:otherwise><spring:message code="requests.outgoing.subtitle" /></c:otherwise>
          </c:choose>
        </p>
        <h2 class="text-2xl font-extrabold tracking-tight text-on-background m-0 mt-6 break-words md:text-4xl">
          <c:choose>
            <c:when test="${isIncoming}"><spring:message code="requests.incoming.title" /></c:when>
            <c:otherwise><spring:message code="requests.outgoing.title" /></c:otherwise>
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
          <div class="flex flex-col gap-4">
            <c:forEach var="b" items="${bookings}">
              <c:set var="badgeClass" value="badge-ghost" />
              <c:if test="${b.status.name() == 'PENDING'}"><c:set var="badgeClass" value="badge-warning" /></c:if>
              <c:if test="${b.status.name() == 'ACCEPTED' || b.status.name() == 'PAID'}"><c:set var="badgeClass" value="badge-info" /></c:if>
              <c:if test="${b.status.name() == 'CONFIRMED'}"><c:set var="badgeClass" value="badge-success" /></c:if>
              <c:if test="${b.status.name() == 'REJECTED' || b.status.name() == 'CANCELLED' || b.status.name() == 'REFUSED'}"><c:set var="badgeClass" value="badge-error" /></c:if>
              <c:if test="${b.status.name() == 'FINISHED'}"><c:set var="badgeClass" value="badge-ghost" /></c:if>

              <c:if test="${not empty b.start}">
                <fmt:parseDate value="${fn:substring(b.start, 0, 16)}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedStart" />
              </c:if>
              <c:if test="${not empty b.end}">
                <fmt:parseDate value="${fn:substring(b.end, 0, 16)}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedEnd" />
              </c:if>
              <c:if test="${not empty b.updatedAt}">
                <fmt:parseDate value="${fn:substring(b.updatedAt, 0, 16)}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedUpdate" />
              </c:if>
              <fmt:formatDate value="${parsedStart}" pattern="dd MMM yyyy, HH:mm" var="fmtStart" />
              <fmt:formatDate value="${parsedEnd}" pattern="dd MMM yyyy, HH:mm" var="fmtEnd" />
              <fmt:formatDate value="${parsedUpdate}" pattern="dd MMM yyyy, HH:mm" var="fmtUpdate" />

              <c:url var="incomingAcceptUrl" value="/requests/incoming/${b.id}/accept" />
              <c:url var="incomingRejectUrl" value="/requests/incoming/${b.id}/reject" />
              <c:url var="incomingConfirmPaymentUrl" value="/requests/incoming/${b.id}/confirm-payment" />
              <c:url var="incomingRejectPaymentUrl" value="/requests/incoming/${b.id}/reject-payment" />
              <c:url var="outgoingCancelUrl" value="/requests/outgoing/${b.id}/cancel" />
              <c:url var="outgoingPaymentUrl" value="/requests/outgoing/${b.id}/payment" />
              <c:url var="paymentProofUrl" value="/requests/bookings/${b.id}/payment-proof" />
              <c:url var="itemDetailUrl" value="/item/${b.version.item.id}" />
              <c:set var="bookingDurationMs" value="${parsedEnd.time - parsedStart.time}" />
              <c:set var="bookingTotalPrice" value="${bookingDurationMs / 60000.0 / 60.0 * b.version.price}" />

              <c:set var="detailModalId" value="nuevo-booking-detail-${sidebarActive}-${b.id}" />

              <div class="flex flex-col md:flex-row w-full bg-base-100 rounded-xl border border-outline-variant/30 shadow-sm overflow-hidden p-0">
                <div class="relative flex flex-col md:flex-row flex-1 min-w-0 transition hover:bg-base-200">
                  <a href="${itemDetailUrl}" class="absolute inset-0 z-0 rounded-l-xl focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary" aria-label="<c:out value='${b.version.title}'/>"></a>

                  <!-- Image Side -->
                  <div class="pointer-events-none relative z-0 flex w-full md:w-48 shrink-0 items-center justify-center bg-base-300 md:border-r border-outline-variant/20 p-4 h-32 md:h-auto overflow-hidden">
                    <c:url var="bookingCoverUrl" value="/css/boat-placeholder.svg" />
                    <c:if test="${not empty b.version.media}">
                      <c:set var="firstMedia" value="${b.version.media[0]}" />
                      <c:if test="${not empty firstMedia.image}">
                        <c:url var="bookingCoverUrl" value="/image/${firstMedia.image.id}" />
                      </c:if>
                    </c:if>
                    <img src="${bookingCoverUrl}" alt="" class="w-full h-full object-cover" loading="lazy" />
                  </div>

                  <!-- Details Side -->
                  <div class="pointer-events-none relative z-0 flex flex-col flex-1 p-4 gap-2">
                    <div class="flex flex-col sm:flex-row sm:items-start justify-between gap-2">
                      <div class="flex-1">
                        <div class="flex items-center gap-2 mb-1">
                          <span class="badge badge-xs <c:out value='${badgeClass}'/> font-bold py-2"><spring:message code="booking.status.${b.status}" /></span>
                        </div>
                        <h3 class="m-0 text-lg font-bold text-on-surface line-clamp-1"><c:out value="${b.version.title}" /></h3>
                        <p class="m-0 text-xs font-semibold text-on-surface-variant flex items-center gap-1 mt-1">
                          <span class="material-symbols-outlined text-[14px]">calendar_today</span>
                          <c:out value="${fmtStart}" /> &rarr; <c:out value="${fmtEnd}" />
                        </p>
                        <p class="m-0 text-xs font-semibold text-on-surface-variant flex items-center gap-1 mt-1">
                          <span class="material-symbols-outlined text-[14px]">attach_money</span>
                          $<fmt:formatNumber value="${bookingTotalPrice}" type="number" groupingUsed="true" maxFractionDigits="0" />
                          <span class="text-[10px] font-bold uppercase tracking-tighter text-outline"><c:out value="${bookingTotalPriceLabel}" /></span>
                        </p>
                      </div>
                    </div>

                    <div class="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-2">
                      <div class="pointer-events-auto relative z-10">
                        <p class="m-0 text-[10px] font-bold uppercase tracking-wider text-outline">
                          <c:choose>
                            <c:when test="${isIncoming}"><spring:message code="requests.incoming.table.guest" /></c:when>
                            <c:otherwise><c:out value="${hostLabel}" /></c:otherwise>
                          </c:choose>
                        </p>
                        <p class="m-0 text-sm font-semibold text-on-surface">
                          <c:choose>
                            <c:when test="${isIncoming}">
                              <c:url var="bookingPartyProfileUrl" value="/profiles/${b.guest.id}" />
                              <a href="${bookingPartyProfileUrl}" class="text-on-surface no-underline hover:underline"><c:out value="${b.guest.firstName}" /> <c:out value="${b.guest.lastName}" /></a>
                            </c:when>
                            <c:otherwise>
                              <c:url var="bookingPartyProfileUrl" value="/profiles/${b.version.item.host.id}" />
                              <a href="${bookingPartyProfileUrl}" class="text-on-surface no-underline hover:underline"><c:out value="${b.version.item.host.firstName}" /> <c:out value="${b.version.item.host.lastName}" /></a>
                            </c:otherwise>
                          </c:choose>
                        </p>
                      </div>
                      <div>
                        <p class="m-0 text-[10px] font-bold uppercase tracking-wider text-outline"><c:out value="${updatedLabel}" /></p>
                        <p class="m-0 text-xs font-mono text-on-surface-variant mt-0.5"><c:out value="${fmtUpdate}" /></p>
                      </div>
                    </div>

                    <c:if test="${not isIncoming && (b.status.name() == 'ACCEPTED' || b.status.name() == 'REFUSED') && not empty b.version.item.host.alias}">
                      <div class="mt-1 bg-base-200/50 p-2 rounded flex items-center justify-between border border-outline-variant/10">
                        <div class="flex-1">
                           <p class="m-0 text-[10px] font-bold uppercase tracking-wider text-outline">
                              <c:out value="${aliasLabel}" default="Payment Alias" />
                           </p>
                            <p class="m-0 text-sm font-mono text-on-surface-variant select-all"><c:out value="${b.version.item.host.alias}" /></p>
                        </div>
                      </div>
                    </c:if>

                    <c:if test="${not empty b.msg}">
                      <div class="mt-2 text-sm text-on-surface italic border-l-2 border-primary/50 pl-2 line-clamp-2">"<c:out value="${b.msg}" />"</div>
                    </c:if>
                  </div>
                </div>

                <!-- Actions Side -->
                <div class="relative z-10 flex flex-col md:w-60 shrink-0 bg-base-200/50 p-4 border-t md:border-t-0 md:border-l border-outline-variant/20 gap-2 justify-center">
                  <c:if test="${isIncoming && b.status.name() == 'PENDING'}">
                    <form action="${incomingAcceptUrl}" method="post" class="w-full">
                      <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
                      <paw:button type="submit" color="primary" cssClass="w-full" size="sm" text="${actionAcceptLabel}" submitLoading="true" />
                    </form>
                    <form action="${incomingRejectUrl}" method="post" class="w-full">
                      <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
                      <paw:button type="submit" color="danger" variant="outline" cssClass="w-full" size="sm" text="${actionRejectLabel}" submitLoading="true" />
                    </form>
                  </c:if>
                  <c:if test="${isIncoming && b.status.name() == 'PAID'}">
                    <form action="${incomingConfirmPaymentUrl}" method="post" class="w-full">
                      <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
                      <paw:button type="submit" color="primary" cssClass="w-full" size="sm" text="${actionConfirmPaymentLabel}" submitLoading="true" />
                    </form>
                    <button type="button" class="btn btn-outline btn-error btn-sm w-full" onclick="document.getElementById('${detailModalId}-reject').showModal()">
                      <c:out value="${actionRejectPaymentLabel}" />
                    </button>
                    <!-- Reject Payment Modal -->
                    <paw:detailsModal id="${detailModalId}-reject" title="${actionRejectPaymentLabel}">
                      <jsp:body>
                        <form action="${incomingRejectPaymentUrl}" method="post" class="space-y-4">
                          <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
                          <div class="form-control w-full">
                            <label class="label block text-[11px] font-bold uppercase tracking-wider text-outline" for="reject-pay-reason-${b.id}"><c:out value="${rejectPaymentReasonLabel}" /></label>
                            <textarea id="reject-pay-reason-${b.id}" name="reason" rows="3" maxlength="255" required="required" class="textarea textarea-bordered w-full"></textarea>
                          </div>
                          <paw:button type="submit" color="danger" text="${actionRejectPaymentLabel}" submitLoading="true" />
                        </form>
                      </jsp:body>
                    </paw:detailsModal>
                  </c:if>
                  <c:if test="${not isIncoming && (b.status.name() == 'PENDING' || b.status.name() == 'ACCEPTED' || b.status.name() == 'PAID' || b.status.name() == 'CONFIRMED' || b.status.name() == 'REFUSED')}">
                    <form action="${outgoingCancelUrl}" method="post" class="w-full">
                      <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
                      <paw:button type="submit" color="danger" variant="outline" cssClass="w-full" size="sm" text="${actionCancelLabel}" submitLoading="true" />
                    </form>
                  </c:if>
                  <c:if test="${not isIncoming && (b.status.name() == 'ACCEPTED' || b.status.name() == 'REFUSED')}">
                    <button type="button" class="btn btn-primary btn-sm w-full" onclick="document.getElementById('${detailModalId}-pay').showModal()">
                      <c:out value="${actionSubmitPaymentLabel}" />
                    </button>
                    <!-- Submit Payment Modal -->
                    <paw:detailsModal id="${detailModalId}-pay" title="${actionSubmitPaymentLabel}">
                      <jsp:body>
                        <form action="${outgoingPaymentUrl}" method="post" enctype="multipart/form-data" class="space-y-4" data-submit-loading-form="true">
                          <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
                          <div class="form-control w-full space-y-1">
                            <p class="block text-[11px] font-bold uppercase tracking-wider text-outline mb-1"><spring:message code="requests.payment.uploadLabel" /></p>
                            <input type="file" name="file" accept="application/pdf,image/png,image/jpeg,image/webp" required="required" class="file-input file-input-bordered file-input-sm w-full" />
                          </div>
                          <div class="form-control w-full">
                            <label class="label block text-[11px] font-bold uppercase tracking-wider text-outline mb-1" for="guest-reply-${b.id}"><c:out value="${guestReplyLabel}" /></label>
                            <textarea id="guest-reply-${b.id}" name="guestReply" rows="3" maxlength="255" class="textarea textarea-bordered w-full" placeholder="<c:out value='${guestReplyPlaceholder}'/>"></textarea>
                          </div>
                          <paw:button type="submit" color="primary" text="${actionSubmitPaymentLabel}" submitLoading="true" />
                        </form>
                      </jsp:body>
                    </paw:detailsModal>
                  </c:if>
                  <c:if test="${b.status.name() == 'PAID' || b.status.name() == 'CONFIRMED' || b.status.name() == 'REFUSED' || b.status.name() == 'FINISHED'}">
                      <button type="button" class="btn btn-outline btn-sm w-full mt-auto" onclick="document.getElementById('${detailModalId}-proof').showModal()">
                        <span class="material-symbols-outlined text-sm">visibility</span> <c:out value="${viewPaymentProofLabel}" />
                      </button>
                      <!-- View Proof Modal -->
                      <paw:detailsModal id="${detailModalId}-proof" title="${viewPaymentProofLabel}">
                        <jsp:body>
                            <c:if test="${not empty b.paymentProof.replyMsg}">
                              <div class="rounded-lg bg-base-100 p-3 border-l-4 border-primary space-y-1 mb-4">
                                <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="payment.guestReply.display.label" /></p>
                                <p class="m-0 break-words text-sm text-on-surface whitespace-pre-line"><c:out value="${b.paymentProof.replyMsg}" /></p>
                              </div>
                            </c:if>
                            <c:if test="${not empty b.paymentProof.refuseMsg}">
                              <div class="rounded-lg bg-error/10 p-3 border-l-4 border-error space-y-1 mb-4">
                                <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-error">
                                  <c:choose>
                                    <c:when test="${isIncoming}"><spring:message code="requests.payment.refusalYouSent" /></c:when>
                                    <c:otherwise><spring:message code="payment.refusal.reason.label" /></c:otherwise>
                                  </c:choose>
                                </p>
                                <p class="m-0 break-words text-sm text-on-surface whitespace-pre-line"><c:out value="${b.paymentProof.refuseMsg}" /></p>
                              </div>
                            </c:if>
                          <c:choose>
                            <c:when test="${fn:startsWith(b.paymentProof.contentType, 'image/')}">
                              <div class="overflow-hidden rounded-lg border border-outline-variant/20 bg-base-200/40">
                                <img src="${paymentProofUrl}" alt="<spring:message code='requests.paymentProof.view' />" class="max-h-[60vh] w-full object-contain" loading="lazy" />
                              </div>
                            </c:when>
                            <c:otherwise>
                              <div class="overflow-hidden rounded-lg border border-outline-variant/20 bg-base-200/40">
                                <iframe src="${paymentProofUrl}#toolbar=0" class="h-[60vh] w-full border-0"></iframe>
                              </div>
                            </c:otherwise>
                          </c:choose>
                        </jsp:body>
                      </paw:detailsModal>
                  </c:if>
                  <c:if test="${b.status.name() == 'FINISHED'}">
                    <c:set var="reviewsForBooking" value="${userReviews[b.id]}" />
                    <c:set var="hasItemReview" value="${false}" />
                    <c:set var="hasUserReview" value="${false}" />
                    <c:set var="itemReview" value="null" />
                    <c:set var="userReview" value="null" />
                    <c:if test="${reviewsForBooking != null}">
                      <c:forEach var="rev" items="${reviewsForBooking}">
                        <c:if test="${rev.targetType.name() == 'ITEM'}">
                          <c:set var="hasItemReview" value="${true}" />
                          <c:set var="itemReview" value="${rev}" />
                        </c:if>
                        <c:if test="${rev.targetType.name() == 'USER'}">
                          <c:set var="hasUserReview" value="${true}" />
                          <c:set var="userReview" value="${rev}" />
                        </c:if>
                      </c:forEach>
                    </c:if>
                    <c:choose>
                      <c:when test="${isIncoming}">
                        <c:choose>
                          <c:when test="${hasUserReview}">
                            <button type="button" class="btn btn-outline btn-sm w-full" onclick="document.getElementById('${detailModalId}-review').showModal()">
                              <span class="material-symbols-outlined text-sm">visibility</span> <c:out value="${reviewViewUserLabel}" />
                            </button>
                            <paw:reviewModal bookingId="${b.id}" modalId="${detailModalId}-review" listMode="${listMode}" existingReview="${userReview}" />
                          </c:when>
                          <c:otherwise>
                            <button type="button" class="btn btn-primary btn-sm w-full" onclick="document.getElementById('${detailModalId}-review').showModal()">
                              <c:out value="${reviewUserLabel}" />
                            </button>
                            <paw:reviewModal bookingId="${b.id}" modalId="${detailModalId}-review" listMode="${listMode}" />
                          </c:otherwise>
                        </c:choose>
                      </c:when>
                      <c:otherwise>
                        <div class="flex flex-col gap-2">
                          <c:choose>
                            <c:when test="${hasItemReview}">
                              <button type="button" class="btn btn-outline btn-sm w-full" onclick="document.getElementById('${detailModalId}-review-item').showModal()">
                                <span class="material-symbols-outlined text-sm">visibility</span> <c:out value="${reviewViewItemLabel}" />
                              </button>
                              <paw:reviewModal bookingId="${b.id}" modalId="${detailModalId}-review-item" listMode="${listMode}" existingReview="${itemReview}" />
                            </c:when>
                            <c:otherwise>
                              <button type="button" class="btn btn-primary btn-sm w-full" onclick="document.getElementById('${detailModalId}-review-item').showModal()">
                                <c:out value="${reviewItemLabel}" />
                              </button>
                              <paw:reviewModal bookingId="${b.id}" modalId="${detailModalId}-review-item" listMode="${listMode}" targetType="ITEM" />
                            </c:otherwise>
                          </c:choose>
                          <c:choose>
                            <c:when test="${hasUserReview}">
                              <button type="button" class="btn btn-outline btn-sm w-full" onclick="document.getElementById('${detailModalId}-review-user').showModal()">
                                <span class="material-symbols-outlined text-sm">visibility</span> <c:out value="${reviewViewOwnerLabel}" />
                              </button>
                              <paw:reviewModal bookingId="${b.id}" modalId="${detailModalId}-review-user" listMode="${listMode}" existingReview="${userReview}" />
                            </c:when>
                            <c:otherwise>
                              <button type="button" class="btn btn-primary btn-sm w-full" onclick="document.getElementById('${detailModalId}-review-user').showModal()">
                                <c:out value="${reviewOwnerLabel}" />
                              </button>
                              <paw:reviewModal bookingId="${b.id}" modalId="${detailModalId}-review-user" listMode="${listMode}" targetType="USER" />
                            </c:otherwise>
                          </c:choose>
                        </div>
                      </c:otherwise>
                    </c:choose>
                  </c:if>
                </div>
              </div>
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
                  <c:otherwise><spring:message code="requests.outgoing.empty" /></c:otherwise>
                </c:choose>
              </p>
              <a href="${clearFiltersUrl}" data-clear-list-filters class="btn btn-primary btn-sm no-underline">
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
</div>
