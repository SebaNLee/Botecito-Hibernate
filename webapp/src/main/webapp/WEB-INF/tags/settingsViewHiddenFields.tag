<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="view" required="true" type="ar.edu.itba.paw.webapp.form.SettingsViewForm" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<input type="hidden" name="subscriptionsPage" value="${view.subscriptionsPage != null ? view.subscriptionsPage : 1}" />
<c:if test="${view.subscriptionsPageSize != null && view.subscriptionsPageSize != 6}">
  <input type="hidden" name="subscriptionsPageSize" value="${view.subscriptionsPageSize}" />
</c:if>
<c:if test="${view.edit}">
  <input type="hidden" name="edit" value="true" />
</c:if>
