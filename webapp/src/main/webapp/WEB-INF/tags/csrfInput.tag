<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%-- Hidden CSRF field for plain <form method="post"> elements. Spring's <form:form> tags
     inject this automatically via CsrfRequestDataValueProcessor and must NOT use this tag. --%>
<c:if test="${not empty _csrf}"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /></c:if>
