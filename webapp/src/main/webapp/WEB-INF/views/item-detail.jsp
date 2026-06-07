<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="publishUrl" value="/publish" />
<spring:message code="detail.back.marketplace" var="detailBackMarketplaceLabel" />
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
<spring:message code="subscription.subscribe" var="subscriptionSubscribeLabel" />
<spring:message code="subscription.unsubscribe" var="subscriptionUnsubscribeLabel" />
<spring:message code="subscription.loginToSubscribe" var="subscriptionLoginToSubscribeLabel" />
<spring:message code="favourite.add" var="favouriteAddLabel" />
<spring:message code="favourite.remove" var="favouriteRemoveLabel" />
<spring:message code="favourite.loginToAdd" var="favouriteLoginToAddLabel" />
<spring:message code="itemDetail.reviews.title" var="itemReviewsTitle" />
<spring:message code="itemDetail.reviews.empty" var="itemReviewsEmpty" />
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
<spring:message code="contact.sendEmail" var="contactSendEmailLabel" />
<spring:message code="itemDetail.owner.ownPublicationNotice" var="itemDetailOwnerNoticeLabel" />
<spring:message code="itemDetail.owner.blockButton" var="itemDetailOwnerBlockButtonLabel" />
<spring:message code="itemDetail.owner.incomingRequestsButton" var="itemDetailOwnerIncomingRequestsButtonLabel" />
<spring:message code="itemDetail.report.button" var="itemDetailReportButtonLabel" />
<spring:message code="itemDetail.report.alreadyReported" var="itemDetailReportAlreadyReportedLabel" />
<spring:message code="page.title.itemDetail" var="titleItemDetail" />

<c:set var="clearMarketplaceFiltersOnLoad" value="false" />
<c:forEach items="${toasts}" var="toast">
  <c:if test="${toast.code == 'detail.preBooking.success'}">
    <c:set var="clearMarketplaceFiltersOnLoad" value="true" />
  </c:if>
</c:forEach>

<paw:layout title="${titleItemDetail} - Botecito" mainClass="pt-24 pb-12 w-full max-w-7xl mx-auto px-6 flex flex-col gap-8" scripts="toast,search-filters,date-time,form-submit,pre-booking-draft,image-carousel,rating-stars">
  <c:if test="${clearMarketplaceFiltersOnLoad}">
    <div hidden data-clear-marketplace-filters="true"></div>
  </c:if>
  <paw:toastNotifier />
      <div class="w-full">
        <a href="${marketplaceUrl}" data-detail-marketplace-back data-nav-filter-page="marketplace" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
          <span class="material-symbols-outlined">arrow_back</span>
          <span><c:out value="${detailBackMarketplaceLabel}" /></span>
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
      <div id="item-reviews" class="scroll-mt-24">
      <paw:sectionCard icon="star">
        <jsp:attribute name="title"><c:out value="${itemReviewsTitle}" /></jsp:attribute>
        <jsp:body>
          <div class="space-y-5">
            <div class="rounded-2xl bg-base-200 px-4 py-3 flex items-center justify-between gap-3">
              <div class="flex items-center gap-2 text-lg font-black text-on-surface">
                <span class="material-symbols-outlined icon-star-filled">star</span>
                <c:choose>
                  <c:when test="${itemReviewPage.page.totalItems > 0}">
                    <fmt:formatNumber value="${itemReviewPage.averageRating}" minFractionDigits="1" maxFractionDigits="1" />
                  </c:when>
                  <c:otherwise><c:out value="${reviewsEmptyShortLabel}" /></c:otherwise>
                </c:choose>
              </div>
              <p class="m-0 text-xs text-on-surface-variant">
                <c:choose>
                  <c:when test="${itemReviewPage.page.totalItems == 1}">
                    <spring:message code="itemDetail.reviews.count.singular" />
                  </c:when>
                  <c:otherwise>
                    <spring:message code="itemDetail.reviews.count.plural" arguments="${itemReviewPage.page.totalItems}" />
                  </c:otherwise>
                </c:choose>
              </p>
            </div>

            <c:choose>
              <c:when test="${not empty itemReviewPage.page.content}">
                <div class="space-y-3">
                  <c:forEach items="${itemReviewPage.page.content}" var="review">
                    <paw:reviewCard review="${review}" showReviewer="false" anonymousLabel="${detailReviewAnonymousLabel}" />
                  </c:forEach>
                </div>
              </c:when>
              <c:otherwise>
                <p class="m-0 text-sm text-on-surface-variant"><c:out value="${itemReviewsEmpty}" /></p>
              </c:otherwise>
            </c:choose>

            <c:url var="reviewPreviousPageUrl" value="/item/${item.id}">
              <c:param name="page" value="${itemReviewPage.page.previousPage}" />
            </c:url>
            <c:url var="reviewNextPageUrl" value="/item/${item.id}">
              <c:param name="page" value="${itemReviewPage.page.nextPage}" />
            </c:url>
            <paw:pagination
                currentPage="${itemReviewPage.page.page}"
                totalPages="${itemReviewPage.page.totalPages}"
                hasPrevious="${itemReviewPage.page.hasPrevious}"
                hasNext="${itemReviewPage.page.hasNext}"
                previousPageUrl="${reviewPreviousPageUrl}#item-reviews"
                nextPageUrl="${reviewNextPageUrl}#item-reviews"
                navClass="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant" />
          </div>
        </jsp:body>
      </paw:sectionCard>
      </div>
    </section>

    <aside class="order-1 lg:order-2 w-full min-w-0 space-y-6">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-8 gap-4">
          <div class="flex items-start justify-between gap-4">
            <h1 class="text-3xl font-extrabold tracking-tight m-0 break-words">
              <c:out value="${version.title}" />
            </h1>
            <c:if test="${canFavouriteItem}">
              <div class="shrink-0 pt-1">
                <c:choose>
                  <c:when test="${viewer == null}">
                    <c:url var="favouriteLoginUrl" value="/login" />
                    <a href="${favouriteLoginUrl}"
                       class="btn btn-circle btn-sm shadow-sm bg-base-100/95 border-outline-variant/40 hover:bg-base-100"
                       aria-label="${favouriteLoginToAddLabel}"
                       title="${favouriteLoginToAddLabel}">
                      <span class="material-symbols-outlined text-lg icon-heart-outline">favorite</span>
                    </a>
                  </c:when>
                  <c:when test="${favouriteItem}">
                    <c:url var="unfavouriteItemUrl" value="/items/${item.id}/unfavourite" />
                    <form action="${unfavouriteItemUrl}" method="post" class="m-0">
                      <paw:itemDetailViewHiddenFields view="${itemDetailView}" />
                      <button
                          type="submit"
                          class="btn btn-circle btn-sm shadow-sm bg-base-100/95 border-outline-variant/40 hover:bg-base-100"
                          aria-label="${favouriteRemoveLabel}"
                          title="${favouriteRemoveLabel}">
                        <span class="material-symbols-outlined text-lg icon-heart-filled">favorite</span>
                      </button>
                    </form>
                  </c:when>
                  <c:otherwise>
                    <c:url var="favouriteItemUrl" value="/items/${item.id}/favourite" />
                    <form action="${favouriteItemUrl}" method="post" class="m-0">
                      <paw:itemDetailViewHiddenFields view="${itemDetailView}" />
                      <button
                          type="submit"
                          class="btn btn-circle btn-sm shadow-sm bg-base-100/95 border-outline-variant/40 hover:bg-base-100"
                          aria-label="${favouriteAddLabel}"
                          title="${favouriteAddLabel}">
                        <span class="material-symbols-outlined text-lg icon-heart-outline">favorite</span>
                      </button>
                    </form>
                  </c:otherwise>
                </c:choose>
              </div>
            </c:if>
          </div>
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

          <c:if test="${canReport}">
            <c:set var="reportModalId" value="report-item-modal-${item.id}" />
            <button type="button" class="btn btn-outline btn-error btn-sm w-full gap-2" onclick="document.getElementById('${reportModalId}').showModal()">
              <span class="material-symbols-outlined text-base">flag</span>
              <c:out value="${itemDetailReportButtonLabel}" />
            </button>
            <paw:reportModal itemId="${item.id}" modalId="${reportModalId}" />
          </c:if>
          <c:if test="${alreadyReported}">
            <p class="m-0 text-sm text-on-surface-variant italic">
              <c:out value="${itemDetailReportAlreadyReportedLabel}" />
            </p>
          </c:if>

          <c:if test="${showPreBookingPanel}">
            <c:choose>
              <c:when test="${isOwner}">
                <div class="space-y-4">
                  <paw:alertMessage type="info"><c:out value="${itemDetailOwnerNoticeLabel}" /></paw:alertMessage>
                  <c:url var="manageAvailabilityUrl" value="/my-boats/${item.id}/availability" />
                  <c:url var="incomingRequestsUrl" value="/requests/incoming" />
                  <paw:button
                      href="${manageAvailabilityUrl}"
                      color="primary"
                      icon="event_available"
                      text="${itemDetailOwnerBlockButtonLabel}"
                      fullWidth="true"
                      size="lg" />
                  <paw:button
                      href="${incomingRequestsUrl}"
                      color="primary"
                      variant="outline"
                      icon="inbox"
                      text="${itemDetailOwnerIncomingRequestsButtonLabel}"
                      fullWidth="true"
                      size="lg" />
                </div>
              </c:when>
              <c:otherwise>
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
                    data-price-pending="<c:out value='${itemDetailPricePendingLabel}'/>"
                    data-price-pending-help="<c:out value='${itemDetailPricePendingHelpLabel}'/>"
                    data-price-pick-end="<c:out value='${itemDetailPricePickEndLabel}'/>"
                    data-price-for-label="<c:out value='${itemDetailPriceForLabel}'/>"
                    data-price-hour-label="<c:out value='${itemDetailPriceHourLabel}'/>"
                    data-price-hours-label="<c:out value='${itemDetailPriceHoursLabel}'/>">
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
                  <paw:datePicker
                      id="detail-prebook-date"
                      dateFieldName="date"
                      label="${itemDetailDateLabel}"
                      value="${preBookingForm.date}"
                      placeholder="${itemDetailDatePlaceholder}"
                      restrictToAvailability="true"
                      offeredDates="${detailOfferedDates}"
                      occupiedDates="${detailOccupiedDates}"
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
                      offeredTimesByDate="${detailOfferedTimesByDate}"
                      occupiedTimesByDate="${detailOccupiedTimesByDate}" />
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
              </c:otherwise>
            </c:choose>
          </c:if>


        </div>
      </div>

      <paw:sectionCard icon="person">
        <jsp:attribute name="title"><spring:message code="itemDetail.contact.host" /></jsp:attribute>
        <jsp:body>
          <div class="flex min-w-0 flex-col gap-4">
            <c:if test="${itemOwner != null}">
              <c:url var="itemOwnerProfileUrl" value="/profiles/${itemOwner.id}/listings" />
            </c:if>
            <c:choose>
              <c:when test="${itemOwner != null}">
                <a href="${itemOwnerProfileUrl}" class="group flex min-w-0 items-center gap-4 no-underline rounded-xl transition-colors hover:bg-base-200/60 -m-2 p-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/25">
                  <div class="avatar placeholder shrink-0">
                    <div class="bg-primary/10 text-primary rounded-full w-14 h-14 flex items-center justify-center">
                      <span class="font-extrabold text-xl"><c:out value="${ownerInitials}" /></span>
                    </div>
                  </div>
                  <div class="min-w-0">
                    <h3 class="font-extrabold text-lg m-0 break-words text-on-background group-hover:underline">
                      <c:out value="${itemOwnerDisplayName}" />
                    </h3>
                    <div class="break-all text-xs text-on-surface-variant">
                      <c:out value="${itemOwner.email}" />
                    </div>
                  </div>
                </a>
              </c:when>
              <c:otherwise>
                <div class="flex min-w-0 items-center gap-4">
                  <div class="avatar placeholder shrink-0">
                    <div class="bg-primary/10 text-primary rounded-full w-14 h-14 flex items-center justify-center">
                      <span class="font-extrabold text-xl"><c:out value="${ownerInitials}" /></span>
                    </div>
                  </div>
                  <div class="min-w-0">
                    <h3 class="font-extrabold text-lg m-0 break-words">
                      <spring:message code="itemDetail.owner.none" />
                    </h3>
                    <div class="break-all text-xs text-on-surface-variant">
                      <spring:message code="itemDetail.owner.noEmail" />
                    </div>
                  </div>
                </div>
              </c:otherwise>
            </c:choose>
            <div class="flex w-full flex-col gap-2">
              <paw:button
                href="mailto:${itemOwner != null ? itemOwner.email : ''}"
                color="outline"
                icon="mail"
                cssClass="w-full sm:w-auto"
                text="${contactSendEmailLabel}"
              />
              <c:if test="${canSubscribeToOwner}">
                <c:choose>
                  <c:when test="${viewer == null}">
                    <c:url var="subscriptionLoginUrl" value="/login" />
                    <paw:button
                      href="${subscriptionLoginUrl}"
                      color="secondary"
                      variant="outline"
                      icon="notifications"
                      cssClass="w-full"
                      text="${subscriptionLoginToSubscribeLabel}"
                    />
                  </c:when>
                  <c:when test="${subscribedToOwner}">
                    <c:url var="unsubscribeOwnerUrl" value="/users/${itemOwner.id}/unsubscribe" />
                    <form action="${unsubscribeOwnerUrl}" method="post" class="m-0">
                      <paw:itemDetailViewHiddenFields view="${itemDetailView}" />
                      <paw:button
                        type="submit"
                        color="outline"
                        icon="notifications_off"
                        cssClass="w-full"
                        text="${subscriptionUnsubscribeLabel}"
                      />
                    </form>
                  </c:when>
                  <c:otherwise>
                    <c:url var="subscribeOwnerUrl" value="/users/${itemOwner.id}/subscribe" />
                    <form action="${subscribeOwnerUrl}" method="post" class="m-0">
                      <paw:itemDetailViewHiddenFields view="${itemDetailView}" />
                      <paw:button
                        type="submit"
                        color="secondary"
                        icon="notifications"
                        cssClass="w-full"
                        text="${subscriptionSubscribeLabel}"
                      />
                    </form>
                  </c:otherwise>
                </c:choose>
              </c:if>
            </div>
          </div>
        </jsp:body>
      </paw:sectionCard>

    </aside>
  </div>

  <dialog
    class="modal"
    data-item-unavailable-alert
    data-marketplace-url="${marketplaceUrl}"
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
