<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="search" required="true" type="ar.edu.itba.paw.webapp.form.FavouritesSearchForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<input type="hidden" name="page" value="${search.page != null ? search.page : 1}" />
<input type="hidden" name="pageSize" value="${search.pageSize != null ? search.pageSize : 12}" />
<c:if test="${not empty search.sortBy && search.sortBy != 'newest'}">
  <input type="hidden" name="sortBy" value="<c:out value='${search.sortBy}'/>" />
</c:if>
<c:if test="${not empty search.searchQuery}">
  <input type="hidden" name="searchQuery" value="<c:out value='${search.searchQuery}'/>" />
</c:if>
