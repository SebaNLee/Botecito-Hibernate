<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="view" required="true" type="ar.edu.itba.paw.webapp.form.ItemDetailViewForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<input type="hidden" name="itemId" value="${view.itemId}" />
<c:if test="${view.page != null && view.page > 1}">
  <input type="hidden" name="page" value="${view.page}" />
</c:if>
