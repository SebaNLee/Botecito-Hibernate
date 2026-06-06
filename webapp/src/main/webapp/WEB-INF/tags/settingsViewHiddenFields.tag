<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="view" required="true" type="ar.edu.itba.paw.webapp.form.SettingsViewForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${view.page != null && view.page > 1}">
  <input type="hidden" name="page" value="${view.page}" />
</c:if>
<c:if test="${view.pageSize != null && view.pageSize != 6}">
  <input type="hidden" name="pageSize" value="${view.pageSize}" />
</c:if>
<c:if test="${view.edit}">
  <input type="hidden" name="edit" value="true" />
</c:if>
