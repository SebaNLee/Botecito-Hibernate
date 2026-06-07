<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="bookingSearch" required="true" type="ar.edu.itba.paw.webapp.form.BookingSearchForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<input type="hidden" name="page" value="${bookingSearch.page != null ? bookingSearch.page : 1}" />
<input type="hidden" name="pageSize" value="${bookingSearch.pageSize != null ? bookingSearch.pageSize : 12}" />
<c:if test="${not empty bookingSearch.sortBy && bookingSearch.sortBy != 'newest'}">
  <input type="hidden" name="sortBy" value="<c:out value='${bookingSearch.sortBy}'/>" />
</c:if>
<c:if test="${not empty bookingSearch.searchQuery}">
  <input type="hidden" name="searchQuery" value="<c:out value='${bookingSearch.searchQuery}'/>" />
</c:if>
<c:if test="${not empty bookingSearch.date}">
  <input type="hidden" name="date" value="<c:out value='${bookingSearch.date}'/>" />
</c:if>
<c:if test="${not empty bookingSearch.status}">
  <input type="hidden" name="status" value="<c:out value='${bookingSearch.status}'/>" />
</c:if>
