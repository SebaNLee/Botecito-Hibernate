<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="active" required="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:url var="requestsIncomingUrl" value="/requests/incoming" />
<c:url var="requestsOutgoingUrl" value="/requests/outgoing" />
<spring:message code="requests.nav.sectionTitle" var="requestsNavSectionTitle" />

<c:choose>
  <c:when test="${active == 'incoming'}">
    <c:set var="incomingNavClass" value="btn btn-primary justify-start no-underline" />
    <c:set var="outgoingNavClass" value="btn btn-ghost justify-start no-underline" />
  </c:when>
  <c:when test="${active == 'outgoing'}">
    <c:set var="incomingNavClass" value="btn btn-ghost justify-start no-underline" />
    <c:set var="outgoingNavClass" value="btn btn-primary justify-start no-underline" />
  </c:when>
  <c:otherwise>
    <c:set var="incomingNavClass" value="btn btn-ghost justify-start no-underline" />
    <c:set var="outgoingNavClass" value="btn btn-ghost justify-start no-underline" />
  </c:otherwise>
</c:choose>

<aside class="card bg-base-200 shadow-sm">
  <nav class="card-body gap-2 p-4" aria-label="<c:out value='${requestsNavSectionTitle}' />">
    <p class="m-0 text-xs font-bold uppercase tracking-wider text-outline"><c:out value="${requestsNavSectionTitle}" /></p>
    <a
      href="<c:out value='${requestsIncomingUrl}' />"
      data-nav-filter-page="requestsIncoming"
      class="<c:out value='${incomingNavClass}' />">
      <span class="material-symbols-outlined text-base">inbox</span>
      <spring:message code="requests.nav.incoming" />
    </a>
    <a
      href="<c:out value='${requestsOutgoingUrl}' />"
      data-nav-filter-page="requestsOutgoing"
      class="<c:out value='${outgoingNavClass}' />">
      <span class="material-symbols-outlined text-base">send</span>
      <spring:message code="requests.nav.outgoing" />
    </a>
  </nav>
</aside>
