<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="active" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:url var="requestsIncomingUrl" value="/requests/incoming" />
<c:url var="requestsOutgoingUrl" value="/requests/outgoing" />
<spring:message code="requests.nav.sectionTitle" var="requestsNavSectionTitle" />

<aside class="card bg-base-200 shadow-sm">
  <nav class="card-body gap-2 p-4" aria-label="${requestsNavSectionTitle}">
    <p class="m-0 text-xs font-bold uppercase tracking-wider text-outline"><c:out value="${requestsNavSectionTitle}" /></p>
    <a
      href="${requestsIncomingUrl}"
      data-nav-filter-page="requestsIncoming"
      class="btn ${active == 'incoming' ? 'btn-primary' : 'btn-ghost'} justify-start no-underline">
      <span class="material-symbols-outlined text-base">inbox</span>
      <spring:message code="requests.nav.incoming" />
    </a>
    <a
      href="${requestsOutgoingUrl}"
      data-nav-filter-page="requestsOutgoing"
      class="btn ${active == 'outgoing' ? 'btn-primary' : 'btn-ghost'} justify-start no-underline">
      <span class="material-symbols-outlined text-base">send</span>
      <spring:message code="requests.nav.outgoing" />
    </a>
  </nav>
</aside>
