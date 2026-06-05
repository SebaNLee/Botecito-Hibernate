<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="bookingId" required="true" type="java.lang.Integer" rtexprvalue="true" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" rtexprvalue="true" %>
<%@ attribute name="listMode" required="true" type="java.lang.String" rtexprvalue="true" %>
<%@ attribute name="targetType" required="false" type="java.lang.String" rtexprvalue="true" %>
<%@ attribute name="existingReview" required="false" type="ar.edu.itba.paw.models.entity.Review" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<spring:message code="itemDetail.reviews.leave" var="reviewLeaveLabel" />
<spring:message code="settings.reviews.view" var="reviewViewLabel" />
<spring:message code="itemDetail.reviews.rating" var="reviewRatingLabel" />
<spring:message code="itemDetail.reviews.comment" var="reviewCommentLabel" />
<spring:message code="settings.reviews.submit" var="reviewSubmitLabel" />

<c:choose>
  <c:when test="${existingReview != null}">
    <paw:detailsModal id="${modalId}" title="${reviewViewLabel}">
      <jsp:body>
        <div class="space-y-4">
          <c:set var="fullStars" value="${existingReview.rating ge 5 ? 5 : (existingReview.rating ge 4 ? 4 : (existingReview.rating ge 3 ? 3 : (existingReview.rating ge 2 ? 2 : (existingReview.rating ge 1 ? 1 : 0))))}" />
          <div class="flex items-center gap-1" aria-label="${existingReview.rating} of 5">
            <c:forEach var="starIndex" begin="1" end="5">
              <c:choose>
                <c:when test="${starIndex <= fullStars}">
                  <span class="material-symbols-outlined text-2xl leading-none text-warning">star</span>
                </c:when>
                <c:otherwise>
                  <span class="material-symbols-outlined text-2xl leading-none text-outline opacity-[0.35]">star</span>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </div>
          <c:if test="${not empty existingReview.comment}">
            <p class="m-0 text-sm text-on-surface-variant break-words whitespace-pre-line"><c:out value="${existingReview.comment}" /></p>
          </c:if>
          <c:if test="${not empty existingReview.createdAt}">
            <fmt:parseDate value="${fn:substring(existingReview.createdAt, 0, 16)}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedReviewDate" />
            <fmt:formatDate value="${parsedReviewDate}" pattern="dd/MM/yyyy" var="formattedReviewDate" />
          </c:if>
          <p class="m-0 text-xs text-outline"><c:out value="${formattedReviewDate}" /></p>
        </div>
      </jsp:body>
    </paw:detailsModal>
  </c:when>
  <c:otherwise>
    <paw:detailsModal id="${modalId}" title="${reviewLeaveLabel}">
      <jsp:body>
        <c:url var="reviewPostUrl" value="/requests/${listMode}/${bookingId}/review" />
        <form action="${reviewPostUrl}" method="post" class="space-y-4">
          <paw:bookingSearchHiddenFields bookingSearch="${bookingSearch}" />
          <c:if test="${not empty targetType}">
            <input type="hidden" name="targetType" value="${targetType}" />
          </c:if>
          <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3 items-center">
            <label class="text-xs font-bold uppercase tracking-wider text-outline" for="review-rating-${bookingId}"><c:out value="${reviewRatingLabel}" /></label>
            <div class="flex items-center gap-1" data-rating-stars>
              <input id="review-rating-${bookingId}" type="hidden" name="rating" value="" data-rating-value />
              <c:forEach var="starIndex" begin="1" end="5">
                <button type="button" class="btn btn-ghost btn-sm btn-square min-h-9 h-9 w-9 p-0" data-rating-star="${starIndex}" aria-label="${reviewRatingLabel} ${starIndex}">
                  <span class="material-symbols-outlined text-xl leading-none text-outline" style="opacity: 0.35;">star</span>
                </button>
              </c:forEach>
            </div>
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
            <label class="text-xs font-bold uppercase tracking-wider text-outline pt-2" for="review-comment-${bookingId}"><c:out value="${reviewCommentLabel}" /></label>
            <textarea id="review-comment-${bookingId}" name="comment" rows="3" maxlength="1000" class="textarea textarea-bordered w-full"></textarea>
          </div>
          <paw:button type="submit" color="primary" text="${reviewSubmitLabel}" submitLoading="true" />
        </form>
      </jsp:body>
    </paw:detailsModal>
  </c:otherwise>
</c:choose>
