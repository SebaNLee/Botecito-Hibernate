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
<spring:message code="profile.publications.delete.confirm.deactivateMessage" var="deleteDeactivateConfirmMessage" />
<spring:message code="profile.publications.delete.confirm.confirm" var="deleteConfirmConfirm" />
<spring:message code="profile.publications.delete.confirm.cancel" var="deleteConfirmCancel" />
<spring:message code="profile.publications.delete.disabled.futureBookings" var="deleteDisabledFutureBookingsLabel" />
<spring:message code="profile.bookings.accept" var="acceptLabel" />
<spring:message code="profile.bookings.decline" var="declineLabel" />
<spring:message code="profile.paymentProofs.confirmReceived" var="paymentReceivedLabel" />
<spring:message code="profile.sentBookings.paymentProof.upload" var="uploadPaymentProofLabel" />
<spring:message code="payment.refuse.submit" var="refuseSubmitLabel" />
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

<paw:layout title="Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6">
  <section class="min-w-0 space-y-6">
      <div class="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div class="min-w-0">
          <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="myBoats.title" /></h1>
          <p class="text-on-surface-variant mt-2 m-0"><spring:message code="myBoats.subtitle" /></p>
        </div>
        <a href="${publishUrl}" class="btn btn-secondary no-underline sm:shrink-0">
          <span class="material-symbols-outlined text-base">add</span>
          <c:out value="${publishCtaLabel}" />
        </a>
      </div>

      <div class="space-y-8">
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
                    <c:set var="deleteModalMessage" value="${publicationDeleteDeactivatesByItemId[item.id] ? deleteDeactivateConfirmMessage : deleteConfirmMessage}" />
                    <c:set var="deleteDisabled" value="${publicationDeleteDisabledByItemId[item.id]}" />
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
                        <li>
                          <c:choose>
                            <c:when test="${deleteDisabled}">
                              <button type="button" class="flex w-full cursor-not-allowed items-center gap-2 rounded-lg px-3 py-2 text-left text-error opacity-50" disabled="disabled" title="${deleteDisabledFutureBookingsLabel}">
                                <span class="material-symbols-outlined text-base leading-none">delete</span><span><c:out value="${deleteLabel}" /></span>
                              </button>
                            </c:when>
                            <c:otherwise>
                              <button type="button" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-error" onclick="document.getElementById('${deleteModalId}').showModal()"><span class="material-symbols-outlined text-base leading-none">delete</span><span><c:out value="${deleteLabel}" /></span></button>
                            </c:otherwise>
                          </c:choose>
                        </li>
                      </paw:kebabMenu>
                    </div>
                    <c:if test="${!deleteDisabled}">
                      <paw:confirmModal id="${deleteModalId}" title="${deleteConfirmTitle}" message="${deleteModalMessage}" confirmText="${deleteConfirmConfirm}" cancelText="${deleteConfirmCancel}" confirmColor="danger" icon="delete_forever">
                        <form action="${deleteItemUrl}" method="post" class="m-0">
                          <paw:button type="submit" color="danger" cssClass="w-full sm:w-auto" text="${deleteConfirmConfirm}" />
                        </form>
                      </paw:confirmModal>
                    </c:if>
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise>
                <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.publications.empty" /></p>
              </c:otherwise>
            </c:choose>
          </div>

          <div id="received-booking-requests" class="scroll-mt-24 space-y-4">
            <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.bookings.title" /></h2>
            <form action="<c:url value='/my-boats' />" method="get" class="flex flex-col gap-3 rounded-xl bg-base-200 p-4 sm:flex-row sm:items-end">
              <label class="form-control w-full sm:max-w-sm">
                <span class="label-text text-xs font-bold uppercase tracking-wider text-outline"><spring:message code="dashboard.filters.boatName" /></span>
                <span class="join w-full">
                  <input type="search" name="q" value="${fn:escapeXml(boatSearchQuery)}" class="input input-bordered input-sm join-item w-full" placeholder="<spring:message code="dashboard.filters.boatName.placeholder" />" />
                  <button type="submit" class="btn btn-primary btn-sm join-item px-3" aria-label="<spring:message code="dashboard.filters.search" />">
                    <span class="material-symbols-outlined text-base">search</span>
                  </button>
                </span>
              </label>
              <div class="form-control w-full sm:max-w-sm">
                <span class="label-text text-xs font-bold uppercase tracking-wider text-outline"><spring:message code="dashboard.filters.status" /></span>
                <details class="dropdown w-full">
                  <summary class="btn btn-outline btn-sm w-full justify-between no-underline">
                    <span><spring:message code="dashboard.filters.status" /></span>
                    <span class="material-symbols-outlined text-base">expand_more</span>
                  </summary>
                  <div class="dropdown-content z-20 mt-2 w-full rounded-xl border border-outline-variant/30 bg-base-100 p-3 shadow-lg">
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="pending" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.pending ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.pending" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="confirmed" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.confirmed ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.confirmed" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="paymentSubmitted" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.paymentSubmitted ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.paymentSubmitted" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="paid" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.paid ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.paid" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="paymentRefused" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.paymentRefused ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.paymentRefused" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="completed" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.completed ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.completed" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="rejected" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.rejected ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.rejected" />
                    </label>
                    <label class="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-semibold hover:bg-base-200">
                      <input type="checkbox" name="status" value="cancelled" class="checkbox checkbox-sm" ${selectedBookingStatusFiltersByValue.cancelled ? 'checked="checked"' : ''} />
                      <spring:message code="dashboard.filters.status.cancelled" />
                    </label>
                  </div>
                </details>
              </div>
              <a href="<c:url value='/my-boats' />" class="btn btn-outline btn-sm border-base-300 bg-base-100 no-underline hover:bg-base-100 sm:ml-auto">
                <spring:message code="dashboard.filters.clear" />
              </a>
            </form>
            <c:if test="${param.reviewAction == 'created'}"><paw:alertMessage type="success"><spring:message code="profile.reviews.created" /></paw:alertMessage></c:if>
            <c:if test="${param.reviewAction == 'validationError'}"><paw:alertMessage type="error"><spring:message code="profile.reviews.validationError" /></paw:alertMessage></c:if>
            <c:if test="${param.reviewAction == 'error'}"><paw:alertMessage type="error"><spring:message code="profile.reviews.error" /></paw:alertMessage></c:if>
            <c:if test="${param.bookingAction == 'accepted'}"><paw:alertMessage type="success"><spring:message code="profile.bookings.accepted" /></paw:alertMessage></c:if>
            <c:if test="${param.bookingAction == 'rejected'}"><paw:alertMessage type="warning"><spring:message code="profile.bookings.rejected" /></paw:alertMessage></c:if>
            <c:if test="${param.bookingAction == 'forbidden' || param.bookingAction == 'error' || param.bookingAction == 'notFound'}"><paw:alertMessage type="error"><spring:message code="profile.bookings.error" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'paid'}"><paw:alertMessage type="success"><spring:message code="profile.payment.paid" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'refused'}"><paw:alertMessage type="success"><spring:message code="profile.payment.refused" /></paw:alertMessage></c:if>
            <c:if test="${param.paymentAction == 'confirmError' || param.paymentAction == 'refuseError'}"><paw:alertMessage type="error"><spring:message code="profile.payment.error" /></paw:alertMessage></c:if>
            <c:choose>
              <c:when test="${not empty receivedBookingRequests}">
                <div class="grid grid-cols-1 min-[56rem]:grid-cols-2 gap-4">
                  <c:forEach var="receivedRequest" items="${receivedBookingRequests}">
                    <c:url var="acceptBookingUrl" value="/bookings/${receivedRequest.id}/accept" />
                    <c:url var="declineBookingUrl" value="/bookings/${receivedRequest.id}/decline" />
                    <c:url var="receivedPaymentProofUrl" value="/bookings/${receivedRequest.id}/payment-proof" />
                    <c:url var="confirmPaymentUrl" value="/bookings/${receivedRequest.id}/payment/confirm" />
                    <c:set var="authoredUserReview" value="${authoredUserReviewsByBookingId[receivedRequest.id]}" />
                    <c:set var="receivedStatusClass" value="badge-ghost" />
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.pending'}"><c:set var="receivedStatusClass" value="badge-warning" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}"><c:set var="receivedStatusClass" value="badge-success" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.rejected' || receivedRequest.statusMessageCode == 'profile.sentBookings.status.cancelled'}"><c:set var="receivedStatusClass" value="badge-error" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted'}"><c:set var="receivedStatusClass" value="badge-info" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paid'}"><c:set var="receivedStatusClass" value="badge-success" /></c:if>
                    <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentRefused'}"><c:set var="receivedStatusClass" value="badge-error" /></c:if>
                    <c:url var="refusePaymentUrl" value="/bookings/${receivedRequest.id}/payment/refuse" />
                    <c:if test="${not empty receivedRequest.imageId}">
                      <c:url var="receivedRequestImageUrl" value="/image/${receivedRequest.imageId}" />
                    </c:if>
                    <div class="rounded-xl bg-base-200 p-4 space-y-4">
                      <div class="flex items-start justify-end">
                        <span class="badge ${receivedStatusClass} badge-sm shrink-0 font-bold"><spring:message code="${receivedRequest.statusMessageCode}" /></span>
                      </div>
                      <div class="flex min-w-0 flex-col gap-4 sm:flex-row">
                        <div class="h-40 w-full shrink-0 overflow-hidden rounded-lg bg-base-100 sm:w-44">
                          <c:choose>
                            <c:when test="${not empty receivedRequest.imageId}">
                              <img src="${receivedRequestImageUrl}" alt="" class="h-full w-full object-cover" loading="lazy" />
                            </c:when>
                            <c:otherwise>
                              <div class="flex h-full w-full items-center justify-center text-outline">
                                <span class="material-symbols-outlined text-3xl">directions_boat</span>
                              </div>
                            </c:otherwise>
                          </c:choose>
                        </div>
                        <div class="min-w-0 flex flex-1 flex-col space-y-3">
                          <div class="space-y-2">
                            <p class="m-0 min-w-0 break-words text-base font-extrabold text-on-surface"><c:out value="${receivedRequest.itemTitle}" /></p>
                            <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedRequest.dateLabel}" /> · <c:out value="${receivedRequest.timeRangeLabel}" /></p>
                          </div>
                          <div class="grid grid-cols-1 gap-3 lg:grid-cols-2 sm:mt-auto">
                            <div class="rounded-lg bg-base-100 p-3 space-y-1">
                              <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.bookings.requester.label" /></p>
                              <p class="m-0 break-words text-sm font-bold text-on-surface"><c:out value="${receivedRequest.requesterName}" /></p>
                              <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${receivedRequest.requesterEmail}" /></p>
                              <p class="m-0 text-xs text-on-surface-variant flex items-center gap-1.5">
                                <span class="material-symbols-outlined text-sm text-warning leading-none">star</span>
                                <c:choose>
                                  <c:when test="${receivedRequest.requesterHasReviews}">
                                    <fmt:formatNumber value="${receivedRequest.requesterAverageRating}" minFractionDigits="1" maxFractionDigits="1" />
                                    <span>·</span>
                                    <spring:message code="profile.bookings.requester.rating.count" arguments="${receivedRequest.requesterTotalReviews}" />
                                  </c:when>
                                  <c:otherwise><spring:message code="profile.bookings.requester.rating.empty" /></c:otherwise>
                                </c:choose>
                              </p>
                            </div>
                            <div class="rounded-lg bg-base-100 p-3 space-y-2">
                              <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><c:out value="${paymentInfoPriceLabel}" /></p>
                              <p class="m-0 text-sm font-bold">$ <c:out value="${not empty receivedRequest.totalPriceLabel ? receivedRequest.totalPriceLabel : '-'}" /></p>
                              <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${paymentInfoAliasLabel}" />: <c:out value="${not empty receivedRequest.paymentAlias ? receivedRequest.paymentAlias : '-'}" /></p>
                            </div>
                          </div>
                          <c:if test="${not empty authoredUserReview}">
                            <p class="m-0 text-[11px] font-bold text-success flex items-center gap-1">
                              <span class="material-symbols-outlined text-sm leading-none">check_circle</span>
                              <spring:message code="profile.reviews.authoredSummary.done" />
                            </p>
                          </c:if>
                        </div>
                      </div>
                      <c:if test="${receivedRequest.hasPaymentProof}">
                        <a href="${receivedPaymentProofUrl}" class="link link-hover block max-w-full break-all text-sm font-bold text-primary"><c:out value="${receivedRequest.paymentProofFileName}" /></a>
                      </c:if>
                      <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.pending'}">
                        <div class="flex flex-wrap gap-2 border-t border-outline-variant/20 pt-3">
                          <form action="${acceptBookingUrl}" method="post" class="m-0" data-submit-loading-form="true"><paw:button type="submit" color="success" size="sm" text="${acceptLabel}" submitLoading="true" /></form>
                          <form action="${declineBookingUrl}" method="post" class="m-0" data-submit-loading-form="true"><paw:button type="submit" color="danger" variant="outline" size="sm" text="${declineLabel}" submitLoading="true" /></form>
                        </div>
                      </c:if>
                      <c:if test="${receivedRequest.hasPaymentGuestReply}">
                        <div class="rounded-lg bg-base-100 p-3 border-l-4 border-info">
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-info"><spring:message code="payment.guestReply.label" /></p>
                          <p class="m-0 mt-1 text-sm text-on-surface whitespace-pre-wrap"><c:out value="${receivedRequest.paymentGuestReply}" /></p>
                        </div>
                      </c:if>
                      <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted' && receivedRequest.hasPaymentProof}">
                        <div class="border-t border-outline-variant/20 pt-3 space-y-3">
                          <form action="${confirmPaymentUrl}" method="post" class="m-0" data-submit-loading-form="true"><paw:button type="submit" color="success" size="sm" text="${paymentReceivedLabel}" submitLoading="true" /></form>
                          <details class="rounded-lg bg-base-100 p-3">
                            <summary class="cursor-pointer text-sm font-bold text-error"><spring:message code="payment.refuse.button" /></summary>
                            <form action="${refusePaymentUrl}" method="post" class="mt-3 space-y-2" data-submit-loading-form="true">
                              <label class="text-[11px] font-bold uppercase tracking-wider text-outline" for="refuse-reason-${receivedRequest.id}"><spring:message code="payment.refuse.reason.label" /></label>
                              <textarea id="refuse-reason-${receivedRequest.id}" name="reason" rows="3" maxlength="500" required class="textarea textarea-bordered w-full" placeholder="<spring:message code="payment.refuse.reason.placeholder" />"></textarea>
                              <paw:button type="submit" color="danger" size="sm" text="${refuseSubmitLabel}" submitLoading="true" />
                            </form>
                          </details>
                        </div>
                      </c:if>
                      <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentRefused' && receivedRequest.hasPaymentRefusalReason}">
                        <div class="rounded-lg bg-error/10 p-3 border-l-4 border-error">
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-error"><spring:message code="payment.refused.shownToGuest" /></p>
                          <p class="m-0 mt-1 text-sm text-on-surface whitespace-pre-wrap"><c:out value="${receivedRequest.paymentRefusalReason}" /></p>
                        </div>
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
            <c:if test="${receivedBookingPage.totalPages > 1}">
              <c:url var="receivedPreviousPageUrl" value="/my-boats">
                <c:param name="page" value="${receivedBookingPage.previousPage}" />
                <c:if test="${not empty boatSearchQuery}"><c:param name="q" value="${boatSearchQuery}" /></c:if>
                <c:forEach var="selectedStatus" items="${selectedBookingStatusFilters}">
                  <c:param name="status" value="${selectedStatus}" />
                </c:forEach>
              </c:url>
              <c:url var="receivedNextPageUrl" value="/my-boats">
                <c:param name="page" value="${receivedBookingPage.nextPage}" />
                <c:if test="${not empty boatSearchQuery}"><c:param name="q" value="${boatSearchQuery}" /></c:if>
                <c:forEach var="selectedStatus" items="${selectedBookingStatusFilters}">
                  <c:param name="status" value="${selectedStatus}" />
                </c:forEach>
              </c:url>
              <nav class="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
                <c:choose>
                  <c:when test="${receivedBookingPage.hasPrevious}">
                    <a href="${receivedPreviousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2"><span class="material-symbols-outlined text-sm">arrow_back</span><spring:message code="marketplace.pagination.previous" /></a>
                  </c:when>
                  <c:otherwise><span class="btn btn-outline btn-sm btn-disabled gap-2"><span class="material-symbols-outlined text-sm">arrow_back</span><spring:message code="marketplace.pagination.previous" /></span></c:otherwise>
                </c:choose>
                <span><spring:message code="marketplace.pagination.page" arguments="${receivedBookingPage.page},${receivedBookingPage.totalPages}" /></span>
                <c:choose>
                  <c:when test="${receivedBookingPage.hasNext}">
                    <a href="${receivedNextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2"><spring:message code="marketplace.pagination.next" /><span class="material-symbols-outlined text-sm">arrow_forward</span></a>
                  </c:when>
                  <c:otherwise><span class="btn btn-outline btn-sm btn-disabled gap-2"><spring:message code="marketplace.pagination.next" /><span class="material-symbols-outlined text-sm">arrow_forward</span></span></c:otherwise>
                </c:choose>
              </nav>
            </c:if>
          </div>
      </div>
  </section>
</paw:layout>
