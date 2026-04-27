<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
<c:url var="publishUrl" value="/publish" />
<spring:message code="nav.publishCta" var="publishCtaLabel" />
<spring:message code="profile.publications.edit" var="editLabel" />
<spring:message code="profile.publications.manageAvailability" var="manageAvailabilityLabel" />
<spring:message code="profile.publications.enable" var="enableLabel" />
<spring:message code="profile.publications.disable" var="disableLabel" />
<spring:message code="profile.publications.delete" var="deleteLabel" />
<spring:message code="profile.publications.actions" var="actionsLabel" />
<spring:message code="profile.publications.delete.confirm.title" var="deleteConfirmTitle" />
<spring:message code="profile.publications.delete.confirm.message" var="deleteConfirmMessage" />
<spring:message code="profile.publications.delete.confirm.confirm" var="deleteConfirmConfirm" />
<spring:message code="profile.publications.delete.confirm.cancel" var="deleteConfirmCancel" />
<spring:message code="profile.bookings.accept" var="acceptLabel" />
<spring:message code="profile.bookings.decline" var="declineLabel" />
<spring:message code="profile.paymentProofs.confirmReceived" var="paymentReceivedLabel" />
<spring:message code="profile.sentBookings.paymentProof.upload" var="uploadPaymentProofLabel" />
<spring:message code="profile.bookings.paymentInfo.price" var="paymentInfoPriceLabel" />
<spring:message code="profile.bookings.paymentInfo.alias" var="paymentInfoAliasLabel" />
<spring:message code="dashboard.tabs.hosting" var="hostingTabLabel" />
<spring:message code="dashboard.tabs.bookings" var="bookingsTabLabel" />
<spring:message code="dashboard.tabs.reviews" var="reviewsTabLabel" />
<spring:message code="profile.reviews.receivedAsGuest.title" var="receivedGuestReviewsTitle" />
<spring:message code="profile.reviews.receivedAsGuest.empty" var="receivedGuestReviewsEmpty" />
<spring:message code="profile.reviews.receivedOnItems.title" var="receivedOnItemsReviewsTitle" />
<spring:message code="profile.reviews.receivedOnItems.empty" var="receivedOnItemsReviewsEmpty" />
<spring:message code="profile.reviews.rating.label" var="reviewRatingLabel" />
<spring:message code="profile.reviews.comment.label" var="reviewCommentLabel" />
<spring:message code="profile.reviews.submit" var="reviewSubmitLabel" />
<spring:message code="profile.reviews.target.item" var="reviewTargetItemLabel" />
<spring:message code="profile.reviews.target.user" var="reviewTargetUserLabel" />
<spring:message code="profile.reviews.authoredSummary.label" var="authoredReviewSummaryLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 max-w-7xl mx-auto px-6">
  <div class="grid grid-cols-1 lg:grid-cols-[18rem_minmax(0,1fr)] gap-8 items-start">
    <paw:accountSidebar active="dashboard" />

    <section class="min-w-0 space-y-6">
      <div class="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div class="min-w-0">
          <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="dashboard.title" /></h1>
          <p class="text-on-surface-variant mt-2 m-0"><spring:message code="dashboard.subtitle" /></p>
        </div>
        <a href="${publishUrl}" class="btn btn-secondary no-underline sm:shrink-0">
          <span class="material-symbols-outlined text-base">add</span>
          <c:out value="${publishCtaLabel}" />
        </a>
      </div>

      <div role="tablist" class="tabs tabs-lifted">
        <input type="radio" name="dashboard_tabs" role="tab" class="tab font-bold" aria-label="${hostingTabLabel}" ${activeDashboardTab == 'hosting' ? 'checked="checked"' : ''} />
        <div role="tabpanel" class="tab-content bg-base-100 border-base-300 rounded-box p-6">
          <div id="my-publications" class="scroll-mt-24 space-y-4">
            <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.publications.title" /></h2>
            <c:if test="${param.publishAction == 'deleted'}"><paw:alertMessage type="success"><spring:message code="profile.publications.deleted" /></paw:alertMessage></c:if>
            <c:if test="${param.publishAction == 'updated'}"><paw:alertMessage type="success"><spring:message code="profile.publications.updated" /></paw:alertMessage></c:if>
            <c:if test="${param.publishAction == 'disabled'}"><paw:alertMessage type="success"><spring:message code="profile.publications.disabled" /></paw:alertMessage></c:if>
            <c:if test="${param.publishAction == 'enabled'}"><paw:alertMessage type="success"><spring:message code="profile.publications.enabled" /></paw:alertMessage></c:if>
            <c:if test="${param.publishAction == 'alreadyDeleted'}"><paw:alertMessage type="warning"><spring:message code="profile.publications.alreadyDeleted" /></paw:alertMessage></c:if>
            <c:if test="${param.publishAction == 'forbidden' || param.publishAction == 'error'}"><paw:alertMessage type="error"><spring:message code="profile.publications.error" /></paw:alertMessage></c:if>
            <c:if test="${param.publishAction == 'deleteBlockedByBookings'}"><paw:alertMessage type="error"><spring:message code="profile.publications.deleteBlockedByBookings" /></paw:alertMessage></c:if>

            <c:choose>
              <c:when test="${not empty ownedItems}">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <c:forEach var="item" items="${ownedItems}">
                    <c:url var="editItemUrl" value="/profile/item/${item.id}/edit" />
                    <c:url var="manageAvailabilityItemUrl" value="/profile/item/${item.id}/availability" />
                    <c:url var="disableItemUrl" value="/profile/item/${item.id}/disable" />
                    <c:url var="enableItemUrl" value="/profile/item/${item.id}/enable" />
                    <c:url var="deleteItemUrl" value="/profile/item/${item.id}/delete" />
                    <c:set var="deleteModalId" value="delete-publication-modal-${item.id}" />
                    <c:set var="kebabId" value="publication-kebab-${item.id}" />
                    <div class="rounded-xl bg-base-200 p-4 flex min-w-0 items-start justify-between gap-4">
                      <div class="min-w-0 ${item.active ? '' : 'opacity-75'}">
                        <div class="flex min-w-0 flex-wrap items-center gap-2">
                          <p class="m-0 min-w-0 break-words text-sm font-bold text-on-surface"><c:out value="${item.title}" /></p>
                          <span class="badge ${item.active ? 'badge-success' : 'badge-ghost'} badge-sm shrink-0 font-bold">
                            <spring:message code="${item.active ? 'profile.publications.status.active' : 'profile.publications.status.inactive'}" />
                          </span>
                        </div>
                        <p class="m-0 text-xs text-on-surface-variant">
                          $<fmt:formatNumber value="${item.pricePerHour}" type="number" groupingUsed="true" maxFractionDigits="0" />
                          <spring:message code="marketplace.card.perHour" />
                        </p>
                      </div>
                      <paw:kebabMenu id="${kebabId}" ariaLabel="${actionsLabel}">
                        <li><a href="${editItemUrl}" class="flex items-center gap-2"><span class="material-symbols-outlined text-base leading-none">edit</span><span><c:out value="${editLabel}" /></span></a></li>
                        <li><a href="${manageAvailabilityItemUrl}" class="flex items-center gap-2"><span class="material-symbols-outlined text-base leading-none">event_available</span><span><c:out value="${manageAvailabilityLabel}" /></span></a></li>
                        <li>
                          <c:choose>
                            <c:when test="${item.active}">
                              <form action="${disableItemUrl}" method="post" class="m-0 w-full p-0"><button type="submit" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left"><span class="material-symbols-outlined text-base leading-none">visibility_off</span><span><c:out value="${disableLabel}" /></span></button></form>
                            </c:when>
                            <c:otherwise>
                              <form action="${enableItemUrl}" method="post" class="m-0 w-full p-0"><button type="submit" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left"><span class="material-symbols-outlined text-base leading-none">visibility</span><span><c:out value="${enableLabel}" /></span></button></form>
                            </c:otherwise>
                          </c:choose>
                        </li>
                        <li><button type="button" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-error" onclick="document.getElementById('${deleteModalId}').showModal()"><span class="material-symbols-outlined text-base leading-none">delete</span><span><c:out value="${deleteLabel}" /></span></button></li>
                      </paw:kebabMenu>
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
                <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.publications.empty" /></p>
              </c:otherwise>
            </c:choose>
          </div>

          <div id="received-booking-requests" class="scroll-mt-24 mt-8 space-y-4">
            <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.bookings.title" /></h2>
            <c:if test="${param.reviewAction == 'created'}"><paw:alertMessage type="success"><spring:message code="profile.reviews.created" /></paw:alertMessage></c:if>
            <c:if test="${param.reviewAction == 'validationError'}"><paw:alertMessage type="error"><spring:message code="profile.reviews.validationError" /></paw:alertMessage></c:if>
            <c:if test="${param.reviewAction == 'error'}"><paw:alertMessage type="error"><spring:message code="profile.reviews.error" /></paw:alertMessage></c:if>
            <c:if test="${param.bookingAction == 'accepted'}"><paw:alertMessage type="success"><spring:message code="profile.bookings.accepted" /></paw:alertMessage></c:if>
            <c:if test="${param.bookingAction == 'rejected'}"><paw:alertMessage type="warning"><spring:message code="profile.bookings.rejected" /></paw:alertMessage></c:if>
            <c:if test="${param.bookingAction == 'forbidden' || param.bookingAction == 'error' || param.bookingAction == 'notFound'}"><paw:alertMessage type="error"><spring:message code="profile.bookings.error" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'paid'}"><paw:alertMessage type="success"><spring:message code="profile.payment.paid" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'confirmError'}"><paw:alertMessage type="error"><spring:message code="profile.payment.error" /></paw:alertMessage></c:if>
            <c:choose>
              <c:when test="${not empty receivedBookingRequests}">
                <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
                  <c:forEach var="receivedRequest" items="${receivedBookingRequests}">
                    <c:url var="acceptBookingUrl" value="/bookings/${receivedRequest.id}/accept" />
                    <c:url var="declineBookingUrl" value="/bookings/${receivedRequest.id}/decline" />
                    <c:url var="receivedPaymentProofUrl" value="/bookings/${receivedRequest.id}/payment-proof" />
                    <c:url var="confirmPaymentUrl" value="/bookings/${receivedRequest.id}/payment/confirm" />
                    <c:set var="receivedStatusClass" value="badge-ghost" />
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.pending'}"><c:set var="receivedStatusClass" value="badge-warning" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}"><c:set var="receivedStatusClass" value="badge-success" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.rejected' || receivedRequest.statusMessageCode == 'profile.sentBookings.status.cancelled'}"><c:set var="receivedStatusClass" value="badge-error" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted'}"><c:set var="receivedStatusClass" value="badge-info" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paid'}"><c:set var="receivedStatusClass" value="badge-success" /></c:if>
                    <div class="rounded-xl bg-base-200 p-4 space-y-4">
                      <div class="flex items-start justify-between gap-3">
                        <p class="m-0 min-w-0 break-words text-sm font-bold text-on-surface"><c:out value="${receivedRequest.itemTitle}" /></p>
                        <span class="badge ${receivedStatusClass} badge-sm shrink-0 font-bold"><spring:message code="${receivedRequest.statusMessageCode}" /></span>
                      </div>
                      <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedRequest.dateLabel}" /> · <c:out value="${receivedRequest.timeRangeLabel}" /></p>
                      <div class="rounded-lg bg-base-100 p-3">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.bookings.requester.label" /></p>
                        <p class="m-0 mt-1 break-words text-sm font-bold text-on-surface"><c:out value="${receivedRequest.requesterName}" /></p>
                        <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${receivedRequest.requesterEmail}" /></p>
                      </div>
                      <div class="rounded-lg bg-base-100 p-3 space-y-2">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><c:out value="${paymentInfoPriceLabel}" /></p>
                        <p class="m-0 text-sm font-bold">$ <c:out value="${not empty receivedRequest.totalPriceLabel ? receivedRequest.totalPriceLabel : '-'}" /></p>
                        <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${paymentInfoAliasLabel}" />: <c:out value="${not empty receivedRequest.paymentAlias ? receivedRequest.paymentAlias : '-'}" /></p>
                      </div>
                      <c:if test="${receivedRequest.hasPaymentProof}">
                        <a href="${receivedPaymentProofUrl}" class="link link-hover block max-w-full break-all text-sm font-bold text-primary"><c:out value="${receivedRequest.paymentProofFileName}" /></a>
                      </c:if>
                      <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.pending'}">
                        <div class="flex flex-wrap gap-2 border-t border-outline-variant/20 pt-3">
                          <form action="${acceptBookingUrl}" method="post" class="m-0"><paw:button type="submit" color="success" size="sm" text="${acceptLabel}" /></form>
                          <form action="${declineBookingUrl}" method="post" class="m-0"><paw:button type="submit" color="danger" variant="outline" size="sm" text="${declineLabel}" /></form>
                        </div>
                      </c:if>
                      <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted' && receivedRequest.hasPaymentProof}">
                        <form action="${confirmPaymentUrl}" method="post" class="m-0 border-t border-outline-variant/20 pt-3"><paw:button type="submit" color="success" size="sm" text="${paymentReceivedLabel}" /></form>
                      </c:if>
                      <c:set var="ownerPendingReview" value="${pendingOwnerUserReviewsByBookingId[receivedRequest.id]}" />
                      <c:if test="${not empty ownerPendingReview}">
                        <form action="/reviews/booking/${receivedRequest.id}" method="post" class="space-y-3 border-t border-outline-variant/20 pt-3">
                          <input type="hidden" name="returnTo" value="dashboardHosting" />
                          <div class="flex items-start justify-between gap-2">
                            <p class="m-0 text-xs text-on-surface-variant break-words"><c:out value="${ownerPendingReview.targetName}" /> · <c:out value="${ownerPendingReview.targetEmail}" /></p>
                            <span class="badge badge-primary badge-sm shrink-0 font-bold"><c:out value="${reviewTargetUserLabel}" /></span>
                          </div>
                          <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
                            <label class="text-xs font-bold uppercase tracking-wider text-outline mt-2" for="review-owner-rating-${receivedRequest.id}"><c:out value="${reviewRatingLabel}" /></label>
                            <div class="flex items-center gap-1" data-rating-stars>
                              <input id="review-owner-rating-${receivedRequest.id}" type="hidden" name="rating" value="" data-rating-value />
                              <c:forEach var="starIndex" begin="1" end="5">
                                <button type="button" class="btn btn-ghost btn-sm btn-square min-h-9 h-9 w-9 p-0" data-rating-star="${starIndex}" aria-label="${reviewRatingLabel} ${starIndex}">
                                  <span class="material-symbols-outlined text-xl leading-none text-outline" style="opacity: 0.35;">star</span>
                                </button>
                              </c:forEach>
                            </div>
                          </div>
                          <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
                            <label class="text-xs font-bold uppercase tracking-wider text-outline mt-2" for="review-owner-comment-${receivedRequest.id}"><c:out value="${reviewCommentLabel}" /></label>
                            <textarea id="review-owner-comment-${receivedRequest.id}" name="comment" rows="3" maxlength="1000" class="textarea textarea-bordered w-full"></textarea>
                          </div>
                          <paw:button type="submit" color="primary" size="sm" text="${reviewSubmitLabel}" />
                        </form>
                      </c:if>
                    </div>
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise><p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.bookings.empty" /></p></c:otherwise>
            </c:choose>
          </div>
        </div>

        <input type="radio" name="dashboard_tabs" role="tab" class="tab font-bold" aria-label="${bookingsTabLabel}" ${activeDashboardTab == 'bookings' ? 'checked="checked"' : ''} />
        <div role="tabpanel" class="tab-content bg-base-100 border-base-300 rounded-box p-6">
          <div id="sent-booking-requests" class="scroll-mt-24 space-y-4">
            <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.sentBookings.title" /></h2>
            <c:if test="${param.reviewAction == 'created'}"><paw:alertMessage type="success"><spring:message code="profile.reviews.created" /></paw:alertMessage></c:if>
            <c:if test="${param.reviewAction == 'validationError'}"><paw:alertMessage type="error"><spring:message code="profile.reviews.validationError" /></paw:alertMessage></c:if>
            <c:if test="${param.reviewAction == 'error'}"><paw:alertMessage type="error"><spring:message code="profile.reviews.error" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'submitted'}"><paw:alertMessage type="success"><spring:message code="profile.payment.submitted" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'invalidFile'}"><paw:alertMessage type="error"><spring:message code="profile.payment.invalidFile" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'forbidden' || param.paymentAction == 'submitError' || param.paymentAction == 'error'}"><paw:alertMessage type="error"><spring:message code="profile.payment.error" /></paw:alertMessage></c:if>
            <c:choose>
              <c:when test="${not empty sentBookingRequests}">
                <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
                  <c:forEach var="sentRequest" items="${sentBookingRequests}">
                    <c:url var="sentPaymentProofUrl" value="/bookings/${sentRequest.id}/payment-proof" />
                    <c:set var="sentStatusClass" value="badge-ghost" />
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.pending'}"><c:set var="sentStatusClass" value="badge-warning" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}"><c:set var="sentStatusClass" value="badge-success" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.rejected' || sentRequest.statusMessageCode == 'profile.sentBookings.status.cancelled'}"><c:set var="sentStatusClass" value="badge-error" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted'}"><c:set var="sentStatusClass" value="badge-info" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paid'}"><c:set var="sentStatusClass" value="badge-success" /></c:if>
                    <div class="rounded-xl bg-base-200 p-4 space-y-4">
                      <div class="flex items-start justify-between gap-3">
                        <p class="m-0 min-w-0 break-words text-sm font-bold text-on-surface"><c:out value="${sentRequest.itemTitle}" /></p>
                        <span class="badge ${sentStatusClass} badge-sm shrink-0 font-bold"><spring:message code="${sentRequest.statusMessageCode}" /></span>
                      </div>
                      <p class="m-0 text-xs text-on-surface-variant"><c:out value="${sentRequest.dateLabel}" /> · <c:out value="${sentRequest.timeRangeLabel}" /></p>
                      <div class="rounded-lg bg-base-100 p-3">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.sentBookings.owner.label" /></p>
                        <p class="m-0 mt-1 break-words text-sm font-bold text-on-surface"><c:out value="${sentRequest.ownerName}" /></p>
                        <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${sentRequest.ownerEmail}" /></p>
                      </div>
                      <div class="rounded-lg bg-base-100 p-3 space-y-2">
                        <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><c:out value="${paymentInfoPriceLabel}" /></p>
                        <p class="m-0 text-sm font-bold">$ <c:out value="${not empty sentRequest.totalPriceLabel ? sentRequest.totalPriceLabel : '-'}" /></p>
                        <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${paymentInfoAliasLabel}" />: <c:out value="${not empty sentRequest.paymentAlias ? sentRequest.paymentAlias : '-'}" /></p>
                      </div>
                      <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}">
                        <form action="${sentPaymentProofUrl}" method="post" enctype="multipart/form-data" class="space-y-2 border-t border-outline-variant/20 pt-3">
                          <input type="file" name="file" accept="application/pdf,image/png,image/jpeg,image/webp" class="file-input file-input-bordered file-input-sm w-full" required />
                          <paw:button type="submit" color="primary" size="sm" text="${uploadPaymentProofLabel}" />
                        </form>
                      </c:if>
                      <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted' || sentRequest.statusMessageCode == 'profile.sentBookings.status.paid'}">
                        <a href="${sentPaymentProofUrl}" class="link link-hover block border-t border-outline-variant/20 pt-3 text-sm font-bold text-primary"><spring:message code="profile.sentBookings.paymentProof.view" /></a>
                      </c:if>
                      <c:set var="guestPendingReview" value="${pendingGuestItemReviewsByBookingId[sentRequest.id]}" />
                      <c:if test="${not empty guestPendingReview}">
                        <form action="/reviews/booking/${sentRequest.id}" method="post" class="space-y-3 border-t border-outline-variant/20 pt-3">
                          <input type="hidden" name="returnTo" value="dashboardBookings" />
                          <div class="flex items-start justify-between gap-2">
                            <p class="m-0 text-xs text-on-surface-variant break-words"><c:out value="${guestPendingReview.targetName}" /> · <c:out value="${guestPendingReview.targetEmail}" /></p>
                            <span class="badge badge-primary badge-sm shrink-0 font-bold"><c:out value="${reviewTargetItemLabel}" /></span>
                          </div>
                          <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
                            <label class="text-xs font-bold uppercase tracking-wider text-outline mt-2" for="review-guest-rating-${sentRequest.id}"><c:out value="${reviewRatingLabel}" /></label>
                            <div class="flex items-center gap-1" data-rating-stars>
                              <input id="review-guest-rating-${sentRequest.id}" type="hidden" name="rating" value="" data-rating-value />
                              <c:forEach var="starIndex" begin="1" end="5">
                                <button type="button" class="btn btn-ghost btn-sm btn-square min-h-9 h-9 w-9 p-0" data-rating-star="${starIndex}" aria-label="${reviewRatingLabel} ${starIndex}">
                                  <span class="material-symbols-outlined text-xl leading-none text-outline" style="opacity: 0.35;">star</span>
                                </button>
                              </c:forEach>
                            </div>
                          </div>
                          <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
                            <label class="text-xs font-bold uppercase tracking-wider text-outline mt-2" for="review-guest-comment-${sentRequest.id}"><c:out value="${reviewCommentLabel}" /></label>
                            <textarea id="review-guest-comment-${sentRequest.id}" name="comment" rows="3" maxlength="1000" class="textarea textarea-bordered w-full"></textarea>
                          </div>
                          <paw:button type="submit" color="primary" size="sm" text="${reviewSubmitLabel}" />
                        </form>
                      </c:if>
                      <c:set var="authoredItemReview" value="${authoredItemReviewsByBookingId[sentRequest.id]}" />
                      <c:if test="${not empty authoredItemReview}">
                        <div class="rounded-lg bg-base-100 p-3 space-y-2 border-t border-outline-variant/20">
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><c:out value="${authoredReviewSummaryLabel}" /></p>
                          <div class="flex items-center gap-2">
                            <div class="flex items-center gap-0.5" aria-label="${authoredItemReview.rating} of 5">
                              <c:forEach var="starIndex" begin="1" end="5">
                                <span class="material-symbols-outlined text-sm leading-none ${starIndex <= authoredItemReview.rating ? 'text-warning' : 'text-outline'}" style="opacity: ${starIndex <= authoredItemReview.rating ? '1' : '0.35'};">star</span>
                              </c:forEach>
                            </div>
                          </div>
                          <c:if test="${not empty authoredItemReview.comment}">
                            <p class="m-0 text-xs text-on-surface-variant break-words">
                              <c:choose>
                                <c:when test="${fn:length(authoredItemReview.comment) > 80}"><c:out value="${fn:substring(authoredItemReview.comment, 0, 80)}" />...</c:when>
                                <c:otherwise><c:out value="${authoredItemReview.comment}" /></c:otherwise>
                              </c:choose>
                            </p>
                          </c:if>
                        </div>
                      </c:if>
                    </div>
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise><p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.sentBookings.empty" /></p></c:otherwise>
            </c:choose>
          </div>
        </div>

        <input type="radio" name="dashboard_tabs" role="tab" class="tab font-bold" aria-label="${reviewsTabLabel}" ${activeDashboardTab == 'reviews' ? 'checked="checked"' : ''} />
        <div role="tabpanel" class="tab-content bg-base-100 border-base-300 rounded-box p-6">
          <div id="reviews" class="scroll-mt-24 space-y-6">
            <h2 class="text-xl font-extrabold tracking-tight m-0"><c:out value="${reviewsTabLabel}" /></h2>

            <div class="space-y-4">
              <h3 class="text-lg font-extrabold tracking-tight m-0"><c:out value="${receivedGuestReviewsTitle}" /></h3>
              <c:choose>
                <c:when test="${not empty receivedGuestReviews}">
                  <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
                    <c:forEach var="receivedReview" items="${receivedGuestReviews}">
                      <div class="rounded-xl bg-base-200 p-4 space-y-3">
                        <div class="flex items-start justify-between gap-3">
                          <div>
                            <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${receivedReview.contextTitle}" /></p>
                            <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedReview.reviewerName}" /> · <c:out value="${receivedReview.reviewerEmail}" /></p>
                          </div>
                          <div class="flex items-center gap-0.5 shrink-0" aria-label="${receivedReview.rating} of 5">
                            <c:forEach var="starIndex" begin="1" end="5">
                              <span class="material-symbols-outlined text-sm leading-none ${starIndex <= receivedReview.rating ? 'text-warning' : 'text-outline'}" style="opacity: ${starIndex <= receivedReview.rating ? '1' : '0.35'};">star</span>
                            </c:forEach>
                          </div>
                        </div>
                        <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedReview.createdAtLabel}" /></p>
                        <c:if test="${not empty receivedReview.comment}">
                          <p class="m-0 text-sm text-on-surface-variant break-words"><c:out value="${receivedReview.comment}" /></p>
                        </c:if>
                      </div>
                    </c:forEach>
                  </div>
                </c:when>
                <c:otherwise><p class="m-0 text-sm text-on-surface-variant"><c:out value="${receivedGuestReviewsEmpty}" /></p></c:otherwise>
              </c:choose>
            </div>

            <div class="space-y-4">
              <h3 class="text-lg font-extrabold tracking-tight m-0"><c:out value="${receivedOnItemsReviewsTitle}" /></h3>
              <c:choose>
                <c:when test="${not empty receivedItemReviewsByOwnedItems}">
                  <div class="space-y-4">
                    <c:forEach var="itemReviewsGroup" items="${receivedItemReviewsByOwnedItems}">
                      <div class="rounded-xl bg-base-200 p-4 space-y-3">
                        <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${itemReviewsGroup.itemTitle}" /></p>
                        <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
                          <c:forEach var="receivedReview" items="${itemReviewsGroup.reviews}">
                            <div class="rounded-lg bg-base-100 p-4 space-y-3">
                              <div class="flex items-start justify-between gap-3">
                                <div>
                                  <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedReview.reviewerName}" /> · <c:out value="${receivedReview.reviewerEmail}" /></p>
                                </div>
                                <div class="flex items-center gap-0.5 shrink-0" aria-label="${receivedReview.rating} of 5">
                                  <c:forEach var="starIndex" begin="1" end="5">
                                    <span class="material-symbols-outlined text-sm leading-none ${starIndex <= receivedReview.rating ? 'text-warning' : 'text-outline'}" style="opacity: ${starIndex <= receivedReview.rating ? '1' : '0.35'};">star</span>
                                  </c:forEach>
                                </div>
                              </div>
                              <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedReview.createdAtLabel}" /></p>
                              <c:if test="${not empty receivedReview.comment}">
                                <p class="m-0 text-sm text-on-surface-variant break-words"><c:out value="${receivedReview.comment}" /></p>
                              </c:if>
                            </div>
                          </c:forEach>
                        </div>
                      </div>
                    </c:forEach>
                  </div>
                </c:when>
                <c:otherwise><p class="m-0 text-sm text-on-surface-variant"><c:out value="${receivedOnItemsReviewsEmpty}" /></p></c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</paw:layout>
