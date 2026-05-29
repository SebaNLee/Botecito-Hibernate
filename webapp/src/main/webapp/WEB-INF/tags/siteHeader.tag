<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="ctaMessageCode" required="false" %>
<%@ attribute name="ctaHref" required="false" %>
<%@ attribute name="ctaVariant" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<c:set var="resolvedCtaMessageCode" value="${not empty ctaMessageCode ? ctaMessageCode : 'nav.publishCta'}" />
<c:set var="resolvedCtaHref" value="${not empty ctaHref ? ctaHref : '/publish'}" />
<c:set var="resolvedCtaVariant" value="${not empty ctaVariant ? ctaVariant : 'publish'}" />
<c:set var="resolvedCtaClass" value="${resolvedCtaVariant == 'rent' ? 'btn btn-primary btn-sm no-underline' : 'btn btn-secondary btn-sm no-underline'}" />

<header class="fixed top-0 w-full z-50 navbar bg-base-100 border-b border-outline-variant/20 shadow-sm px-6 min-h-16">
  <div class="flex justify-between items-center w-full max-w-7xl mx-auto">
    <a href="<c:url value='/' />" class="text-2xl font-black text-primary font-headline tracking-tight no-underline">
      Botecito
    </a>
    <div class="flex items-center gap-2">
      <a href="<c:url value='/marketplace' />" class="btn btn-outline btn-sm border-outline-variant/40 bg-base-100 no-underline hover:bg-base-200">
        <spring:message code="nav.marketplace" />
      </a>
      <sec:authorize access="isAnonymous()">
        <a href="<c:url value='/login' />" class="btn btn-ghost btn-sm no-underline">
          <spring:message code="nav.login" />
        </a>
      </sec:authorize>
      <sec:authorize access="isAuthenticated()">
        <sec:authorize access="hasRole('ADMIN')">
          <a href="<c:url value='/admin/reports' />" class="btn btn-outline btn-sm border-outline-variant/40 bg-base-100 no-underline hover:bg-base-200">
            <spring:message code="nav.adminReports" />
          </a>
        </sec:authorize>
        <a href="<c:url value='/my-boats' />" class="btn btn-outline btn-sm border-outline-variant/40 bg-base-100 no-underline hover:bg-base-200">
          <spring:message code="account.nav.myBoats" />
        </a>
        <a href="<c:url value='/requests/outgoing' />" class="btn btn-outline btn-sm border-outline-variant/40 bg-base-100 no-underline hover:bg-base-200">
          <spring:message code="account.nav.bookings" />
        </a>
        <a href="<c:url value='/settings' />" class="btn btn-outline btn-circle btn-sm border-outline-variant/40 bg-base-100 no-underline hover:bg-base-200" title="<sec:authentication property='name' />" aria-label="Settings">
          <span class="material-symbols-outlined text-xl">account_circle</span>
        </a>
      </sec:authorize>
      <c:choose>
        <c:when test="${resolvedCtaVariant == 'publish'}">
          <sec:authorize access="isAnonymous()">
            <a href="<c:url value='${resolvedCtaHref}' />" class="${resolvedCtaClass}">
              <spring:message code="${resolvedCtaMessageCode}" />
            </a>
          </sec:authorize>
        </c:when>
        <c:when test="${resolvedCtaVariant == 'rent'}">
        </c:when>
        <c:otherwise>
          <a href="<c:url value='${resolvedCtaHref}' />" class="${resolvedCtaClass}">
            <spring:message code="${resolvedCtaMessageCode}" />
          </a>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</header>
