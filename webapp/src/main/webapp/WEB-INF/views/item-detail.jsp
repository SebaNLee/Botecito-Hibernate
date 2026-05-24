<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
<c:url var="publishUrl" value="/publish" />
<spring:message code="itemDetail.unavailable.mismatchPrefix" var="unavailableMismatchPrefix" />
<spring:message code="itemDetail.unavailable.mismatchSuffix" var="unavailableMismatchSuffix" />
<spring:message code="common.and" var="andLabel" />
<spring:message code="itemDetail.unavailable.reason.location" var="unavailableReasonLocation" />
<spring:message code="itemDetail.unavailable.reason.capacity" var="unavailableReasonCapacity" />
<spring:message code="itemDetail.unavailable.reason.weight" var="unavailableReasonWeight" />
<spring:message code="itemDetail.unavailable.reason.dateTime" var="unavailableReasonDateTime" />
<spring:message code="itemDetail.unavailable.reason.difficulty" var="unavailableReasonDifficulty" />
<spring:message code="itemDetail.unavailable.clear" var="unavailableClearLabel" />
<spring:message code="itemDetail.unavailable.backToMarketplace" var="unavailableBackLabel" />
<spring:message code="itemDetail.contact.sendEmail" var="contactSendEmailLabel" />
<spring:message code="itemDetail.reviews.title" var="itemReviewsTitle" />
<spring:message code="itemDetail.reviews.empty" var="itemReviewsEmpty" />
<spring:message code="itemDetail.reviews.count" var="itemReviewsCountLabel" />
<spring:message code="itemDetail.reviews.leave" var="itemReviewLeaveLabel" />
<spring:message code="itemDetail.reviews.rating" var="itemReviewRatingLabel" />
<spring:message code="itemDetail.reviews.comment" var="itemReviewCommentLabel" />
<spring:message code="reviews.empty.short" var="reviewsEmptyShortLabel" />
<spring:message code="itemDetail.description.empty" var="itemDescriptionEmptyLabel" />
<spring:message code="detail.preBooking.subtitle" var="detailPreBookingSubtitle" />
<c:url var="prebookLoginUrl" value="/login" />
<spring:message code="itemDetail.form.loginToBook" var="itemDetailLoginToBookLabel" />
<spring:message code="itemDetail.form.requestBooking" var="itemDetailRequestBookingLabel" />
<spring:message code="filters.date" var="itemDetailDateLabel" />
<spring:message code="filters.date.placeholder" var="itemDetailDatePlaceholder" />
<spring:message code="filters.time" var="itemDetailTimeLabel" />
<spring:message code="filters.time.placeholder" var="itemDetailTimePlaceholder" />
<spring:message code="itemDetail.form.submit" var="itemDetailFormSubmitLabel" />
<spring:message code="itemDetail.form.message" var="itemDetailRequestMessageLabel" />
<spring:message code="itemDetail.form.addMessage" var="itemDetailAddMessageLabel" />
<spring:message code="itemDetail.form.message.placeholder" var="itemDetailRequestMessagePlaceholder" />
<spring:message code="itemDetail.price.total" var="itemDetailPriceTotalLabel" />
<spring:message code="itemDetail.price.pending" var="itemDetailPricePendingLabel" />
<spring:message code="itemDetail.price.pendingHelp" var="itemDetailPricePendingHelpLabel" />
<spring:message code="itemDetail.price.pickEnd" var="itemDetailPricePickEndLabel" />
<spring:message code="itemDetail.price.for" var="itemDetailPriceForLabel" />
<spring:message code="itemDetail.price.hour" var="itemDetailPriceHourLabel" />
<spring:message code="itemDetail.price.hours" var="itemDetailPriceHoursLabel" />
<spring:message code="detail.reviews.anonymous" var="detailReviewAnonymousLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-12 w-full max-w-7xl mx-auto px-6 flex flex-col gap-8" scripts="toast,search-filters,date-time,form-submit,pre-booking-draft,image-carousel,rating-stars">
  <paw:toastNotifier />
      <div class="w-full">
        <a href="<c:out value="${marketplaceBackHref}" />" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
          <span class="material-symbols-outlined">arrow_back</span>
          <span><spring:message code="common.back" /></span>
        </a>
      </div>

  <c:if test="${listingInactiveNotice}">
    <paw:alertMessage type="warning"><spring:message code="itemDetail.listingInactive.notice" /></paw:alertMessage>
  </c:if>

  <div class="w-full grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_400px] gap-8 items-start">
    <section class="order-2 lg:order-1 min-w-0 space-y-8">
      <paw:imageCarousel imageUrls="${itemImageUrls}" altText="${version.title}" />

      <paw:sectionCard>
        <jsp:attribute name="title"><spring:message code="itemDetail.description.title" /></jsp:attribute>
        <jsp:body>
          <c:choose>
            <c:when test="${not empty version.description}">
              <p class="m-0 break-words text-on-surface-variant leading-relaxed">
                <c:out value="${version.description}" />
              </p>
            </c:when>
            <c:otherwise>
              <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/45 px-4 py-3 text-sm italic text-outline">
                <c:out value="${itemDescriptionEmptyLabel}" />
              </p>
            </c:otherwise>
          </c:choose>
        </jsp:body>
      </paw:sectionCard>

      <paw:sectionCard icon="fact_check">
        <jsp:attribute name="title"><spring:message code="itemDetail.specs.title" /></jsp:attribute>
        <jsp:body>
          <ul class="grid grid-cols-1 sm:grid-cols-2 gap-4 m-0 p-0 list-none">
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="publish.form.type.label" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <c:choose>
                  <c:when test="${not empty version.type.name}"><c:out value="${version.type.name}" /></c:when>
                  <c:otherwise><spring:message code="itemDetail.type.none" /></c:otherwise>
                </c:choose>
              </span>
            </li>
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="itemDetail.capacity.label" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <spring:message code="itemDetail.capacity" arguments="${version.capacity}" />
              </span>
            </li>
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="itemDetail.weight.short" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <spring:message code="itemDetail.weight" arguments="${version.weight}" />
              </span>
            </li>
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="publish.form.difficulty.label" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <spring:message code="itemDetail.difficulty" arguments="${version.difficulty}" />
              </span>
            </li>
          </ul>
        </jsp:body>
      </paw:sectionCard>

      <paw:sectionCard icon="person">
        <jsp:attribute name="title"><spring:message code="itemDetail.contact.host" /></jsp:attribute>
        <jsp:body>
          <div class="flex min-w-0 flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex min-w-0 items-center gap-4">
              <div class="avatar placeholder shrink-0">
                <div class="bg-primary/10 text-primary rounded-full w-14 h-14 flex items-center justify-center">
                  <span class="font-extrabold text-xl"><c:out value="${ownerInitials}" /></span>
                </div>
              </div>
              <div class="min-w-0">
                <h3 class="font-extrabold text-lg m-0 break-words">
                  <c:choose>
                    <c:when test="${itemOwner != null}"><c:out value="${itemOwnerDisplayName}" /></c:when>
                    <c:otherwise><spring:message code="itemDetail.owner.none" /></c:otherwise>
                  </c:choose>
                </h3>
                <div class="break-all text-xs text-on-surface-variant">
                  <c:choose>
                    <c:when test="${itemOwner != null}"><c:out value="${itemOwner.email}" /></c:when>
                    <c:otherwise><spring:message code="itemDetail.owner.noEmail" /></c:otherwise>
                  </c:choose>
                </div>
              </div>
            </div>
            <paw:button
              href="mailto:${itemOwner != null ? itemOwner.email : ''}"
              color="outline"
              icon="mail"
              cssClass="w-full sm:w-auto"
              text="${contactSendEmailLabel}"
            />
          </div>
        </jsp:body>
      </paw:sectionCard>

      <paw:sectionCard icon="star">
        <jsp:attribute name="title"><c:out value="${itemReviewsTitle}" /></jsp:attribute>
        <jsp:body>
          <div class="space-y-5">
            <div class="rounded-2xl bg-base-200 px-4 py-3 flex items-center justify-between gap-3">
              <div class="flex items-center gap-2 text-lg font-black text-on-surface">
                <span class="material-symbols-outlined text-warning">star</span>
                <c:choose>
                  <c:when test="${totalReviews > 0}">
                    <fmt:formatNumber value="${averageRating}" minFractionDigits="1" maxFractionDigits="1" />
                  </c:when>
                  <c:otherwise><c:out value="${reviewsEmptyShortLabel}" /></c:otherwise>
                </c:choose>
              </div>
              <p class="m-0 text-xs text-on-surface-variant">
                <spring:message code="itemDetail.reviews.count" arguments="${totalReviews}" />
              </p>
            </div>

            <c:if test="${pendingItemReviewAction != null}">
              <c:url var="createItemReviewUrl" value="/reviews/booking/${pendingItemReviewAction.bookingId}" />
              <form action="${createItemReviewUrl}" method="post" class="rounded-2xl bg-base-200 p-4 space-y-3">
                <input type="hidden" name="returnTo" value="item" />
                <input type="hidden" name="itemId" value="${item.id}" />
                <h3 class="m-0 text-sm font-bold text-on-surface"><c:out value="${itemReviewLeaveLabel}" /></h3>
                <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3 items-center">
                  <label class="text-xs font-bold uppercase tracking-wider text-outline" for="item-review-rating"><c:out value="${itemReviewRatingLabel}" /></label>
                  <div class="flex items-center gap-1" data-rating-stars>
                    <input id="item-review-rating" type="hidden" name="rating" value="" data-rating-value />
                    <c:forEach var="starIndex" begin="1" end="5">
                      <button type="button" class="btn btn-ghost btn-sm btn-square min-h-9 h-9 w-9 p-0" data-rating-star="${starIndex}" aria-label="${itemReviewRatingLabel} ${starIndex}">
                        <span class="material-symbols-outlined text-xl leading-none text-outline" style="opacity: 0.35;">star</span>
                      </button>
                    </c:forEach>
                  </div>
                </div>
                <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
                  <label class="text-xs font-bold uppercase tracking-wider text-outline pt-2" for="item-review-comment"><c:out value="${itemReviewCommentLabel}" /></label>
                  <textarea id="item-review-comment" name="comment" rows="3" maxlength="1000" class="textarea textarea-bordered w-full"></textarea>
                </div>
                <paw:button type="submit" color="primary" size="sm" text="${itemReviewLeaveLabel}" />
              </form>
            </c:if>

            <c:choose>
              <c:when test="${not empty reviewPage.content}">
                <div class="space-y-3">
                  <c:forEach items="${reviewPage.content}" var="review">
                    <c:set var="fullStars" value="${review.rating ge 5 ? 5 : (review.rating ge 4 ? 4 : (review.rating ge 3 ? 3 : (review.rating ge 2 ? 2 : (review.rating ge 1 ? 1 : 0))))}" />
                    <div class="rounded-xl bg-base-200 p-4 space-y-2">
                      <div class="flex items-center justify-between gap-3">
                        <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${detailReviewAnonymousLabel}" /></p>
                        <div class="flex flex-col items-end gap-0.5 shrink-0">
                          <div class="flex items-center gap-0.5" aria-label="${review.rating} of 5">
                            <c:forEach var="starIndex" begin="1" end="5">
                              <c:choose>
                                <c:when test="${starIndex <= fullStars}">
                                  <span class="material-symbols-outlined text-sm leading-none text-warning">star</span>
                                </c:when>
                                <c:otherwise>
                                  <span class="material-symbols-outlined text-sm leading-none text-outline opacity-[0.35]">star</span>
                                </c:otherwise>
                              </c:choose>
                            </c:forEach>
                          </div>
                          <span class="text-[10px] font-bold text-on-surface-variant tabular-nums">
                            <fmt:formatNumber value="${review.rating}" minFractionDigits="1" maxFractionDigits="1" />
                          </span>
                        </div>
                      </div>
                      <p class="m-0 text-xs text-on-surface-variant">
                        <time datetime="${review.createdAt}"><c:out value="${review.createdAt}" /></time>
                      </p>
                      <c:if test="${not empty review.comment}">
                        <p class="m-0 text-sm text-on-surface-variant break-words"><c:out value="${review.comment}" /></p>
                      </c:if>
                    </div>
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise>
                <p class="m-0 text-sm text-on-surface-variant"><c:out value="${itemReviewsEmpty}" /></p>
              </c:otherwise>
            </c:choose>

            <c:if test="${reviewPage.totalPages > 1}">
              <div class="flex items-center justify-between gap-3 pt-2">
                <c:url var="reviewPrevUrl" value="/item/${item.id}">
                  <c:param name="reviewPage" value="${reviewPage.previousPage}" />
                  <c:if test="${not empty param.returnTo}">
                    <c:param name="returnTo" value="${param.returnTo}" />
                  </c:if>
                </c:url>
                <c:url var="reviewNextUrl" value="/item/${item.id}">
                  <c:param name="reviewPage" value="${reviewPage.nextPage}" />
                  <c:if test="${not empty param.returnTo}">
                    <c:param name="returnTo" value="${param.returnTo}" />
                  </c:if>
                </c:url>
                <c:choose>
                  <c:when test="${reviewPage.hasPrevious}">
                    <a href="${reviewPrevUrl}" class="btn btn-outline btn-xs no-underline">
                      <spring:message code="marketplace.pagination.previous" />
                    </a>
                  </c:when>
                  <c:otherwise>
                    <span class="btn btn-outline btn-xs btn-disabled">
                      <spring:message code="marketplace.pagination.previous" />
                    </span>
                  </c:otherwise>
                </c:choose>
                <span class="text-xs text-on-surface-variant">
                  <spring:message code="marketplace.pagination.page" arguments="${reviewPage.page},${reviewPage.totalPages}" />
                </span>
                <c:choose>
                  <c:when test="${reviewPage.hasNext}">
                    <a href="${reviewNextUrl}" class="btn btn-outline btn-xs no-underline">
                      <spring:message code="marketplace.pagination.next" />
                    </a>
                  </c:when>
                  <c:otherwise>
                    <span class="btn btn-outline btn-xs btn-disabled">
                      <spring:message code="marketplace.pagination.next" />
                    </span>
                  </c:otherwise>
                </c:choose>
              </div>
            </c:if>
          </div>
        </jsp:body>
      </paw:sectionCard>
    </section>

    <aside class="order-1 lg:order-2 w-full min-w-0 lg:sticky lg:top-24 space-y-6">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-8 gap-4">
          <h1 class="text-3xl font-extrabold tracking-tight m-0 break-words">
            <c:out value="${version.title}" />
          </h1>
          <div class="flex min-w-0 items-center text-on-surface-variant text-sm gap-1">
            <span class="material-symbols-outlined text-primary text-lg">location_on</span>
            <span class="min-w-0 break-words"><c:out value="${version.location.name}" /></span>
          </div>
          <div class="flex items-baseline gap-2">
            <span class="text-4xl font-black text-primary whitespace-nowrap">
              $<fmt:formatNumber value="${version.price}" type="number" groupingUsed="true" maxFractionDigits="0" />
            </span>
            <span class="text-xs font-bold uppercase tracking-wider text-outline"><spring:message code="marketplace.card.perHour" /></span>
          </div>

          <c:if test="${showPreBookingPanel}">
              <div
                  data-prebook-draft-root
                  data-item-id="${item.id}"
                  data-viewer-logged-in="${viewer != null ? 'true' : 'false'}"
                  data-login-url="<c:out value="${prebookLoginUrl}" />">
                <div
                    class="hidden rounded-2xl bg-base-200 px-4 py-4"
                    data-reservation-price-summary
                    data-price-per-hour="${version.price}"
                    data-currency-symbol="$"
                    data-price-pending="${fn:escapeXml(itemDetailPricePendingLabel)}"
                    data-price-pending-help="${fn:escapeXml(itemDetailPricePendingHelpLabel)}"
                    data-price-pick-end="${fn:escapeXml(itemDetailPricePickEndLabel)}"
                    data-price-for-label="${fn:escapeXml(itemDetailPriceForLabel)}"
                    data-price-hour-label="${fn:escapeXml(itemDetailPriceHourLabel)}"
                    data-price-hours-label="${fn:escapeXml(itemDetailPriceHoursLabel)}">
                  <div class="flex items-baseline justify-between gap-3">
                    <span class="text-[10px] font-bold uppercase tracking-wider text-outline"><c:out value="${itemDetailPriceTotalLabel}" /></span>
                    <span class="text-2xl font-black text-primary" data-price-total><c:out value="${itemDetailPricePendingLabel}" /></span>
                  </div>
                  <p class="mb-0 mt-2 text-xs text-on-surface-variant" data-price-duration><c:out value="${itemDetailPricePendingHelpLabel}" /></p>
                </div>
                <c:url var="preBookingPostUrl" value="/item/${item.id}" />
                <form:form
                    id="detail-prebook-form"
                    modelAttribute="preBookingForm"
                    action="${preBookingPostUrl}"
                    method="post"
                    cssClass="space-y-4"
                    data-submit-loading-form="true">
                  <spring:hasBindErrors name="preBookingForm">
                    <c:if test="${errors.hasGlobalErrors()}">
                      <c:forEach items="${errors.globalErrors}" var="globalErr">
                        <paw:alertMessage type="error"><spring:message code="${globalErr.code}" /></paw:alertMessage>
                      </c:forEach>
                    </c:if>
                  </spring:hasBindErrors>
                  <p class="m-0 text-sm text-on-surface-variant"><c:out value="${detailPreBookingSubtitle}" /></p>
                  <c:if test="${not empty detailListingTimezoneId}">
                    <p class="m-0 text-xs font-semibold text-on-surface rounded-xl border border-outline-variant/30 bg-base-200/50 px-3 py-2">
                      <spring:message code="detail.preBooking.timezoneNotice" arguments="${detailListingTimezoneId}" />
                    </p>
                  </c:if>
                  <form:hidden path="versionId" />
                  <c:if test="${not empty param.returnTo}">
                    <input type="hidden" name="returnTo" value="<c:out value="${param.returnTo}" />" />
                  </c:if>
                  <paw:datePicker
                      id="detail-prebook-date"
                      dateFieldName="date"
                      label="${itemDetailDateLabel}"
                      value="${preBookingForm.date}"
                      placeholder="${itemDetailDatePlaceholder}"
                      restrictToAvailability="true"
                      offeredDatesJson="${detailOfferedDatesJson}"
                      occupiedDatesJson="${detailOccupiedDatesJson}"
                      anchorTodayIso="${detailListingTodayIso}"
                      anchorMaxDateIso="${detailListingMaxDateIso}"
                      civilCalendar="true" />
                  <form:errors path="date" element="p" cssClass="text-error text-xs mt-1" />
                  <paw:timeRangePicker
                      id="detail-prebook-time"
                      dateInputId="detail-prebook-date"
                      startTimeFieldName="startTime"
                      endTimeFieldName="endTime"
                      label="${itemDetailTimeLabel}"
                      startValue="${preBookingForm.startTime}"
                      endValue="${preBookingForm.endTime}"
                      placeholder="${itemDetailTimePlaceholder}"
                      restrictToAvailability="true"
                      offeredTimesJson="${detailOfferedTimesJson}"
                      occupiedTimesJson="${detailOccupiedTimesJson}" />
                  <form:errors path="startTime" element="p" cssClass="text-error text-xs mt-1" />
                  <form:errors path="endTime" element="p" cssClass="text-error text-xs mt-1" />

                  <label class="label cursor-pointer justify-start gap-3 rounded-xl bg-base-200 px-4 py-3">
                    <input
                        type="checkbox"
                        class="checkbox checkbox-primary checkbox-sm"
                        data-optional-toggle="detail-prebook-message-panel"
                        ${not empty preBookingForm.message ? 'checked="checked"' : ''} />
                    <span class="label-text font-bold text-on-surface"><c:out value="${itemDetailAddMessageLabel}" /></span>
                  </label>
                  <div id="detail-prebook-message-panel" class="${empty preBookingForm.message ? 'hidden' : ''}" data-optional-panel>
                    <paw:textareaField
                        path="message"
                        label="${itemDetailRequestMessageLabel}"
                        placeholder="${itemDetailRequestMessagePlaceholder}"
                        rows="4"
                        maxlength="255" />
                  </div>

                  <c:choose>
                    <c:when test="${viewer == null}">
                      <paw:button
                          type="button"
                          color="primary"
                          text="${itemDetailLoginToBookLabel}"
                          fullWidth="true"
                          size="lg"
                          iconTrailing="true"
                          icon="chevron_right"
                          cssClass="mt-2 js-prebook-login" />
                    </c:when>
                    <c:otherwise>
                      <paw:button
                          type="submit"
                          color="primary"
                          text="${itemDetailRequestBookingLabel}"
                          submitLoading="true"
                          fullWidth="true"
                          size="lg"
                          iconTrailing="true"
                          icon="chevron_right"
                          cssClass="mt-2" />
                    </c:otherwise>
                  </c:choose>
                </form:form>
              </div>
          </c:if>
        </div>
      </div>
    </aside>
  </div>

  <dialog
    class="modal"
    data-item-unavailable-alert
    data-marketplace-url="<c:out value="${marketplaceBackHref}" />"
    data-item-location-option-id="${version.location.id}"
    data-item-location-slug="${itemLocationSlug}"
    data-item-capacity="${version.capacity}"
    data-item-weight="${version.weight}"
    data-item-difficulty="${version.difficulty}"
    data-mismatch-prefix="${unavailableMismatchPrefix}"
    data-mismatch-suffix="${unavailableMismatchSuffix}"
    data-mismatch-join="${andLabel}"
    data-mismatch-location="${unavailableReasonLocation}"
    data-mismatch-capacity="${unavailableReasonCapacity}"
    data-mismatch-weight="${unavailableReasonWeight}"
    data-mismatch-date-time="${unavailableReasonDateTime}"
    data-mismatch-difficulty="${unavailableReasonDifficulty}"
    hidden
  >
    <div class="modal-box max-w-lg p-0 bg-transparent shadow-none">
      <div class="card bg-base-100 shadow-xl">
        <div class="card-body p-8 gap-4">
          <div class="flex items-start gap-4">
            <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-error/15 text-error">
              <span class="material-symbols-outlined">warning</span>
            </div>
            <div class="space-y-2">
              <h2 class="card-title m-0 text-2xl font-extrabold tracking-tight">
                <spring:message code="itemDetail.unavailable.title" />
              </h2>
              <p class="m-0 leading-relaxed text-on-surface-variant" data-item-unavailable-message>
                <spring:message code="itemDetail.unavailable.message" />
              </p>
            </div>
          </div>
          <div class="card-actions mt-4 flex flex-col gap-3 sm:flex-row">
            <button type="button" class="btn btn-primary flex-1" data-item-unavailable-clear>
              <c:out value="${unavailableClearLabel}" />
            </button>
            <button type="button" class="btn btn-outline flex-1" data-item-unavailable-marketplace>
              <c:out value="${unavailableBackLabel}" />
            </button>
          </div>
        </div>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button aria-label="${unavailableBackLabel}">close</button>
    </form>
  </dialog>
</paw:layout>
