<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
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
<spring:message code="profile.reviews.rating.label" var="reviewRatingLabel" />
<spring:message code="profile.reviews.comment.label" var="reviewCommentLabel" />
<spring:message code="profile.reviews.submit" var="reviewSubmitLabel" />
<spring:message code="profile.reviews.target.item" var="reviewTargetItemLabel" />
<spring:message code="profile.reviews.target.user" var="reviewTargetUserLabel" />
<spring:message code="profile.reviews.authoredSummary.label" var="authoredReviewSummaryLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6">
  <paw:toastNotifier />
  <section class="min-w-0 space-y-6">
      <div class="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div class="min-w-0">
          <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words"><spring:message code="bookings.title" /></h1>
          <p class="text-on-surface-variant mt-2 m-0"><spring:message code="bookings.subtitle" /></p>
        </div>
      </div>
          <div id="sent-booking-requests" class="scroll-mt-24 space-y-4">
            <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.sentBookings.title" /></h2>
            <form action="<c:url value='/bookings' />" method="get" class="flex flex-col gap-3 rounded-xl bg-base-200 p-4 sm:flex-row sm:items-end">
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
              <a href="<c:url value='/bookings' />" class="btn btn-outline btn-sm border-base-300 bg-base-100 no-underline hover:bg-base-100 sm:ml-auto">
                <spring:message code="dashboard.filters.clear" />
              </a>
            </form>
            <c:choose>
              <c:when test="${not empty sentBookingRequests}">
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 sm:gap-4">
                  <c:forEach var="sentRequest" items="${sentBookingRequests}">
                    <c:url var="sentPaymentProofUrl" value="/bookings/${sentRequest.id}/payment-proof" />
                    <c:url var="sentRequestItemUrl" value="/item/${sentRequest.itemId}">
                      <c:if test="${not empty sentRequest.bookedSnapshotVersionId}">
                        <c:param name="snapshotVersionId" value="${sentRequest.bookedSnapshotVersionId}" />
                      </c:if>
                    </c:url>
                    <c:set var="authoredItemReview" value="${authoredItemReviewsByBookingId[sentRequest.id]}" />
                    <c:set var="sentStatusClass" value="badge-ghost" />
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.pending'}"><c:set var="sentStatusClass" value="badge-warning" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}"><c:set var="sentStatusClass" value="badge-success" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.rejected' || sentRequest.statusMessageCode == 'profile.sentBookings.status.cancelled'}"><c:set var="sentStatusClass" value="badge-error" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted'}"><c:set var="sentStatusClass" value="badge-info" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paid'}"><c:set var="sentStatusClass" value="badge-success" /></c:if>
                    <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentRefused'}"><c:set var="sentStatusClass" value="badge-error" /></c:if>
                    <c:set var="sentRequestImageUrl" value="${imageUrlsByItemId[sentRequest.itemId]}" />
                    <c:set var="sentDetailsModalId" value="sent-booking-details-modal-${sentRequest.id}" />
                    <button type="button" class="flex h-full w-full max-w-sm flex-col gap-2 rounded-xl bg-base-200 p-2 text-left transition hover:bg-base-300 sm:p-3" onclick="document.getElementById('${sentDetailsModalId}').showModal()">
                      <div class="h-24 w-full shrink-0 overflow-hidden rounded-lg bg-base-100 sm:h-32">
                        <img src="${sentRequestImageUrl}" alt="${sentRequest.itemTitle}" class="h-full w-full object-cover" loading="lazy" />
                      </div>
                      <div class="flex min-w-0 flex-1 flex-col gap-1">
                        <div class="flex min-w-0 items-start gap-1.5">
                          <p class="m-0 min-w-0 flex-1 break-words text-xs font-extrabold text-on-surface line-clamp-2 sm:text-sm">
                            <c:out value="${sentRequest.itemTitle}" />
                          </p>
                          <span class="badge ${sentStatusClass} badge-xs shrink-0 font-bold"><spring:message code="${sentRequest.statusMessageCode}" /></span>
                        </div>
                        <p class="m-0 truncate text-[10px] text-on-surface-variant sm:text-xs"><c:out value="${sentRequest.dateLabel}" /> · <c:out value="${sentRequest.timeRangeLabel}" /></p>
                        <p class="m-0 mt-auto text-[11px] font-bold sm:text-xs">$ <c:out value="${not empty sentRequest.totalPriceLabel ? sentRequest.totalPriceLabel : '-'}" /></p>
                      </div>
                    </button>
                    <paw:detailsModal id="${sentDetailsModalId}" title="${sentRequest.itemTitle}">
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="badge ${sentStatusClass} font-bold"><spring:message code="${sentRequest.statusMessageCode}" /></span>
                        <a href="${sentRequestItemUrl}" class="link link-hover text-xs no-underline">
                          <spring:message code="common.viewListing" />
                        </a>
                      </div>
                      <div class="overflow-hidden rounded-lg bg-base-100">
                        <img src="${sentRequestImageUrl}" alt="${sentRequest.itemTitle}" class="h-48 w-full object-cover" loading="lazy" />
                      </div>
                      <p class="m-0 text-sm text-on-surface-variant"><c:out value="${sentRequest.dateLabel}" /> · <c:out value="${sentRequest.timeRangeLabel}" /></p>
                      <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
                        <div class="rounded-lg bg-base-100 p-3 space-y-1">
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.sentBookings.owner.label" /></p>
                          <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${sentRequest.ownerName}" /></p>
                          <c:if test="${not empty sentRequest.ownerEmail}">
                            <p class="m-0 text-xs text-on-surface-variant break-all"><c:out value="${sentRequest.ownerEmail}" /></p>
                          </c:if>
                        </div>
                        <div class="rounded-lg bg-base-100 p-3 space-y-1">
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><c:out value="${paymentInfoPriceLabel}" /></p>
                          <p class="m-0 text-sm font-bold">$ <c:out value="${not empty sentRequest.totalPriceLabel ? sentRequest.totalPriceLabel : '-'}" /></p>
                          <c:if test="${not empty sentRequest.paymentAlias}">
                            <p class="m-0 text-xs text-on-surface-variant break-all"><c:out value="${paymentInfoAliasLabel}" />: <c:out value="${sentRequest.paymentAlias}" /></p>
                          </c:if>
                        </div>
                      </div>
                      <c:if test="${not empty authoredItemReview}">
                        <p class="m-0 flex items-center gap-1 text-xs font-bold text-success">
                          <span class="material-symbols-outlined text-sm leading-none">check_circle</span>
                          <spring:message code="profile.reviews.authoredSummary.done" />
                        </p>
                      </c:if>
                      <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}">
                        <form action="${sentPaymentProofUrl}" method="post" enctype="multipart/form-data" class="space-y-2 border-t border-outline-variant/20 pt-3" data-submit-loading-form="true">
                          <input type="file" name="file" accept="application/pdf,image/png,image/jpeg,image/webp" class="file-input file-input-bordered file-input-sm w-full" />
                          <paw:button type="submit" color="primary" size="sm" text="${uploadPaymentProofLabel}" submitLoading="true" />
                        </form>
                      </c:if>
                      <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentRefused'}">
                        <div class="rounded-lg bg-error/10 p-3 border-l-4 border-error space-y-2">
                          <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-error"><spring:message code="payment.refused.banner" /></p>
                          <c:if test="${sentRequest.hasPaymentRefusalReason}">
                            <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="payment.refused.reasonLabel" /></p>
                            <p class="m-0 break-words text-sm text-on-surface whitespace-pre-line"><c:out value="${sentRequest.paymentRefusalReason}" /></p>
                          </c:if>
                        </div>
                        <form action="${sentPaymentProofUrl}" method="post" enctype="multipart/form-data" class="space-y-2 border-t border-outline-variant/20 pt-3" data-submit-loading-form="true">
                          <input type="file" name="file" accept="application/pdf,image/png,image/jpeg,image/webp" class="file-input file-input-bordered file-input-sm w-full" />
                          <label class="text-[11px] font-bold uppercase tracking-wider text-outline" for="guest-reply-${sentRequest.id}"><spring:message code="payment.reply.label" /></label>
                          <textarea id="guest-reply-${sentRequest.id}" name="guestReply" rows="2" maxlength="500" class="textarea textarea-bordered w-full" placeholder="<spring:message code="payment.reply.placeholder" />"></textarea>
                          <paw:button type="submit" color="primary" size="sm" text="${uploadPaymentProofLabel}" submitLoading="true" />
                        </form>
                        <details class="rounded-lg bg-base-100 p-3">
                          <summary class="cursor-pointer text-xs font-bold text-primary"><spring:message code="profile.sentBookings.paymentProof.view" /></summary>
                          <div class="mt-3 overflow-hidden rounded-lg border border-outline-variant/20 bg-base-200/40">
                            <c:choose>
                              <c:when test="${sentRequest.isPaymentProofPdf}">
                                <embed src="${sentPaymentProofUrl}" type="application/pdf" class="h-80 w-full" />
                              </c:when>
                              <c:otherwise>
                                <img src="${sentPaymentProofUrl}" alt="<spring:message code='profile.sentBookings.paymentProof.view' />" class="max-h-80 w-full object-contain" loading="lazy" />
                              </c:otherwise>
                            </c:choose>
                          </div>
                        </details>
                      </c:if>
                      <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted' || sentRequest.statusMessageCode == 'profile.sentBookings.status.paid'}">
                        <details class="rounded-lg bg-base-100 p-3 border-t border-outline-variant/20 pt-3">
                          <summary class="cursor-pointer text-sm font-bold text-primary"><spring:message code="profile.sentBookings.paymentProof.view" /></summary>
                          <div class="mt-3 overflow-hidden rounded-lg border border-outline-variant/20 bg-base-200/40">
                            <c:choose>
                              <c:when test="${sentRequest.isPaymentProofPdf}">
                                <embed src="${sentPaymentProofUrl}" type="application/pdf" class="h-80 w-full" />
                              </c:when>
                              <c:otherwise>
                                <img src="${sentPaymentProofUrl}" alt="<spring:message code='profile.sentBookings.paymentProof.view' />" class="max-h-80 w-full object-contain" loading="lazy" />
                              </c:otherwise>
                            </c:choose>
                          </div>
                        </details>
                      </c:if>
                      <c:set var="guestPendingReview" value="${pendingGuestItemReviewsByBookingId[sentRequest.id]}" />
                      <c:if test="${not empty guestPendingReview}">
                        <form action="/reviews/booking/${sentRequest.id}" method="post" class="space-y-3 border-t border-outline-variant/20 pt-3">
                          <input type="hidden" name="returnTo" value="dashboardBookings" />
                          <div class="flex items-start justify-between gap-2">
                            <p class="m-0 min-w-0 truncate text-xs text-on-surface-variant"><c:out value="${guestPendingReview.targetName}" /> · <c:out value="${guestPendingReview.targetEmail}" /></p>
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
                            <p class="m-0 break-words text-xs text-on-surface-variant"><c:out value="${authoredItemReview.comment}" /></p>
                          </c:if>
                        </div>
                      </c:if>
                    </paw:detailsModal>
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise>
                <div class="rounded-xl bg-base-200 px-4 py-6 text-center">
                  <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.sentBookings.empty" /></p>
                </div>
              </c:otherwise>
            </c:choose>
            <c:if test="${sentBookingPage.totalPages > 1}">
              <c:url var="sentPreviousPageUrl" value="/bookings">
                <c:param name="page" value="${sentBookingPage.previousPage}" />
                <c:if test="${not empty boatSearchQuery}"><c:param name="q" value="${boatSearchQuery}" /></c:if>
                <c:forEach var="selectedStatus" items="${selectedBookingStatusFilters}">
                  <c:param name="status" value="${selectedStatus}" />
                </c:forEach>
              </c:url>
              <c:url var="sentNextPageUrl" value="/bookings">
                <c:param name="page" value="${sentBookingPage.nextPage}" />
                <c:if test="${not empty boatSearchQuery}"><c:param name="q" value="${boatSearchQuery}" /></c:if>
                <c:forEach var="selectedStatus" items="${selectedBookingStatusFilters}">
                  <c:param name="status" value="${selectedStatus}" />
                </c:forEach>
              </c:url>
              <nav class="flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
                <c:choose>
                  <c:when test="${sentBookingPage.hasPrevious}">
                    <a href="${sentPreviousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2"><span class="material-symbols-outlined text-sm">arrow_back</span><spring:message code="marketplace.pagination.previous" /></a>
                  </c:when>
                  <c:otherwise><span class="btn btn-outline btn-sm btn-disabled gap-2"><span class="material-symbols-outlined text-sm">arrow_back</span><spring:message code="marketplace.pagination.previous" /></span></c:otherwise>
                </c:choose>
                <span><spring:message code="marketplace.pagination.page" arguments="${sentBookingPage.page},${sentBookingPage.totalPages}" /></span>
                <c:choose>
                  <c:when test="${sentBookingPage.hasNext}">
                    <a href="${sentNextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2"><spring:message code="marketplace.pagination.next" /><span class="material-symbols-outlined text-sm">arrow_forward</span></a>
                  </c:when>
                  <c:otherwise><span class="btn btn-outline btn-sm btn-disabled gap-2"><spring:message code="marketplace.pagination.next" /><span class="material-symbols-outlined text-sm">arrow_forward</span></span></c:otherwise>
                </c:choose>
              </nav>
            </c:if>
          </div>
  </section>
</paw:layout>
