<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="rating" required="true" type="java.lang.Integer" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="starSize" value="${not empty size ? size : 'text-sm'}" />
<div class="flex items-center gap-0.5 ${cssClass}" aria-label="${rating} of 5">
  <c:forEach var="starIndex" begin="1" end="5">
    <span class="material-symbols-outlined leading-none ${starSize} ${starIndex <= rating ? 'text-warning opacity-100' : 'text-outline opacity-[0.35]'}">star</span>
  </c:forEach>
</div>
