<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="rating" required="true" type="java.lang.Integer" rtexprvalue="true" %>
<%@ attribute name="sizeClass" required="false" type="java.lang.String" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="resolvedSizeClass" value="${not empty sizeClass ? sizeClass : 'text-2xl'}" />
<c:set var="fullStars" value="${rating ge 5 ? 5 : (rating ge 4 ? 4 : (rating ge 3 ? 3 : (rating ge 2 ? 2 : (rating ge 1 ? 1 : 0))))}" />

<div class="flex items-center gap-0.5" aria-label="<c:out value='${rating}' /> of 5">
  <c:forEach var="starIndex" begin="1" end="5">
    <c:choose>
      <c:when test="${starIndex <= fullStars}">
        <span class="material-symbols-outlined <c:out value='${resolvedSizeClass}' /> leading-none icon-star-filled">star</span>
      </c:when>
      <c:otherwise>
        <span class="material-symbols-outlined <c:out value='${resolvedSizeClass}' /> leading-none icon-star-outline">star</span>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</div>
