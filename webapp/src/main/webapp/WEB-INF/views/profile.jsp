<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="logoutUrl" value="/logout" />

<c:set var="initials" value="" />
<c:if test="${not empty user.givenName}">
  <c:set var="initials" value="${fn:substring(user.givenName, 0, 1)}" />
</c:if>
<c:if test="${not empty user.lastName}">
  <c:set var="initials" value="${initials}${fn:substring(user.lastName, 0, 1)}" />
</c:if>
<c:set var="initials" value="${fn:toUpperCase(initials)}" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 max-w-2xl mx-auto px-6">
  <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-8">

    <div class="flex items-center gap-5 pb-6 border-b border-outline-variant/20">
      <div class="w-16 h-16 rounded-full bg-primary flex items-center justify-center shrink-0">
        <span class="text-on-primary font-bold text-2xl font-headline">${initials}</span>
      </div>
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
          <c:out value="${user.name}" />
        </h1>
        <p class="text-on-surface-variant m-0 mt-1">
          <c:out value="${user.email}" />
        </p>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.givenName" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.givenName}" />
        </p>
      </div>

      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.lastName" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.lastName}" />
        </p>
      </div>

      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.email" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.email}" />
        </p>
      </div>

      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.memberSince" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.createdAt}" />
        </p>
      </div>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.publications.title" /></h2>
      <c:if test="${param.publishAction == 'deleted'}">
        <div class="rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm font-medium text-green-700">
          <spring:message code="profile.publications.deleted" />
        </div>
      </c:if>
      <c:if test="${param.publishAction == 'alreadyDeleted'}">
        <div class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800">
          <spring:message code="profile.publications.alreadyDeleted" />
        </div>
      </c:if>
      <c:if test="${param.publishAction == 'forbidden' || param.publishAction == 'error'}">
        <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          <spring:message code="profile.publications.error" />
        </div>
      </c:if>
      <c:choose>
        <c:when test="${not empty ownedItems}">
          <c:forEach var="item" items="${ownedItems}">
            <c:url var="deleteItemUrl" value="/publish/item/${item.id}/delete" />
            <div class="rounded-xl border border-outline-variant/30 bg-surface-container-high p-4 flex items-center justify-between gap-4 ${item.active ? '' : 'opacity-75'}">
              <div>
                <div class="flex items-center gap-2">
                  <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${item.title}" /></p>
                  <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${item.active ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-700'}">
                    <spring:message code="${item.active ? 'profile.publications.status.active' : 'profile.publications.status.inactive'}" />
                  </span>
                </div>
                <p class="m-0 text-xs text-on-surface-variant"><spring:message code="profile.publications.price" arguments="${item.pricePerHour}" /></p>
              </div>
              <c:if test="${item.active}">
                <form action="${deleteItemUrl}" method="post" class="m-0">
                  <button type="submit" class="px-4 py-2 rounded-lg border border-red-300 bg-white text-red-700 font-semibold text-xs cursor-pointer">
                    <spring:message code="profile.publications.delete" />
                  </button>
                </form>
              </c:if>
            </div>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.publications.empty" /></p>
        </c:otherwise>
      </c:choose>
    </div>

    <div class="space-y-4">
      <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.bookings.title" /></h2>
      <c:if test="${param.bookingAction == 'accepted'}">
        <div class="rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm font-medium text-green-700">
          <spring:message code="profile.bookings.accepted" />
        </div>
      </c:if>
      <c:if test="${param.bookingAction == 'rejected'}">
        <div class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-medium text-amber-800">
          <spring:message code="profile.bookings.rejected" />
        </div>
      </c:if>
      <c:if test="${param.bookingAction == 'forbidden' || param.bookingAction == 'error' || param.bookingAction == 'notFound'}">
        <div class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
          <spring:message code="profile.bookings.error" />
        </div>
      </c:if>
      <c:choose>
        <c:when test="${not empty pendingBookingRequests}">
          <c:forEach var="request" items="${pendingBookingRequests}">
            <c:url var="acceptBookingUrl" value="/bookings/${request.id}/accept" />
            <c:url var="declineBookingUrl" value="/bookings/${request.id}/decline" />
            <div class="rounded-xl border border-outline-variant/30 bg-surface-container-high p-4 space-y-3">
              <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${request.itemTitle}" /></p>
              <p class="m-0 text-xs text-on-surface-variant"><spring:message code="profile.bookings.requester" arguments="${request.requesterName},${request.requesterEmail}" /></p>
              <div class="flex gap-2">
                <form action="${acceptBookingUrl}" method="post" class="m-0">
                  <button type="submit" class="px-4 py-2 rounded-lg border-none bg-green-600 text-white font-semibold text-xs cursor-pointer">
                    <spring:message code="profile.bookings.accept" />
                  </button>
                </form>
                <form action="${declineBookingUrl}" method="post" class="m-0">
                  <button type="submit" class="px-4 py-2 rounded-lg border border-red-300 bg-white text-red-700 font-semibold text-xs cursor-pointer">
                    <spring:message code="profile.bookings.decline" />
                  </button>
                </form>
              </div>
            </div>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.bookings.empty" /></p>
        </c:otherwise>
      </c:choose>
    </div>

    <form action="${logoutUrl}" method="post">
      <button
        type="submit"
        class="w-full px-6 py-3 bg-secondary text-white font-bold rounded-xl shadow-[0_16px_32px_rgba(174,49,35,0.24)] hover:bg-[#962619] transition-all active:scale-[0.98] border-none cursor-pointer text-sm">
        <spring:message code="profile.logout" />
      </button>
    </form>
  </div>
</paw:layout>
