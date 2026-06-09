<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="view" required="true" type="ar.edu.itba.paw.webapp.form.ProfileListingsViewForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${view.page > 1}">
  <input type="hidden" name="page" value="<c:out value='${view.page}'/>" />
</c:if>
<c:if test="${view.sortBy != 'newest'}">
  <input type="hidden" name="sortBy" value="<c:out value='${view.sortBy}'/>" />
</c:if>
<c:if test="${view.pageSize != 12}">
  <input type="hidden" name="pageSize" value="<c:out value='${view.pageSize}'/>" />
</c:if>
