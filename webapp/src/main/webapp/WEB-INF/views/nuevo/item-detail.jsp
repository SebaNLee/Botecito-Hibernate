<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%-- Nuevo item detail: same layout as marketplace-item.jsp; model attribute "item" is ar.edu.itba.paw.models.nuevo.ItemModel --%>

<fmt:setLocale value="es_AR" />
<c:url var="publishUrl" value="/publish" />
<c:url var="currentVersionUrl" value="/item/${item.id}" />
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
<spring:message code="itemDetail.owner.ownPublicationNotice" var="ownerPublicationNotice" />
<spring:message code="itemDetail.owner.blockButton" var="ownerBlockButtonLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-12 w-full max-w-7xl mx-auto px-6 flex flex-col gap-8">
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
      <paw:imageCarousel imageUrls="${itemImageUrls}" altText="${item.title}" />

      <paw:sectionCard>
        <jsp:attribute name="title"><spring:message code="itemDetail.description.title" /></jsp:attribute>
        <jsp:body>
          <c:choose>
            <c:when test="${not empty item.description}">
              <p class="m-0 break-words text-on-surface-variant leading-relaxed">
                <c:out value="${item.description}" />
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
                  <c:when test="${not empty item.itemTypeName}"><c:out value="${item.itemTypeName}" /></c:when>
                  <c:otherwise><spring:message code="itemDetail.type.none" /></c:otherwise>
                </c:choose>
              </span>
            </li>
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="itemDetail.capacity.label" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <spring:message code="itemDetail.capacity" arguments="${item.capacity}" />
              </span>
            </li>
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="itemDetail.weight.short" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <spring:message code="itemDetail.weight" arguments="${item.weight}" />
              </span>
            </li>
            <li class="rounded-2xl bg-base-200 p-4 min-w-0">
              <span class="mb-2 block text-[10px] font-bold uppercase tracking-wider text-outline"><spring:message code="publish.form.difficulty.label" /></span>
              <span class="flex items-center gap-3 break-words font-bold text-on-surface">
                <span class="material-symbols-outlined text-primary">check_circle</span>
                <spring:message code="itemDetail.difficulty" arguments="${item.difficulty}" />
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
                  <span class="font-extrabold text-xl"><c:out value="${ownerInitial}" /></span>
                </div>
              </div>
              <div class="min-w-0">
                <h3 class="font-extrabold text-lg m-0 break-words">
                  <c:choose>
                    <c:when test="${itemOwner != null}"><c:out value="${itemOwner.name}" /></c:when>
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
                  <c:when test="${item.totalReviews > 0}">
                    <fmt:formatNumber value="${item.averageRating}" minFractionDigits="1" maxFractionDigits="1" />
                  </c:when>
                  <c:otherwise><c:out value="${reviewsEmptyShortLabel}" /></c:otherwise>
                </c:choose>
              </div>
              <p class="m-0 text-xs text-on-surface-variant">
                <spring:message code="itemDetail.reviews.count" arguments="${item.totalReviews}" />
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
              <c:when test="${not empty itemReviews}">
                <div class="space-y-3">
                  <c:forEach items="${itemReviews}" var="review">
                    <div class="rounded-xl bg-base-200 p-4 space-y-2">
                      <div class="flex items-center justify-between gap-3">
                        <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${reviewAuthorNames[review.reviewerUserId]}" /></p>
                        <div class="flex items-center gap-0.5 shrink-0" aria-label="${review.rating} of 5">
                          <c:forEach var="starIndex" begin="1" end="5">
                            <c:choose>
                              <c:when test="${starIndex <= review.rating}">
                                <span class="material-symbols-outlined text-sm leading-none text-warning">star</span>
                              </c:when>
                              <c:otherwise>
                                <span class="material-symbols-outlined text-sm leading-none text-outline opacity-[0.35]">star</span>
                              </c:otherwise>
                            </c:choose>
                          </c:forEach>
                        </div>
                      </div>
                      <p class="m-0 text-xs text-on-surface-variant">
                        <time datetime="${review.createdAt}"><c:out value="${reviewCreatedAtLabels[review.id]}" /></time>
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
          </div>
        </jsp:body>
      </paw:sectionCard>
    </section>

    <aside class="order-1 lg:order-2 w-full min-w-0 lg:sticky lg:top-24 space-y-6">
      <div class="card bg-base-100 shadow-sm">
        <div class="card-body p-8 gap-4">
          <c:choose>
            <c:when test="${hideListingLiveVersionNavigation}">
              <paw:alertMessage type="info"><spring:message code="itemDetail.listingInactive.guestBookedSnapshotOnly" /></paw:alertMessage>
            </c:when>
            <c:when test="${showVersionSelector}">
              <div class="rounded-2xl bg-base-200/70 p-5 space-y-4">
                <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline">
                  <spring:message code="itemDetail.version.title" />
                </p>
                <c:choose>
                  <c:when test="${viewingNonCurrentVersion}">
                    <p class="m-0 text-xs text-on-surface-variant"><spring:message code="itemDetail.version.snapshotNotice" /></p>
                  </c:when>
                  <c:otherwise>
                    <p class="m-0 text-xs text-on-surface-variant"><spring:message code="itemDetail.version.currentNotice" /></p>
                  </c:otherwise>
                </c:choose>
                <div class="flex flex-wrap items-center gap-4 pt-2">
                  <a href="${currentVersionUrl}" class="btn ${!viewingNonCurrentVersion ? 'btn-primary' : 'btn-outline'} btn-xs no-underline">
                    <spring:message code="itemDetail.version.seeCurrent" />
                  </a>
                  <div class="dropdown dropdown-end">
                    <button type="button" tabindex="0" class="btn btn-outline btn-xs">
                      <spring:message code="itemDetail.version.seeOlder" />
                    </button>
                    <ul tabindex="0" class="dropdown-content menu p-3 space-y-2 shadow bg-base-100 rounded-box w-64 max-h-56 overflow-auto border border-outline-variant/20">
                      <c:forEach items="${itemDetail.versions}" var="ver">
                        <c:if test="${ver.versionId ne currentVersionId}">
                          <c:url var="snapshotUrl" value="/item/${item.id}/snapshot/${ver.versionId}" />
                          <li>
                            <a href="${snapshotUrl}" class="${ver.versionId eq selectedVersionId ? 'active' : ''}">
                              <spring:message code="itemDetail.version.guestSnapshot" arguments="${ver.versionId}" />
                            </a>
                          </li>
                        </c:if>
                      </c:forEach>
                    </ul>
                  </div>
                </div>
              </div>
            </c:when>
          </c:choose>

          <div class="flex flex-wrap gap-2">
            <span class="badge badge-primary badge-outline"><spring:message code="detail.version.badge" /></span>
          </div>

          <h1 class="text-3xl font-extrabold tracking-tight m-0 break-words">
            <c:out value="${item.title}" />
          </h1>
          <div class="flex min-w-0 items-center text-on-surface-variant text-sm gap-1">
            <span class="material-symbols-outlined text-primary text-lg">location_on</span>
            <span class="min-w-0 break-words"><c:out value="${item.location}" /></span>
          </div>
          <div class="flex items-baseline gap-2">
            <span class="text-4xl font-black text-primary whitespace-nowrap">
              $<fmt:formatNumber value="${item.price}" type="number" groupingUsed="true" maxFractionDigits="0" />
            </span>
            <span class="text-xs font-bold uppercase tracking-wider text-outline"><spring:message code="marketplace.card.perHour" /></span>
          </div>

          <c:choose>
            <c:when test="${viewingNonCurrentVersion}">
              <paw:alertMessage type="info"><spring:message code="itemDetail.version.snapshotBookingDisabled" /></paw:alertMessage>
            </c:when>
            <c:when test="${isOwner}">
              <paw:alertMessage type="info"><c:out value="${ownerPublicationNotice}" /></paw:alertMessage>
              <c:url var="ownerManageAvailabilityUrl" value="/profile/item/${item.id}/availability">
                <c:param name="return" value="/item/${item.id}" />
              </c:url>
              <a href="${ownerManageAvailabilityUrl}" class="btn btn-primary btn-block btn-lg no-underline">
                <c:out value="${ownerBlockButtonLabel}" />
                <span class="material-symbols-outlined text-sm align-middle">event_busy</span>
              </a>
            </c:when>
            <c:otherwise>
              <%-- Guest reservation POST is handled outside this GET-only flow. --%>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </aside>
  </div>

  <dialog
    class="modal"
    data-item-unavailable-alert
    data-marketplace-url="<c:out value="${marketplaceBackHref}" />"
    data-item-location-option-id="${item.locationId}"
    data-item-location-slug="${itemLocationSlug}"
    data-item-capacity="${item.capacity}"
    data-item-max-weight="${item.weight}"
    data-item-difficulty-level="${item.difficulty}"
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
