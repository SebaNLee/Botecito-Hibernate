<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="item" required="true" type="ar.edu.itba.paw.models.entity.Item" %>
<%@ attribute name="coverSrc" required="true" %>
<%@ attribute name="returnTo" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<fmt:setLocale value="es_AR" />
<spring:message code="reviews.empty.short" var="reviewsEmptyShortLabel" />
<c:url var="itemUrl" value="/item/${item.id}">
  <c:param name="returnTo" value="${returnTo}" />
</c:url>
<c:set var="resolvedCoverSrc" value="${coverSrc}" />
<c:if test="${empty resolvedCoverSrc}">
  <c:url var="resolvedCoverSrc" value="/css/boat-placeholder.svg" />
</c:if>
<a href="${itemUrl}" data-marketplace-item-link class="group card min-w-0 bg-base-100 shadow-sm overflow-hidden no-underline text-base-content transition duration-200 hover:-translate-y-0.5 hover:shadow-md">
  <figure class="aspect-[4/3] overflow-hidden">
    <img class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105" alt="${item.latestVersion.title}" src="${resolvedCoverSrc}"/>
  </figure>
  <div class="card-body min-w-0 p-4 gap-3">
    <div class="flex justify-between items-start gap-3">
      <h3 class="card-title min-w-0 text-lg font-bold text-on-background leading-tight m-0 line-clamp-2 break-words"><c:out value="${item.latestVersion.title}" /></h3>
      <div class="shrink-0 text-right">
        <span class="block text-xl font-black text-primary">$<fmt:formatNumber value="${item.latestVersion.price}" type="number" groupingUsed="true" maxFractionDigits="0" /></span>
        <span class="text-[10px] font-bold uppercase tracking-tighter text-outline"><spring:message code="marketplace.card.perHour" /></span>
      </div>
    </div>
    <div class="flex items-center gap-1 text-sm font-semibold text-on-surface-variant">
      <span class="material-symbols-outlined text-base text-warning">star</span>
      <c:out value="${reviewsEmptyShortLabel}" />
    </div>
    <div class="flex min-w-0 items-center text-on-surface-variant text-sm gap-1">
      <span class="material-symbols-outlined shrink-0 text-primary text-lg">location_on</span>
      <span class="min-w-0 truncate"><c:out value="${item.latestVersion.location.name}" /></span>
    </div>
    <div class="pt-3 border-t border-outline-variant/15 flex flex-col gap-3">
      <div class="flex flex-wrap items-center gap-3">
        <div class="flex items-center gap-1.5">
          <span class="material-symbols-outlined text-outline text-lg">groups</span>
          <span class="text-sm font-semibold"><spring:message code="marketplace.card.people" arguments="${item.latestVersion.capacity}" /></span>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="material-symbols-outlined text-outline text-lg">weight</span>
          <span class="text-sm font-semibold">
            <spring:message code="marketplace.card.weight" arguments="${item.latestVersion.weight}" />
          </span>
        </div>
      </div>
      <div class="text-primary font-bold text-sm flex items-center gap-1">
        <spring:message code="marketplace.card.details" />
        <span class="material-symbols-outlined text-sm transition-transform group-hover:translate-x-1">arrow_forward</span>
      </div>
    </div>
  </div>
</a>
