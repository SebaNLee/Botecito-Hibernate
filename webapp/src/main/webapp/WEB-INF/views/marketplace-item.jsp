<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="form" uri="http://www.springframework.org/tags/form" %> <%@ taglib
prefix="paw" tagdir="/WEB-INF/tags" %> <%@ taglib prefix="spring"
uri="http://www.springframework.org/tags" %> <%@ page contentType="text/html;
charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="publishUrl" value="/publish" />
<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="reservationRequestUrl" value="/item/${item.id}" />
<spring:message code="filters.date" var="dateLabel" />
<spring:message code="filters.date.placeholder" var="datePlaceholder" />
<spring:message code="filters.time" var="timeLabel" />
<spring:message code="filters.time.placeholder" var="timePlaceholder" />
<spring:message
  code="itemDetail.form.message.placeholder"
  var="requestMessagePlaceholder"
/>
<spring:message
  code="itemDetail.unavailable.mismatchPrefix"
  var="unavailableMismatchPrefix"
/>
<spring:message
  code="itemDetail.unavailable.mismatchSuffix"
  var="unavailableMismatchSuffix"
/>
<spring:message code="common.and" var="andLabel" />
<spring:message
  code="itemDetail.unavailable.reason.location"
  var="unavailableReasonLocation"
/>
<spring:message
  code="itemDetail.unavailable.reason.capacity"
  var="unavailableReasonCapacity"
/>
<spring:message
  code="itemDetail.unavailable.reason.weight"
  var="unavailableReasonWeight"
/>
<spring:message
  code="itemDetail.unavailable.reason.dateTime"
  var="unavailableReasonDateTime"
/>
<spring:message code="itemDetail.price.total" var="priceTotalLabel" />
<spring:message code="itemDetail.price.pending" var="pricePendingLabel" />
<spring:message
  code="itemDetail.price.pendingHelp"
  var="pricePendingHelpLabel"
/>
<spring:message
  code="itemDetail.price.pickEnd"
  var="pricePickEndLabel"
/>
<spring:message code="itemDetail.price.for" var="priceForLabel" />
<spring:message code="itemDetail.price.hour" var="priceHourLabel" />
<spring:message code="itemDetail.price.hours" var="priceHoursLabel" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-12 max-w-7xl mx-auto px-6 flex flex-col gap-8"
>
  <div class="w-full">
    <a
      href="${marketplaceUrl}"
      class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope mb-6 bg-transparent no-underline w-fit"
    >
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="flex flex-col lg:flex-row gap-8">
    <section class="flex-1 space-y-8">
      <div
        class="bg-surface-container-lowest rounded-2xl overflow-hidden shadow-[0_32px_48px_rgba(11,28,50,0.04)]"
      >
        <img
          class="w-full h-[400px] object-cover"
          alt="${item.title}"
          src="${itemImageUrl}"
        />
      </div>

      <div
        class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]"
      >
        <h2 class="text-2xl font-extrabold tracking-tight mb-4 m-0">
          <spring:message code="itemDetail.description.title" />
        </h2>
        <div class="space-y-4 text-on-surface-variant leading-relaxed">
          <p class="m-0"><c:out value="${item.description}" /></p>
        </div>

        <h3 class="text-xl font-extrabold tracking-tight mt-10 mb-6 m-0">
          <spring:message code="itemDetail.specs.title" />
        </h3>
        <ul class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary"
              >check_circle</span
            >
            <c:choose>
              <c:when test="${itemType != null}">
                <c:out value="${itemType.name}" />
              </c:when>
              <c:otherwise>
                <spring:message code="itemDetail.type.none" />
              </c:otherwise>
            </c:choose>
          </li>
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary"
              >check_circle</span
            >
            <spring:message
              code="itemDetail.capacity"
              arguments="${item.capacityPeople}"
            />
          </li>
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary"
              >check_circle</span
            >
            <spring:message
              code="itemDetail.weight"
              arguments="${item.maxWeightKg}"
            />
          </li>
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary"
              >check_circle</span
            >
            <spring:message
              code="itemDetail.difficulty"
              arguments="${item.difficultyLevel}"
            />
          </li>
        </ul>
      </div>
    </section>

    <aside class="w-full lg:w-[400px] space-y-6">
      <div
        class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]"
      >
        <span
          class="inline-block px-3 py-1 bg-surface-container-high text-primary text-xs font-bold uppercase tracking-wider rounded-full mb-4"
        >
          <c:choose>
            <c:when test="${item.difficultyLevel != null}">
              <spring:message
                code="marketplace.difficulty.level"
                arguments="${item.difficultyLevel}"
              />
            </c:when>
            <c:otherwise>
              <spring:message code="marketplace.difficulty.undefined" />
            </c:otherwise>
          </c:choose>
        </span>
        <h1 class="text-3xl font-extrabold tracking-tight mb-2 m-0">
          <c:out value="${item.title}" />
        </h1>
        <div
          class="flex items-center text-on-surface-variant text-sm gap-1 mb-6"
        >
          <span class="material-symbols-outlined text-primary text-lg"
            >location_on</span
          >
          <span><c:out value="${item.location}" /></span>
        </div>
        <div class="flex items-baseline gap-2 mb-8">
          <span class="text-4xl font-black text-primary"
            >$<c:out value="${item.pricePerHour}"
          /></span>
          <span class="text-xs font-bold uppercase tracking-wider text-outline"
            ><spring:message code="marketplace.card.perHour"
          /></span>
        </div>
        <div
          class="mb-8 rounded-2xl bg-surface-container-high px-4 py-4"
          data-reservation-price-summary
          data-price-per-hour="${item.pricePerHour}"
          data-currency-symbol="$"
          data-price-pending="${pricePendingLabel}"
          data-price-pending-help="${pricePendingHelpLabel}"
          data-price-pick-end="${pricePickEndLabel}"
          data-price-for-label="${priceForLabel}"
          data-price-hour-label="${priceHourLabel}"
          data-price-hours-label="${priceHoursLabel}"
        >
          <div class="flex items-baseline justify-between gap-3">
            <span class="text-[10px] font-bold uppercase tracking-wider text-outline">
              <c:out value="${priceTotalLabel}" />
            </span>
            <span class="text-2xl font-black text-primary" data-price-total>
              <c:out value="${pricePendingLabel}" />
            </span>
          </div>
          <p class="mb-0 mt-2 text-xs text-on-surface-variant" data-price-duration>
            <c:out value="${pricePendingHelpLabel}" />
          </p>
        </div>

        <form:form
          action="${reservationRequestUrl}"
          method="post"
          modelAttribute="reservationRequestForm"
          class="space-y-4"
          data-submit-loading-form="true"
        >
          <c:if test="${not empty mailSuccessCode}">
            <div
              class="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700"
            >
              <spring:message code="${mailSuccessCode}" />
            </div>
          </c:if>
          <c:if test="${not empty mailErrorCode}">
            <div
              class="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"
            >
              <spring:message code="${mailErrorCode}" />
            </div>
          </c:if>
          <paw:datePicker
            id="reservation-date"
            name="date"
            label="${dateLabel}"
            value="${reservationRequestForm.date}"
            placeholder="${datePlaceholder}"
            offeredDatesJson="${reservationOfferedDatesJson}"
            occupiedDatesJson="${reservationOccupiedDatesJson}"
          />
          <form:errors
            path="date"
            element="div"
            cssClass="text-sm text-error"
          />
          <paw:timeRangePicker
            id="reservation-time-range"
            dateInputId="reservation-date"
            startName="startTime"
            endName="endTime"
            label="${timeLabel}"
            startValue="${reservationRequestForm.startTime}"
            endValue="${reservationRequestForm.endTime}"
            placeholder="${timePlaceholder}"
            offeredTimesJson="${reservationOfferedTimesJson}"
            occupiedTimesJson="${reservationOccupiedTimesJson}"
          />
          <form:errors
            path="startTime"
            element="div"
            cssClass="text-sm text-error"
          />
          <form:errors
            path="endTime"
            element="div"
            cssClass="text-sm text-error"
          />
          <p class="m-0 text-xs text-on-surface-variant">
            <spring:message code="itemDetail.form.accountNote" />
          </p>
          <div class="space-y-2">
            <label
              class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant"
              for="requestMessage"
              ><spring:message code="itemDetail.form.message"
            /></label>
            <form:textarea
              path="requestMessage"
              id="requestMessage"
              rows="4"
              maxlength="1000"
              class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline resize-none"
              placeholder="${requestMessagePlaceholder}"
            />
            <form:errors
              path="requestMessage"
              element="div"
              cssClass="text-sm text-error"
            />
          </div>
          <button
            type="submit"
            class="relative mt-4 flex w-full items-center justify-center gap-2 rounded-xl border-none bg-primary py-4 font-bold text-on-primary shadow-lg shadow-primary/20 transition-all hover:bg-primary-container active:scale-[0.98] disabled:cursor-wait disabled:opacity-80"
            data-submit-loading-button
          >
            <span class="flex items-center justify-center gap-2" data-submit-loading-content>
              <spring:message code="itemDetail.form.submit" />
              <span class="material-symbols-outlined text-sm">chevron_right</span>
            </span>
            <span
              class="pointer-events-none absolute inset-0 hidden items-center justify-center"
              aria-hidden="true"
              data-submit-loading-spinner
            >
              <span class="h-5 w-5 animate-spin rounded-full border-2 border-current border-t-transparent"></span>
            </span>
          </button>
        </form:form>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div
          class="bg-surface-container-lowest rounded-2xl p-6 shadow-[0_32px_48px_rgba(11,28,50,0.04)] flex flex-col items-center justify-center text-center gap-2"
        >
          <span class="material-symbols-outlined text-outline text-3xl"
            >groups</span
          >
          <div>
            <div
              class="text-[10px] font-bold uppercase tracking-wider text-outline"
            >
              <spring:message code="itemDetail.capacity.label" />
            </div>
            <div class="font-extrabold text-lg">
              <spring:message
                code="marketplace.card.people"
                arguments="${item.capacityPeople}"
              />
            </div>
          </div>
        </div>
        <div
          class="bg-surface-container-lowest rounded-2xl p-6 shadow-[0_32px_48px_rgba(11,28,50,0.04)] flex flex-col items-center justify-center text-center gap-2"
        >
          <span class="material-symbols-outlined text-outline text-3xl"
            >weight</span
          >
          <div>
            <div
              class="text-[10px] font-bold uppercase tracking-wider text-outline"
            >
              <spring:message code="itemDetail.weight.short" />
            </div>
            <div class="font-extrabold text-lg">
              <spring:message
                code="marketplace.card.weight"
                arguments="${item.maxWeightKg}"
              />
            </div>
          </div>
        </div>
      </div>

      <div
        class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]"
      >
        <div class="flex items-center gap-4 mb-6">
          <div
            class="w-14 h-14 rounded-full bg-primary/10 text-primary flex items-center justify-center font-extrabold text-xl"
          >
            <c:out value="${ownerInitial}" />
          </div>
          <div>
            <h3 class="font-extrabold text-lg m-0">
              <c:choose>
                <c:when test="${itemOwner != null}">
                  <c:out value="${itemOwner.name}" />
                </c:when>
                <c:otherwise>
                  <spring:message code="itemDetail.owner.none" />
                </c:otherwise>
              </c:choose>
            </h3>
            <div class="text-xs text-on-surface-variant">
              <c:choose>
                <c:when test="${itemOwner != null}">
                  <c:out value="${itemOwner.email}" />
                </c:when>
                <c:otherwise>
                  <spring:message code="itemDetail.owner.noEmail" />
                </c:otherwise>
              </c:choose>
            </div>
          </div>
        </div>
        <div class="space-y-3">
          <a
            href="mailto:${itemOwner != null ? itemOwner.email : ''}"
            class="w-full py-3 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors flex justify-center items-center gap-2 no-underline"
          >
            <span class="material-symbols-outlined text-sm">mail</span>
            <spring:message code="itemDetail.contact.sendEmail" />
          </a>
        </div>
      </div>
    </aside>
  </div>

  <div
    class="fixed inset-0 z-[280] hidden items-center justify-center bg-on-background/45 px-6"
    data-item-unavailable-alert
    data-marketplace-url="${marketplaceUrl}"
    data-item-location-option-id="${item.locationOptionId}"
    data-item-capacity="${item.capacityPeople}"
    data-item-max-weight="${item.maxWeightKg}"
    data-mismatch-prefix="${unavailableMismatchPrefix}"
    data-mismatch-suffix="${unavailableMismatchSuffix}"
    data-mismatch-join="${andLabel}"
    data-mismatch-location="${unavailableReasonLocation}"
    data-mismatch-capacity="${unavailableReasonCapacity}"
    data-mismatch-weight="${unavailableReasonWeight}"
    data-mismatch-date-time="${unavailableReasonDateTime}"
    hidden
  >
    <div
      class="w-full max-w-lg rounded-3xl bg-surface-container-lowest p-8 shadow-[0_32px_64px_rgba(11,28,50,0.18)]"
    >
      <div class="flex items-start gap-4">
        <div
          class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-error-container text-error"
        >
          <span class="material-symbols-outlined">warning</span>
        </div>
        <div class="space-y-3">
          <h2 class="m-0 text-2xl font-extrabold tracking-tight">
            <spring:message code="itemDetail.unavailable.title" />
          </h2>
          <p
            class="m-0 leading-relaxed text-on-surface-variant"
            data-item-unavailable-message
          >
            <spring:message code="itemDetail.unavailable.message" />
          </p>
        </div>
      </div>

      <div class="mt-8 flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          class="flex-1 rounded-xl border-none bg-primary px-5 py-3 font-bold text-on-primary cursor-pointer"
          data-item-unavailable-clear
        >
          <spring:message code="itemDetail.unavailable.clear" />
        </button>
        <button
          type="button"
          class="flex-1 rounded-xl border border-outline-variant bg-transparent px-5 py-3 font-bold text-on-surface cursor-pointer"
          data-item-unavailable-marketplace
        >
          <spring:message code="itemDetail.unavailable.backToMarketplace" />
        </button>
      </div>
    </div>
  </div>
</paw:layout>
