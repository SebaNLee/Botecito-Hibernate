<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="profileUrl" value="/my-boats" />
<c:url var="editActionUrl" value="/profile/item/${item.id}/edit" />
<c:url var="placeholderImageUrl" value="/css/boat-placeholder.svg" />
<spring:message code="publish.form.title.label" var="publishTitleLabel" />
<spring:message code="publish.form.title.placeholder" var="publishTitlePlaceholder" />
<spring:message code="publish.form.description.label" var="publishDescriptionLabel" />
<spring:message code="publish.form.description.placeholder" var="publishDescriptionPlaceholder" />
<spring:message code="publish.form.price.label" var="publishPriceLabel" />
<spring:message code="publish.form.price.placeholder" var="publishPricePlaceholder" />
<spring:message code="publish.form.difficulty.label" var="publishDifficultyLabel" />
<spring:message code="publish.form.difficulty.placeholder" var="publishDifficultyPlaceholder" />
<spring:message code="publish.difficulty.1" var="publishDifficulty1" />
<spring:message code="publish.difficulty.2" var="publishDifficulty2" />
<spring:message code="publish.difficulty.3" var="publishDifficulty3" />
<spring:message code="publish.difficulty.4" var="publishDifficulty4" />
<spring:message code="publish.difficulty.5" var="publishDifficulty5" />
<spring:message code="publish.form.location.label" var="publishLocationLabel" />
<spring:message code="publish.form.location.placeholder" var="publishLocationPlaceholder" />
<spring:message code="editPublication.actions.save" var="saveLabel" />
<spring:message code="editPublication.actions.cancel" var="cancelLabel" />
<spring:message code="editPublication.conflict.confirmChanges" var="confirmChangesLabel" />
<spring:message code="editPublication.conflict.discard" var="discardEditLabel" />

<paw:layout
    title="Botecito"
    mainClass="pt-24 pb-14 max-w-3xl mx-auto px-6"
    headerCtaMessageCode="nav.rent"
    headerCtaHref="/marketplace"
    headerCtaVariant="rent">
  <div class="mb-8">
    <a href="${profileUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-8">
    <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">
      <spring:message code="editPublication.title" />
    </h1>
    <p class="text-on-surface-variant mt-2 m-0 text-lg">
      <c:out value="${item.title}" />
    </p>
  </div>

  <form:form
      action="${editActionUrl}"
      method="post"
      modelAttribute="editForm"
      enctype="multipart/form-data"
      cssClass="space-y-8">

    <spring:hasBindErrors name="editForm">
      <c:if test="${not empty errors.globalErrors}">
        <c:forEach var="error" items="${errors.globalErrors}">
          <spring:message
              code="${error.code}"
              text="${not empty error.defaultMessage ? error.defaultMessage : error.code}"
              var="resolvedGlobalError" />
          <paw:alertMessage type="error" message="${resolvedGlobalError}" />
        </c:forEach>
      </c:if>
    </spring:hasBindErrors>

    <paw:sectionCard icon="edit">
      <jsp:attribute name="title"><spring:message code="editPublication.title" /></jsp:attribute>
      <jsp:body>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div class="md:col-span-2">
            <paw:formField path="title" label="${publishTitleLabel}" placeholder="${publishTitlePlaceholder}" />
          </div>
          <div class="md:col-span-2">
            <paw:textareaField path="description" label="${publishDescriptionLabel}" placeholder="${publishDescriptionPlaceholder}" rows="5" maxlength="1000" />
          </div>
          <paw:formField path="pricePerHour" type="number" label="${publishPriceLabel}" placeholder="${publishPricePlaceholder}" />
          <fieldset class="fieldset">
            <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="difficultyLevel">
              <c:out value="${publishDifficultyLabel}" />
            </legend>
            <form:select path="difficultyLevel" id="difficultyLevel" cssClass="select w-full" cssErrorClass="select w-full select-error">
              <form:option value="" label="${publishDifficultyPlaceholder}" />
              <form:option value="1" label="${publishDifficulty1}" />
              <form:option value="2" label="${publishDifficulty2}" />
              <form:option value="3" label="${publishDifficulty3}" />
              <form:option value="4" label="${publishDifficulty4}" />
              <form:option value="5" label="${publishDifficulty5}" />
            </form:select>
            <form:errors path="difficultyLevel" cssClass="text-error text-xs mt-1" element="p" />
          </fieldset>
          <div class="md:col-span-2">
            <paw:locationPicker
                id="marina"
                name="marina"
                label="${publishLocationLabel}"
                value="${editForm.marina}"
                placeholder="${publishLocationPlaceholder}"
                icon="location_on"
                errorPath="marina" />
          </div>
        </div>
      </jsp:body>
    </paw:sectionCard>

    <paw:sectionCard icon="add_a_photo">
      <jsp:attribute name="title"><spring:message code="publish.step1.section.photo" /></jsp:attribute>
      <jsp:body>
        <div class="rounded-xl overflow-hidden border border-outline-variant/30 bg-base-200">
          <img
              id="edit-file-preview"
              src="${not empty itemImageUrl ? itemImageUrl : placeholderImageUrl}"
              alt="<spring:message code='publish.image.previewAlt' />"
              class="w-full aspect-[16/10] object-cover" />
        </div>

        <label class="border-2 border-dashed border-outline-variant rounded-xl p-10 flex flex-col items-center justify-center text-center hover:bg-base-200/60 transition-colors cursor-pointer group">
          <input
              type="file"
              name="file"
              accept="image/*"
              class="hidden"
              data-image-preview-input
              data-image-preview-target-id="edit-file-preview"
              data-image-preview-filename-id="edit-file-name-display"
              data-image-preview-placeholder="${not empty itemImageUrl ? itemImageUrl : placeholderImageUrl}" />
          <div class="w-16 h-16 bg-primary/10 text-primary rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-3xl">upload_file</span>
          </div>
          <span class="font-bold text-lg text-primary"><spring:message code="publish.image.upload" /></span>
          <span id="edit-file-name-display" class="text-sm text-outline mt-1"><spring:message code="publish.image.helper" /></span>
          <form:errors path="file" cssClass="text-error text-xs mt-2" element="p" />
        </label>

        <div class="rounded-xl bg-base-200 p-4 text-sm text-on-surface-variant leading-relaxed">
          <spring:message code="publish.image.note" />
        </div>
      </jsp:body>
    </paw:sectionCard>

    <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-2">
      <paw:button href="${profileUrl}" color="ghost" size="lg" cssClass="w-full sm:w-auto" text="${cancelLabel}" />
      <c:choose>
        <c:when test="${not empty activeEditBookings}">
          <button type="button" class="btn btn-primary btn-lg w-full sm:w-auto" data-edit-conflict-open>
            <c:out value="${saveLabel}" />
          </button>
        </c:when>
        <c:otherwise>
          <paw:button type="submit" color="primary" size="lg" cssClass="w-full sm:w-auto" text="${saveLabel}" />
        </c:otherwise>
      </c:choose>
    </div>

    <c:if test="${not empty activeEditBookings}">
      <dialog class="modal" data-edit-conflict-modal ${showEditConflictModal ? 'open="open"' : ''}>
        <div class="modal-box max-w-3xl p-0 bg-transparent shadow-none">
          <div class="card bg-base-100 shadow-xl">
            <div class="card-body p-8 gap-5">
              <div class="flex items-start gap-4">
                <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-warning/15 text-warning">
                  <span class="material-symbols-outlined">warning</span>
                </div>
                <div class="space-y-2">
                  <h2 class="card-title m-0 text-2xl font-extrabold tracking-tight">
                    <spring:message code="editPublication.conflict.title" />
                  </h2>
                  <p class="m-0 leading-relaxed text-on-surface-variant">
                    <spring:message code="editPublication.conflict.message" />
                  </p>
                </div>
              </div>

              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <div class="flex items-center gap-2">
                    <button type="button" class="btn btn-ghost btn-sm btn-square" data-edit-conflict-prev aria-label="Previous booking">
                      <span class="material-symbols-outlined">chevron_left</span>
                    </button>
                    <span class="text-sm font-bold min-w-14 text-center" data-edit-conflict-counter>1/1</span>
                    <button type="button" class="btn btn-ghost btn-sm btn-square" data-edit-conflict-next aria-label="Next booking">
                      <span class="material-symbols-outlined">chevron_right</span>
                    </button>
                  </div>
                </div>

                <c:forEach items="${activeEditBookings}" var="booking" varStatus="bookingStatus">
                  <div class="card border border-outline-variant/30 bg-base-100 shadow-md ${bookingStatus.first ? '' : 'hidden'}" data-edit-booking-card data-edit-booking-index="${bookingStatus.index}">
                    <div class="card-body gap-4 p-4 sm:p-5">
                      <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                        <div class="flex min-w-0 items-start gap-3">
                          <div class="mt-0.5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/15 text-primary">
                            <span class="material-symbols-outlined text-base">event</span>
                          </div>
                          <div class="min-w-0 grid grid-cols-1 gap-1.5">
                            <p class="m-0 text-sm font-semibold text-on-surface break-words">
                              <c:out value="${editBookingGuests[booking.id]}" />
                            </p>
                            <p class="m-0 text-xs text-on-surface-variant break-words">
                              <c:out value="${editBookingFriendlyDates[booking.id]}" />
                            </p>
                            <p class="m-0 text-xs text-on-surface-variant">
                              from <c:out value="${editBookingFriendlyTimeRanges[booking.id]}" />
                            </p>
                            <p class="m-0 text-sm font-bold text-success">
                              $<c:out value="${editBookingFriendlyPrices[booking.id]}" />
                            </p>
                          </div>
                        </div>
                        <div class="sm:pl-2">
                          <span class="badge badge-outline font-semibold">
                            <spring:message code="${editBookingStatusCodes[booking.id]}" />
                          </span>
                        </div>
                      </div>

                      <c:if test="${booking.state == 'BOOKING_PENDING'}">
                        <div class="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
                          <label class="label cursor-pointer justify-start gap-3 rounded-xl border border-success/25 bg-success/10 px-3 py-2.5">
                            <input type="radio" class="radio radio-success radio-sm" name="bookingDecision_${booking.id}" value="accept" />
                            <span class="label-text font-semibold text-success-content"><spring:message code="editPublication.conflict.pending.accept" /></span>
                          </label>
                          <label class="label cursor-pointer justify-start gap-3 rounded-xl border border-error/25 bg-error/10 px-3 py-2.5">
                            <input type="radio" class="radio radio-error radio-sm" name="bookingDecision_${booking.id}" value="decline" />
                            <span class="label-text font-semibold text-error-content"><spring:message code="editPublication.conflict.pending.decline" /></span>
                          </label>
                        </div>
                      </c:if>
                    </div>
                  </div>
                </c:forEach>
              </div>

              <div class="rounded-xl bg-info/10 p-4 text-sm text-on-surface-variant">
                <spring:message code="editPublication.conflict.snapshotNotice" />
              </div>

              <div class="card-actions mt-2 flex flex-col gap-3 sm:flex-row">
                <a href="${profileUrl}" class="btn btn-outline flex-1 no-underline">
                  <c:out value="${discardEditLabel}" />
                </a>
                <button type="submit" name="confirmEditWithSnapshots" value="true" class="btn btn-primary flex-1">
                  <c:out value="${confirmChangesLabel}" />
                </button>
              </div>
            </div>
          </div>
        </div>
        <button type="button" class="modal-backdrop" aria-label="${discardEditLabel}" data-edit-conflict-close>close</button>
      </dialog>
    </c:if>
  </form:form>
  <script>
    document.addEventListener("DOMContentLoaded", function () {
      const openButton = document.querySelector("[data-edit-conflict-open]");
      const modal = document.querySelector("[data-edit-conflict-modal]");
      const bookingCards = Array.from(document.querySelectorAll("[data-edit-booking-card]"));
      const counter = document.querySelector("[data-edit-conflict-counter]");
      const prevButton = document.querySelector("[data-edit-conflict-prev]");
      const nextButton = document.querySelector("[data-edit-conflict-next]");
      let activeIndex = 0;

      function renderBookingCarousel() {
        if (!bookingCards.length) {
          if (prevButton) {
            prevButton.disabled = true;
          }
          if (nextButton) {
            nextButton.disabled = true;
          }
          return;
        }
        bookingCards.forEach(function (card, index) {
          card.classList.toggle("hidden", index !== activeIndex);
        });
        if (counter) {
          counter.textContent = (activeIndex + 1) + "/" + bookingCards.length;
        }
        if (prevButton) {
          prevButton.disabled = activeIndex <= 0;
        }
        if (nextButton) {
          nextButton.disabled = activeIndex >= bookingCards.length - 1;
        }
      }

      function moveBooking(step) {
        if (!bookingCards.length) {
          return;
        }
        const nextIndex = activeIndex + step;
        if (nextIndex < 0 || nextIndex >= bookingCards.length) {
          return;
        }
        activeIndex = nextIndex;
        renderBookingCarousel();
      }

      renderBookingCarousel();
      if (openButton && modal) {
        openButton.addEventListener("click", function () {
          if (typeof modal.showModal === "function") {
            modal.showModal();
          } else {
            modal.setAttribute("open", "open");
          }
        });
      }
      if (prevButton) {
        prevButton.addEventListener("click", function () {
          moveBooking(-1);
        });
      }
      if (nextButton) {
        nextButton.addEventListener("click", function () {
          moveBooking(1);
        });
      }
      document.querySelectorAll("[data-edit-conflict-close]").forEach(function (button) {
        button.addEventListener("click", function () {
          if (modal && typeof modal.close === "function") {
            modal.close();
          } else if (modal) {
            modal.removeAttribute("open");
          }
        });
      });
      document.addEventListener("keydown", function (event) {
        if (!modal || !modal.hasAttribute("open")) {
          return;
        }
        if (event.key === "ArrowLeft") {
          moveBooking(-1);
        } else if (event.key === "ArrowRight") {
          moveBooking(1);
        }
      });
    });
  </script>
</paw:layout>
