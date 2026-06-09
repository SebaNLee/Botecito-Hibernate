<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="currentPage" required="true" %>
<%@ attribute name="totalPages" required="true" %>
<%@ attribute name="hasPrevious" required="true" %>
<%@ attribute name="hasNext" required="true" %>
<%@ attribute name="previousPageUrl" required="false" %>
<%@ attribute name="nextPageUrl" required="false" %>
<%@ attribute name="navClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:if test="${totalPages > 1}">
  <c:set var="resolvedNavClass" value="${not empty navClass ? navClass : 'mt-10 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant'}" />
  <nav class="<c:out value='${resolvedNavClass}' />">
    <c:choose>
      <c:when test="${hasPrevious}">
        <a href="<c:out value='${previousPageUrl}' />" class="btn btn-outline btn-sm no-underline gap-2">
          <span class="material-symbols-outlined text-sm">arrow_back</span>
          <spring:message code="marketplace.pagination.previous" />
        </a>
      </c:when>
      <c:otherwise>
        <span class="btn btn-outline btn-sm btn-disabled gap-2">
          <span class="material-symbols-outlined text-sm">arrow_back</span>
          <spring:message code="marketplace.pagination.previous" />
        </span>
      </c:otherwise>
    </c:choose>

    <span>
      <spring:message code="marketplace.pagination.page" arguments="${currentPage},${totalPages}" />
    </span>

    <c:choose>
      <c:when test="${hasNext}">
        <a href="<c:out value='${nextPageUrl}' />" class="btn btn-outline btn-sm no-underline gap-2">
          <spring:message code="marketplace.pagination.next" />
          <span class="material-symbols-outlined text-sm">arrow_forward</span>
        </a>
      </c:when>
      <c:otherwise>
        <span class="btn btn-outline btn-sm btn-disabled gap-2">
          <spring:message code="marketplace.pagination.next" />
          <span class="material-symbols-outlined text-sm">arrow_forward</span>
        </span>
      </c:otherwise>
    </c:choose>
  </nav>
</c:if>
