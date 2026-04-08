<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="stepTwoUrl" value="/publish/availability" />
<c:url var="stepThreeUrl" value="/publish/contact" />
<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="placeholderImageUrl" value="/css/boat-placeholder.svg" />
<c:url var="publishPreviewImageUrl" value="/publish/preview-image" />
<spring:message code="publish.form.firstName.placeholder" var="ownerFirstNamePlaceholder" />
<spring:message code="publish.form.lastName.placeholder" var="ownerLastNamePlaceholder" />
<spring:message code="publish.form.email.placeholder" var="ownerEmailPlaceholder" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">
  <div class="mb-8">
    <a href="${stepTwoUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope bg-transparent no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span><spring:message code="publish.step3.progress" /></span>
      <span><spring:message code="publish.step3.badge" /></span>
    </div>
    <div class="mt-3 h-2 w-full rounded-full bg-surface-container-high overflow-hidden">
      <div class="h-full w-full rounded-full bg-primary"></div>
    </div>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"><spring:message code="publish.step3.title" /></h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0"><spring:message code="publish.step3.subtitle" /></p>
  </div>

  <form:form
      action="${stepThreeUrl}"
      method="post"
      modelAttribute="publishForm"
      class="space-y-8"
      data-submit-loading-form="true">

    <spring:hasBindErrors name="publishForm">
      <c:if test="${not empty errors.globalErrors}">
        <c:forEach var="error" items="${errors.globalErrors}">
          <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700 flex items-start gap-3">
            <span class="material-symbols-outlined text-red-500 text-lg mt-0.5">error</span>
            <span><c:out value="${error.defaultMessage}" /></span>
          </div>
        </c:forEach>
      </c:if>
    </spring:hasBindErrors>

    <div class="grid grid-cols-1 lg:grid-cols-5 gap-8 items-start">
      <aside class="lg:col-span-2 bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
        <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/20">
          <span class="material-symbols-outlined text-primary text-2xl">inventory_2</span>
          <h2 class="text-xl font-extrabold m-0"><spring:message code="publish.step3.summary.title" /></h2>
        </div>

        <div class="rounded-xl overflow-hidden border border-outline-variant/30 bg-surface-container-high">
          <c:choose>
            <c:when test="${not empty uploadedImagePreviewUrl}">
              <img
                src="${publishPreviewImageUrl}"
                alt="<spring:message code='publish.image.previewAlt' />"
                class="w-full aspect-[16/10] object-cover" />
            </c:when>
            <c:otherwise>
              <img
                src="${placeholderImageUrl}"
                alt="<spring:message code='publish.image.defaultAlt' />"
                class="w-full aspect-[16/10] object-cover" />
            </c:otherwise>
          </c:choose>
        </div>

        <div>
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-1"><spring:message code="publish.form.title.label" /></p>
          <p class="text-lg font-extrabold text-on-surface m-0"><c:out value="${publishForm.title}" /></p>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div class="rounded-xl bg-surface-container-high p-3">
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.type.label" /></p>
            <p class="text-sm font-bold text-on-surface mt-1 mb-0">
              <c:choose>
                <c:when test="${publishForm.itemTypeId == '1'}"><spring:message code="publish.type.other" /></c:when>
                <c:when test="${publishForm.itemTypeId == '2'}"><spring:message code="publish.type.kayak" /></c:when>
                <c:when test="${publishForm.itemTypeId == '3'}"><spring:message code="publish.type.paddle" /></c:when>
                <c:when test="${publishForm.itemTypeId == '4'}"><spring:message code="publish.type.canoe" /></c:when>
                <c:when test="${publishForm.itemTypeId == '5'}"><spring:message code="publish.type.windsurf" /></c:when>
                <c:when test="${publishForm.itemTypeId == '6'}"><spring:message code="publish.type.efoil" /></c:when>
                <c:when test="${publishForm.itemTypeId == '7'}"><spring:message code="publish.type.optimist" /></c:when>
                <c:otherwise><spring:message code="publish.type.none" /></c:otherwise>
              </c:choose>
            </p>
          </div>
          <div class="rounded-xl bg-surface-container-high p-3">
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.capacity.label" /></p>
            <p class="text-sm font-bold text-on-surface mt-1 mb-0"><spring:message code="publish.capacity.people" arguments="${publishForm.capacity}" /></p>
          </div>
          <div class="rounded-xl bg-surface-container-high p-3">
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.price.short" /></p>
            <p class="text-sm font-bold text-on-surface mt-1 mb-0">$ <c:out value="${publishForm.pricePerHour}" /></p>
          </div>
          <div class="rounded-xl bg-surface-container-high p-3">
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.location.label" /></p>
            <p class="text-sm font-bold text-on-surface mt-1 mb-0"><c:out value="${publishForm.marina}" /></p>
          </div>
          <div class="rounded-xl bg-surface-container-high p-3 col-span-2">
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.maxWeight.label" /></p>
            <p class="text-sm font-bold text-on-surface mt-1 mb-0">
              <c:choose>
                <c:when test="${not empty publishForm.maxWeight}">
                  <c:out value="${publishForm.maxWeight}" /> kg
                </c:when>
                <c:otherwise>
                  <spring:message code="publish.form.maxWeight.unspecified" />
                </c:otherwise>
              </c:choose>
            </p>
          </div>
        </div>

        <div>
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-2"><spring:message code="publish.step3.summary.availability" /></p>
          <form:errors path="availabilityByWeekday" cssClass="mb-2 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          <c:choose>
            <c:when test="${not empty availabilitySummary}">
              <ul class="space-y-2 m-0 p-0 list-none">
                <c:forEach var="slot" items="${availabilitySummary}">
                  <li class="rounded-xl bg-surface-container-high px-3 py-2 text-sm text-on-surface font-medium">
                    <c:out value="${slot}" />
                  </li>
                </c:forEach>
              </ul>
            </c:when>
            <c:otherwise>
              <p class="text-sm text-error m-0"><spring:message code="publish.step3.summary.noAvailability" /></p>
            </c:otherwise>
          </c:choose>
        </div>
      </aside>

      <section class="lg:col-span-3 bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
        <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/20">
          <span class="material-symbols-outlined text-primary text-2xl">person</span>
          <h2 class="text-xl font-extrabold m-0"><spring:message code="publish.step3.contact.title" /></h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="ownerFirstName"><spring:message code="publish.form.firstName.label" /></label>
            <form:input path="ownerFirstName" id="ownerFirstName" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="${ownerFirstNamePlaceholder}"/>
            <form:errors path="ownerFirstName" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="ownerLastName"><spring:message code="publish.form.lastName.label" /></label>
            <form:input path="ownerLastName" id="ownerLastName" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="${ownerLastNamePlaceholder}"/>
            <form:errors path="ownerLastName" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="ownerEmail"><spring:message code="publish.form.email.label" /></label>
            <form:input path="ownerEmail" id="ownerEmail" type="email" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="${ownerEmailPlaceholder}"/>
            <form:errors path="ownerEmail" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

        </div>

        <div class="rounded-xl bg-surface-container-high p-4 text-sm text-on-surface-variant leading-relaxed">
          <spring:message code="publish.step3.note" />
        </div>

        <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
          <a href="${marketplaceUrl}" class="w-full sm:w-auto px-8 py-4 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors no-underline text-center">
            <spring:message code="publish.actions.saveDraft" />
          </a>
          <button
            type="submit"
            class="relative flex w-full items-center justify-center gap-2 rounded-xl border-none bg-primary px-10 py-4 font-bold text-on-primary shadow-lg shadow-primary/20 transition-all hover:bg-primary-container active:scale-[0.98] disabled:cursor-wait disabled:opacity-80 sm:w-auto"
            data-submit-loading-button
          >
            <span class="flex items-center justify-center gap-2" data-submit-loading-content>
              <spring:message code="publish.actions.submit" />
              <span class="material-symbols-outlined text-sm">check_circle</span>
            </span>
            <span
              class="pointer-events-none absolute inset-0 hidden items-center justify-center"
              aria-hidden="true"
              data-submit-loading-spinner
            >
              <span class="h-5 w-5 animate-spin rounded-full border-2 border-current border-t-transparent"></span>
            </span>
          </button>
        </div>
      </section>
    </div>
  </form:form>
</paw:layout>
