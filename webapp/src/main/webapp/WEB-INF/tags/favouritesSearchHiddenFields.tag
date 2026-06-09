<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="search" required="true" type="ar.edu.itba.paw.webapp.form.FavouritesSearchForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<input type="hidden" name="page" value="<c:out value='${search.page}'/>" />
<input type="hidden" name="pageSize" value="<c:out value='${search.pageSize}'/>" />
<input type="hidden" name="sortBy" value="<c:out value='${search.sortBy}'/>" />
<c:if test="${not empty search.searchQuery}">
  <input type="hidden" name="searchQuery" value="<c:out value='${search.searchQuery}'/>" />
</c:if>
