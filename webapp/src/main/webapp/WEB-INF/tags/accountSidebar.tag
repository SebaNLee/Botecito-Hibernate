<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="active" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:url var="profileUrl" value="/profile" />
<c:url var="myBoatsUrl" value="/my-boats" />
<c:url var="bookingsUrl" value="/bookings" />

<aside class="card bg-base-100 shadow-sm lg:sticky lg:top-24">
  <nav class="card-body gap-2 p-4">
    <a href="${profileUrl}" class="btn ${active == 'profile' ? 'btn-primary' : 'btn-ghost'} justify-start no-underline">
      <span class="material-symbols-outlined text-base">person</span>
      <spring:message code="account.nav.profile" />
    </a>
    <a href="${myBoatsUrl}" class="btn ${active == 'my-boats' ? 'btn-primary' : 'btn-ghost'} justify-start no-underline">
      <span class="material-symbols-outlined text-base">directions_boat</span>
      <spring:message code="account.nav.myBoats" />
    </a>
    <a href="${bookingsUrl}" class="btn ${active == 'bookings' ? 'btn-primary' : 'btn-ghost'} justify-start no-underline">
      <span class="material-symbols-outlined text-base">luggage</span>
      <spring:message code="account.nav.bookings" />
    </a>
  </nav>
</aside>
