<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="review" required="true" type="ar.edu.itba.paw.models.entity.Review" rtexprvalue="true" %>
<%@ attribute name="reviewDate" required="true" type="java.lang.String" rtexprvalue="true" %>
<%@ attribute name="showReviewer" required="false" type="java.lang.Boolean" rtexprvalue="true" %>
<%@ attribute name="anonymousLabel" required="false" type="java.lang.String" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<div class="rounded-xl bg-base-200 p-4 flex flex-col gap-2">
  <div class="flex items-center justify-between gap-3 flex-wrap">
    <c:choose>
      <c:when test="${showReviewer}">
        <c:set var="reviewerFirstName" value="${review.sender.firstName != null ? fn:trim(review.sender.firstName) : ''}" />
        <c:set var="reviewerLastName" value="${review.sender.lastName != null ? fn:trim(review.sender.lastName) : ''}" />
        <c:url var="reviewerProfileUrl" value="/profiles/${review.sender.id}/reviews" />
        <a href="${reviewerProfileUrl}" class="font-bold text-on-surface no-underline hover:underline break-words">
          <c:choose>
            <c:when test="${not empty reviewerFirstName or not empty reviewerLastName}">
              <c:out value="${reviewerFirstName}" />
              <c:if test="${not empty reviewerFirstName and not empty reviewerLastName}"> </c:if>
              <c:out value="${reviewerLastName}" />
            </c:when>
            <c:otherwise><c:out value="${review.sender.email}" /></c:otherwise>
          </c:choose>
        </a>
      </c:when>
      <c:otherwise>
        <p class="m-0 text-sm font-bold text-on-surface break-words"><c:out value="${anonymousLabel}" /></p>
      </c:otherwise>
    </c:choose>
    <paw:ratingStarsDisplay rating="${review.rating}" sizeClass="text-lg" />
  </div>
  <c:if test="${not empty review.comment}">
    <p class="m-0 text-sm text-on-surface-variant break-words"><c:out value="${review.comment}" /></p>
  </c:if>
  <p class="m-0 text-xs text-outline">
    <c:out value="${reviewDate}" />
  </p>
</div>
