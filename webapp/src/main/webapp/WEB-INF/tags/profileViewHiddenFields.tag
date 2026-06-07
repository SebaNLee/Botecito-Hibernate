<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="view" required="true" type="ar.edu.itba.paw.webapp.form.ProfileViewForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${not empty view.tab && view.tab != 'listings'}">
  <input type="hidden" name="tab" value="${fn:escapeXml(view.tab)}" />
</c:if>
<c:if test="${view.tab == 'reviews'}">
  <input type="hidden" name="reviewsPage" value="${view.reviewsPage != null ? view.reviewsPage : 1}" />
  <c:if test="${view.reviewsPageSize != null && view.reviewsPageSize != 5}">
    <input type="hidden" name="reviewsPageSize" value="${view.reviewsPageSize}" />
  </c:if>
</c:if>
<c:if test="${view.tab != 'reviews'}">
  <input type="hidden" name="listingsPage" value="${view.listingsPage != null ? view.listingsPage : 1}" />
  <c:if test="${view.listingsPageSize != null && view.listingsPageSize != 6}">
    <input type="hidden" name="listingsPageSize" value="${view.listingsPageSize}" />
  </c:if>
</c:if>
