<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="view" required="true" type="ar.edu.itba.paw.webapp.form.ProfileViewForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:if test="${view.tab == 'reviews'}">
  <input type="hidden" name="tab" value="reviews" />
  <c:if test="${view.page > 1}">
    <input type="hidden" name="page" value="${view.page}" />
  </c:if>
</c:if>
<c:if test="${view.tab != 'reviews'}">
  <c:if test="${view.page > 1}">
    <input type="hidden" name="page" value="${view.page}" />
  </c:if>
  <input type="hidden" name="sortBy" value="<c:out value='${view.sortBy}'/>" />
  <input type="hidden" name="pageSize" value="${view.pageSize}" />
</c:if>
