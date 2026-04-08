<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="publishUrl" value="/publish" />
<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="placeholderImageUrl" value="/css/boat-placeholder.svg" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-3xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">

  <div class="text-center mb-10">
    <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-green-100 text-green-600 mb-6">
      <span class="material-symbols-outlined text-5xl">check_circle</span>
    </div>
    <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0"><spring:message code="publish.success.title" /></h1>
    <p class="text-on-surface-variant mt-3 text-lg m-0"><spring:message code="publish.success.subtitle" /></p>
  </div>

  <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] mb-8">
    <div class="flex items-center gap-3 mb-6 pb-4 border-b border-outline-variant/20">
      <span class="material-symbols-outlined text-primary text-2xl">inventory_2</span>
      <h2 class="text-xl font-extrabold m-0"><spring:message code="publish.success.summary.title" /></h2>
    </div>

    <div class="rounded-xl overflow-hidden border border-outline-variant/30 bg-surface-container-high mb-6">
      <c:choose>
        <c:when test="${not empty itemImageUrl}">
          <img
            src="${itemImageUrl}"
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

    <div class="mb-4">
      <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-1"><spring:message code="publish.form.title.label" /></p>
      <p class="text-lg font-extrabold text-on-surface m-0"><c:out value="${item.title}" /></p>
    </div>

    <c:if test="${not empty item.description}">
      <div class="mb-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-1"><spring:message code="publish.form.description.label" /></p>
        <p class="text-sm text-on-surface m-0"><c:out value="${item.description}" /></p>
      </div>
    </c:if>

    <div class="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
      <div class="rounded-xl bg-surface-container-high p-3">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.type.label" /></p>
        <p class="text-sm font-bold text-on-surface mt-1 mb-0">
          <c:choose>
            <c:when test="${item.typeId == 1}"><spring:message code="publish.type.other" /></c:when>
            <c:when test="${item.typeId == 2}"><spring:message code="publish.type.kayak" /></c:when>
            <c:when test="${item.typeId == 3}"><spring:message code="publish.type.paddle" /></c:when>
            <c:when test="${item.typeId == 4}"><spring:message code="publish.type.canoe" /></c:when>
            <c:when test="${item.typeId == 5}"><spring:message code="publish.type.windsurf" /></c:when>
            <c:when test="${item.typeId == 6}"><spring:message code="publish.type.efoil" /></c:when>
            <c:when test="${item.typeId == 7}"><spring:message code="publish.type.optimist" /></c:when>
            <c:otherwise><spring:message code="publish.type.none" /></c:otherwise>
          </c:choose>
        </p>
      </div>
      <div class="rounded-xl bg-surface-container-high p-3">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.price.short" /></p>
        <p class="text-sm font-bold text-on-surface mt-1 mb-0">$ <c:out value="${item.pricePerHour}" /></p>
      </div>
      <div class="rounded-xl bg-surface-container-high p-3">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.capacity.label" /></p>
        <p class="text-sm font-bold text-on-surface mt-1 mb-0"><spring:message code="publish.capacity.people" arguments="${item.capacityPeople}" /></p>
      </div>
      <div class="rounded-xl bg-surface-container-high p-3">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.location.label" /></p>
        <p class="text-sm font-bold text-on-surface mt-1 mb-0"><c:out value="${item.location}" /></p>
      </div>
      <c:if test="${not empty item.maxWeightKg}">
        <div class="rounded-xl bg-surface-container-high p-3">
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.maxWeight.label" /></p>
          <p class="text-sm font-bold text-on-surface mt-1 mb-0"><c:out value="${item.maxWeightKg}" /> kg</p>
        </div>
      </c:if>
      <c:if test="${not empty item.difficultyLevel}">
        <div class="rounded-xl bg-surface-container-high p-3">
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0"><spring:message code="publish.form.difficulty.label" /></p>
          <p class="text-sm font-bold text-on-surface mt-1 mb-0"><c:out value="${item.difficultyLevel}" /> / 5</p>
        </div>
      </c:if>
    </div>

    <c:if test="${not empty availabilities}">
      <div>
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline mb-2"><spring:message code="publish.step3.summary.availability" /></p>
        <ul class="space-y-1.5 m-0 p-0 list-none">
          <c:forEach var="avail" items="${availabilities}">
            <li class="rounded-xl bg-surface-container-high px-3 py-2 text-sm text-on-surface font-medium">
              <c:choose>
                <c:when test="${avail.weekday == 'MONDAY'}"><spring:message code="weekday.monday" /></c:when>
                <c:when test="${avail.weekday == 'TUESDAY'}"><spring:message code="weekday.tuesday" /></c:when>
                <c:when test="${avail.weekday == 'WEDNESDAY'}"><spring:message code="weekday.wednesday" /></c:when>
                <c:when test="${avail.weekday == 'THURSDAY'}"><spring:message code="weekday.thursday" /></c:when>
                <c:when test="${avail.weekday == 'FRIDAY'}"><spring:message code="weekday.friday" /></c:when>
                <c:when test="${avail.weekday == 'SATURDAY'}"><spring:message code="weekday.saturday" /></c:when>
                <c:when test="${avail.weekday == 'SUNDAY'}"><spring:message code="weekday.sunday" /></c:when>
                <c:otherwise><c:out value="${avail.weekday}" /></c:otherwise>
              </c:choose>
              : <c:out value="${avail.startTime}" /> - <c:out value="${avail.endTime}" />
            </li>
          </c:forEach>
        </ul>
      </div>
    </c:if>
  </section>

  <div class="flex flex-col sm:flex-row justify-center items-center gap-4">
    <a href="${publishUrl}" class="w-full sm:w-auto px-8 py-4 bg-primary text-on-primary rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary-container transition-all no-underline text-center flex items-center justify-center gap-2">
      <span class="material-symbols-outlined text-sm">add</span>
      <spring:message code="publish.success.createAnother" />
    </a>
    <a href="${marketplaceUrl}" class="w-full sm:w-auto px-8 py-4 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors no-underline text-center flex items-center justify-center gap-2">
      <span class="material-symbols-outlined text-sm">storefront</span>
      <spring:message code="publish.success.goMarketplace" />
    </a>
  </div>

</paw:layout>
