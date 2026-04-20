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
<spring:message code="publish.actions.saveDraft" var="publishSaveDraftLabel" />
<spring:message code="publish.actions.submit" var="publishSubmitLabel" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">
  <div class="mb-8">
    <a href="${stepTwoUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span><spring:message code="publish.step3.progress" /></span>
      <span><spring:message code="publish.step3.badge" /></span>
    </div>
    <progress class="progress progress-primary mt-3 w-full" value="100" max="100"></progress>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"><spring:message code="publish.step3.title" /></h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0"><spring:message code="publish.step3.subtitle" /></p>
  </div>

  <spring:hasBindErrors name="publishForm">
    <c:forEach var="error" items="${errors.globalErrors}">
      <paw:alertMessage type="error">
        <spring:message code="${error.codes[0]}" arguments="${error.arguments}" />
      </paw:alertMessage>
    </c:forEach>
  </spring:hasBindErrors>

  <form:form
      action="${stepThreeUrl}"
      method="post"
      modelAttribute="publishForm"
      class="space-y-8"
      data-submit-loading-form="true">

    <div class="grid grid-cols-1 lg:grid-cols-5 gap-8 items-start">
      <paw:sectionCard element="aside" cssClass="lg:col-span-2" icon="inventory_2">
        <jsp:attribute name="title"><spring:message code="publish.step3.summary.title" /></jsp:attribute>
        <jsp:body>
          <div class="rounded-xl overflow-hidden border border-outline-variant/30 bg-base-200">
            <c:choose>
              <c:when test="${not empty uploadedImagePreviewUrl}">
                <img src="${publishPreviewImageUrl}" alt="<spring:message code='publish.image.previewAlt' />" class="w-full aspect-[16/10] object-cover" />
              </c:when>
              <c:otherwise>
                <img src="${placeholderImageUrl}" alt="<spring:message code='publish.image.defaultAlt' />" class="w-full aspect-[16/10] object-cover" />
              </c:otherwise>
            </c:choose>
          </div>

          <div>
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-1"><spring:message code="publish.form.title.label" /></p>
            <p class="text-lg font-extrabold text-on-surface m-0"><c:out value="${publishForm.title}" /></p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div class="rounded-xl bg-base-200 p-3">
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
            <div class="rounded-xl bg-base-200 p-3">
              <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.capacity.label" /></p>
              <p class="text-sm font-bold text-on-surface mt-1 mb-0"><spring:message code="publish.capacity.people" arguments="${publishForm.capacity}" /></p>
            </div>
            <div class="rounded-xl bg-base-200 p-3">
              <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.price.short" /></p>
              <p class="text-sm font-bold text-on-surface mt-1 mb-0">$ <c:out value="${publishForm.pricePerHour}" /></p>
            </div>
            <div class="rounded-xl bg-base-200 p-3">
              <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.location.label" /></p>
              <p class="text-sm font-bold text-on-surface mt-1 mb-0"><c:out value="${selectedLocationName}" /></p>
            </div>
            <div class="rounded-xl bg-base-200 p-3 col-span-2">
              <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.maxWeight.label" /></p>
              <p class="text-sm font-bold text-on-surface mt-1 mb-0">
                <c:choose>
                  <c:when test="${not empty publishForm.maxWeight}"><c:out value="${publishForm.maxWeight}" /> kg</c:when>
                  <c:otherwise><spring:message code="publish.form.maxWeight.unspecified" /></c:otherwise>
                </c:choose>
              </p>
            </div>
          </div>

          <div>
            <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-2"><spring:message code="publish.step3.summary.availability" /></p>
            <form:errors path="availabilityByWeekday" cssClass="mb-2" element="div" />
            <c:choose>
              <c:when test="${not empty availabilitySummary}">
                <ul class="space-y-2 m-0 p-0 list-none">
                  <c:forEach var="slot" items="${availabilitySummary}">
                    <li class="rounded-xl bg-base-200 px-3 py-2 text-sm text-on-surface font-medium">
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
        </jsp:body>
      </paw:sectionCard>

      <paw:sectionCard cssClass="lg:col-span-3" icon="person">
        <jsp:attribute name="title"><spring:message code="publish.step3.account.title" /></jsp:attribute>
        <jsp:body>
          <div class="rounded-xl bg-base-200 p-4 text-sm text-on-surface-variant leading-relaxed">
            <spring:message code="publish.step3.note" />
          </div>

          <div class="flex flex-col sm:flex-row justify-between items-center gap-4">
            <paw:button href="${marketplaceUrl}" color="ghost" size="lg" cssClass="w-full sm:w-auto" text="${publishSaveDraftLabel}" />
            <button
              type="submit"
              class="btn btn-primary btn-lg w-full sm:w-auto"
              data-submit-loading-button>
              <span class="flex items-center justify-center gap-2" data-submit-loading-content>
                <spring:message code="publish.actions.submit" />
                <span class="material-symbols-outlined text-sm">check_circle</span>
              </span>
              <span class="pointer-events-none absolute inset-0 hidden items-center justify-center" aria-hidden="true" data-submit-loading-spinner>
                <span class="loading loading-spinner loading-sm"></span>
              </span>
            </button>
          </div>
        </jsp:body>
      </paw:sectionCard>
    </div>
  </form:form>
</paw:layout>
